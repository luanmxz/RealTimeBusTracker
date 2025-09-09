package com.devluanmarcene.NextBusRealTimeTracker.service;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.devluanmarcene.NextBusRealTimeTracker.config.WebClientConfig;
import com.devluanmarcene.NextBusRealTimeTracker.dto.internal.StopDTO;
import com.devluanmarcene.NextBusRealTimeTracker.dto.internal.TravelDTO;
import com.devluanmarcene.NextBusRealTimeTracker.dto.response.RouteListResponse;
import com.devluanmarcene.NextBusRealTimeTracker.dto.response.RouteResponse;
import com.devluanmarcene.NextBusRealTimeTracker.helpers.HaversineUtils;
import com.devluanmarcene.NextBusRealTimeTracker.mapper.RouteListMapper;
import com.devluanmarcene.NextBusRealTimeTracker.mapper.RouteMapper;
import com.devluanmarcene.NextBusRealTimeTracker.model.Agency;
import com.devluanmarcene.NextBusRealTimeTracker.model.AgencyList;
import com.devluanmarcene.NextBusRealTimeTracker.model.BodyPredictions;
import com.devluanmarcene.NextBusRealTimeTracker.model.BodyVehicle;
import com.devluanmarcene.NextBusRealTimeTracker.model.LatLng;
import com.devluanmarcene.NextBusRealTimeTracker.model.Route;
import com.devluanmarcene.NextBusRealTimeTracker.model.RouteList;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
public class BusTrackService {

        private WebClientConfig webClientConfig;

        public BusTrackService(WebClientConfig webClientConfig) {
                this.webClientConfig = webClientConfig;
        }

        @Cacheable("agencies")
        public Mono<AgencyList> getAgencies() {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "agencyList")
                                                .build())
                                .accept(MediaType.APPLICATION_XML)
                                .retrieve().bodyToMono(String.class)
                                .map(xmlString -> doXmlMapping(xmlString, AgencyList.class));
        }

        public Mono<RouteList> getAgencyRoutes(Agency agency) {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "routeConfig")
                                                .queryParam("a", agency)
                                                .build())
                                .retrieve().bodyToMono(String.class)
                                .flatMap(xml -> Mono.fromCallable(() -> doXmlMapping(xml, RouteList.class))
                                                .subscribeOn(Schedulers.boundedElastic()));
        }

        public Flux<Route> getRoutesByAgencyTag(String agencyTag) {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "routeConfig")
                                                .queryParam("a", agencyTag)
                                                .build())
                                .retrieve().bodyToMono(String.class)
                                .flatMap(xml -> Mono.fromCallable(() -> doXmlMapping(xml, RouteList.class))
                                                .subscribeOn(Schedulers.boundedElastic()))
                                .flatMapMany(routeList -> Flux
                                                .fromIterable(routeList.routes() == null
                                                                ? Collections.emptyList()
                                                                : routeList.routes())
                                                .subscribeOn(Schedulers.parallel()));

        }

        public Flux<RouteResponse> getRoutesByAgencyTagWithPredictions(String agencyTag) {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "routeConfig")
                                                .queryParam("a", agencyTag)
                                                .build())
                                .retrieve().bodyToMono(String.class)
                                .flatMap(xml -> Mono.fromCallable(() -> doXmlMapping(xml, RouteList.class))
                                                .subscribeOn(Schedulers.boundedElastic()))
                                .flatMapMany(routeList -> Flux
                                                .fromIterable(routeList.routes() == null ? Collections.emptyList()
                                                                : routeList.routes())
                                                .subscribeOn(Schedulers.parallel())
                                                .flatMap(route -> {
                                                        RouteResponse routeResponse = RouteMapper.fromRoute(route)
                                                                        .build();
                                                        Mono<RouteResponse> routeWithPredictions = enrichRouteWithPredictions(
                                                                        routeResponse,
                                                                        agencyTag);
                                                        return routeWithPredictions;
                                                }, 8))
                                .flatMap(routeResponse -> {
                                        Mono<RouteResponse> routeWithVehicle = getRouteVehicles(agencyTag,
                                                        routeResponse.tag())
                                                        .map(v -> {
                                                                return RouteResponse.Builder.from(routeResponse)
                                                                                .vehicles(v.vehicles())
                                                                                .build();
                                                        });
                                        return routeWithVehicle;
                                });
        }

        private Mono<RouteResponse> enrichRouteWithPredictions(RouteResponse routeResponse, String agencyTag) {
                final int stopConcurrency = 16;

                return Flux.fromIterable(
                                routeResponse.stops() == null ? Collections.emptyList() : routeResponse.stops())
                                .flatMap(stop -> getPredictionByStopTagAndRoute(agencyTag, stop.tag(),
                                                routeResponse.tag())
                                                .timeout(Duration.ofSeconds(3))
                                                .map(bodyPredictions -> StopDTO.Builder.from(stop)
                                                                .predictions(bodyPredictions.predictions())
                                                                .build()),
                                                stopConcurrency)
                                .collectList()
                                .map(enrichedStops -> {

                                        RouteResponse routeResponseWithStops = RouteResponse.Builder
                                                        .from(routeResponse)
                                                        .agencyTag(agencyTag)
                                                        .stops(enrichedStops)
                                                        .build();

                                        return routeResponseWithStops;
                                });

        }

        public Mono<String> getPredictionByStopId(String agencyTag, long stopId) {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "predictions")
                                                .queryParam("a", agencyTag)
                                                .queryParam("stopId", stopId)
                                                .build())
                                .retrieve().bodyToMono(String.class);
        }

        public Mono<String> getPredictionByStopId(String agencyTag, long stopId, String routeTag) {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "predictions")
                                                .queryParam("a", agencyTag)
                                                .queryParam("stopId", stopId)
                                                .queryParam("routeTag", routeTag)
                                                .build())
                                .retrieve().bodyToMono(String.class);
        }

        public Mono<BodyPredictions> getPredictionByStopTagAndRoute(String agencyTag, String stopTag, String routeTag) {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "predictions")
                                                .queryParam("a", agencyTag)
                                                .queryParam("r", routeTag)
                                                .queryParam("s", stopTag)
                                                .build())
                                .retrieve().bodyToMono(String.class)
                                .map(xml -> doXmlMapping(xml, BodyPredictions.class));

        }

        public Mono<BodyVehicle> getRouteVehicles(String agencyTag, String routeTag) {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "vehicleLocations")
                                                .queryParam("a", agencyTag)
                                                .queryParam("r", routeTag)
                                                .queryParam("t", 0)
                                                .build())
                                .retrieve()
                                .bodyToMono(String.class)
                                .map(xml -> doXmlMapping(xml, BodyVehicle.class));
        }

        public Mono<BodyVehicle> getVehicleById(String agencyTag, String vehicleId) {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "vehicleLocations")
                                                .queryParam("a", agencyTag)
                                                .queryParam("v", vehicleId)
                                                .build())
                                .retrieve()
                                .bodyToMono(String.class)
                                .map(xml -> doXmlMapping(xml, BodyVehicle.class));
        }

        public <T> T doXmlMapping(String xmlString, Class<T> desiredClass) {
                try {

                        XmlMapper xmlMapper = XmlMapper.builder()
                                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                        .configure(MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES, true)
                                        .build();

                        xmlMapper.registerModule(new ParameterNamesModule());

                        T clazz = xmlMapper.readValue(xmlString, desiredClass);
                        System.out.println(clazz);

                        return clazz;
                } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                }
        }

        public Flux<RouteResponse> searchBestRouteForUserDestination(TravelDTO travel)
                        throws JsonMappingException, JsonProcessingException {

                Mono<AgencyList> monoAgencyList = getAgencies();

                Mono<List<RouteListResponse>> routeListMono = monoAgencyList
                                .flatMap(agencyList -> Flux.fromIterable(agencyList.agencies())
                                                .flatMap(agency -> getAgencyRoutes(agency)
                                                                .map(routeList -> {
                                                                        return RouteListMapper.fromRouteList(routeList)
                                                                                        .agencyTag(agency.tag())
                                                                                        .build();
                                                                }))
                                                .collectList());

                Flux<Tuple2<RouteResponse, List<StopDTO>>> fluxRoute = routeListMono
                                .flatMapMany(list -> Flux.fromIterable(list == null ? Collections.emptyList() : list))
                                .flatMap(routeList -> Flux
                                                .fromIterable(routeList.routes() == null ? Collections.emptyList()
                                                                : routeList.routes())
                                                .publishOn(Schedulers.parallel())
                                                .map(route -> {

                                                        RouteResponse routeResponse = RouteMapper.fromRoute(route)
                                                                        .agencyTag(routeList.agencyTag())
                                                                        .build();

                                                        List<StopDTO> stopsWithDistance = routeResponse.stops().stream()
                                                                        .map(stop -> {
                                                                                double distance = HaversineUtils
                                                                                                .distanceMeters(
                                                                                                                new LatLng(travel
                                                                                                                                .latTo(),
                                                                                                                                travel.lonTo()),
                                                                                                                new LatLng(stop.lat(),
                                                                                                                                stop.lon()));

                                                                                StopDTO stopWithDistanceFromUserDestination = StopDTO.Builder
                                                                                                .from(stop)
                                                                                                .distanceFromUserDestination(
                                                                                                                distance)
                                                                                                .build();

                                                                                return stopWithDistanceFromUserDestination;
                                                                        }).collect(Collectors.toList());

                                                        return Tuples.of(routeResponse, stopsWithDistance);
                                                }));

                Flux<Tuple2<RouteResponse, List<StopDTO>>> filteredRoutes = fluxRoute.filter(tuple -> tuple.getT2()
                                .stream()
                                .anyMatch(stopDistance -> stopDistance.distanceFromUserDestination() <= 1000));

                Flux<RouteResponse> orderedRoutes = filteredRoutes.map(tuple -> {
                        RouteResponse routeResponse = RouteResponse.Builder.from(tuple.getT1()).stops(tuple.getT2())
                                        .build();

                        double minDistance = tuple.getT2().stream().mapToDouble(StopDTO::distanceFromUserDestination)
                                        .min().orElse(Double.MAX_VALUE);

                        return Tuples.of(routeResponse, minDistance);
                }).sort(Comparator.comparingDouble(Tuple2::getT2))
                                .map(Tuple2::getT1);

                return orderedRoutes;
        }
}
