package com.devluanmarcene.RealTimeBusTracker.service;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.devluanmarcene.RealTimeBusTracker.config.Constants;
import com.devluanmarcene.RealTimeBusTracker.config.WebClientConfig;
import com.devluanmarcene.RealTimeBusTracker.dto.internal.StopDTO;
import com.devluanmarcene.RealTimeBusTracker.dto.response.RouteListResponse;
import com.devluanmarcene.RealTimeBusTracker.dto.response.RouteResponse;
import com.devluanmarcene.RealTimeBusTracker.helpers.HaversineUtils;
import com.devluanmarcene.RealTimeBusTracker.mapper.RouteListMapper;
import com.devluanmarcene.RealTimeBusTracker.mapper.RouteMapper;
import com.devluanmarcene.RealTimeBusTracker.model.Agency;
import com.devluanmarcene.RealTimeBusTracker.model.AgencyList;
import com.devluanmarcene.RealTimeBusTracker.model.BodyPredictions;
import com.devluanmarcene.RealTimeBusTracker.model.BodyVehicle;
import com.devluanmarcene.RealTimeBusTracker.model.LatLng;
import com.devluanmarcene.RealTimeBusTracker.model.Route;
import com.devluanmarcene.RealTimeBusTracker.model.RouteList;
import com.devluanmarcene.RealTimeBusTracker.model.Travel;
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
    private Constants constants;

    public BusTrackService(WebClientConfig webClientConfig, Constants constants) {
        this.webClientConfig = webClientConfig;
        this.constants = constants;
    }

    public Mono<AgencyList> getAgencies() {
        return webClientConfig.getWebClient().get()
                .uri(constants.API_AGENCY_LIST())
                .accept(MediaType.APPLICATION_XML)
                .retrieve().bodyToMono(String.class)
                .map(xmlString -> doXmlMapping(xmlString, AgencyList.class));
    }

    public Mono<RouteList> getAgencyRoutes(Agency agency) {
        return webClientConfig.getWebClient().get()
                .uri("https://retro.umoiq.com/service/publicXMLFeed?command=routeConfig&a={agencyTag}",
                        agency.tag())
                .retrieve().bodyToMono(String.class)
                .flatMap(xml -> Mono.fromCallable(() -> doXmlMapping(xml, RouteList.class))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    public Flux<Route> getRoutesByAgencyTag(String agencyTag) {
        return webClientConfig.getWebClient().get()
                .uri("https://retro.umoiq.com/service/publicXMLFeed?command=routeConfig&a={agencyTag}",
                        agencyTag)
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
                .uri("https://retro.umoiq.com/service/publicXMLFeed?command=routeConfig&a={agencyTag}",
                        agencyTag)
                .retrieve().bodyToMono(String.class)
                .flatMap(xml -> Mono.fromCallable(() -> doXmlMapping(xml, RouteList.class))
                        .subscribeOn(Schedulers.boundedElastic()))
                .flatMapMany(routeList -> Flux
                        .fromIterable(routeList.routes() == null ? Collections.emptyList()
                                : routeList.routes())
                        .subscribeOn(Schedulers.parallel())
                        .flatMap(route -> {
                            RouteResponse routeResponse = RouteMapper.fromRoute(route).build();
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

        return Flux.fromIterable(routeResponse.stops() == null ? Collections.emptyList() : routeResponse.stops())
                .flatMap(stop -> getPredictionByStopTagAndRoute(agencyTag, stop.tag(), routeResponse.tag())
                        .timeout(Duration.ofSeconds(3))
                        .map(bodyPredictions -> StopDTO.Builder.from(stop).predictions(bodyPredictions.predictions())
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
                .uri("https://retro.umoiq.com/service/publicXMLFeed?command=predictions&a={agencyTag}&stopId={stopId}",
                        agencyTag, stopId)
                .retrieve().bodyToMono(String.class);
    }

    public Mono<String> getPredictionByStopId(String agencyTag, long stopId, String routeTag) {
        return webClientConfig.getWebClient().get()
                .uri("https://retro.umoiq.com/service/publicXMLFeed?command=predictions&a={agencyTag}&stopId={stopId}&routeTag={routeTag}",
                        agencyTag, stopId, routeTag)
                .retrieve().bodyToMono(String.class);
    }

    public Mono<BodyPredictions> getPredictionByStopTagAndRoute(String agencyTag, String stopTag, String routeTag) {
        return webClientConfig.getWebClient().get()
                .uri("https://retro.umoiq.com/service/publicXMLFeed?command=predictions&a={agencyTag}&r={routeTag}&s={stopTag}",
                        agencyTag, routeTag, stopTag)
                .retrieve().bodyToMono(String.class)
                .map(xml -> doXmlMapping(xml, BodyPredictions.class));

    }

    public Mono<BodyVehicle> getRouteVehicles(String agencyTag, String routeTag) {
        return webClientConfig.getWebClient().get()
                .uri("https://retro.umoiq.com/service/publicXMLFeed?command=vehicleLocations&a={agencyTag}&r={routeTag}&t=0",
                        agencyTag, routeTag)
                .retrieve()
                .bodyToMono(String.class)
                .map(xml -> doXmlMapping(xml, BodyVehicle.class));
    }

    public Mono<BodyVehicle> getVehicleById(String agencyTag, String vehicleId) {
        return webClientConfig.getWebClient().get()
                .uri("https://retro.umoiq.com/service/publicXMLFeed?command=vehicleLocation&a={agencyTag}&v={vehicleId}",
                        agencyTag, vehicleId)
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

    public Flux<RouteResponse> searchBestRouteForUserDestination(Travel travel)
            throws JsonMappingException, JsonProcessingException {

        Mono<AgencyList> monoAgencyList = getAgencies();

        Mono<List<RouteListResponse>> routeListMono = monoAgencyList
                .flatMap(agencyList -> Flux.fromIterable(agencyList.agencies())
                        .flatMap(agency -> getAgencyRoutes(agency)
                                .map(routeList -> {
                                    return RouteListMapper.fromRouteList(routeList).agencyTag(agency.tag()).build();
                                }))
                        .collectList());

        Flux<Tuple2<RouteResponse, List<StopDTO>>> fluxRoute = routeListMono
                .flatMapMany(list -> Flux.fromIterable(list == null ? Collections.emptyList() : list))
                .flatMap(routeList -> Flux
                        .fromIterable(routeList.routes() == null ? Collections.emptyList()
                                : routeList.routes())
                        .publishOn(Schedulers.parallel())
                        .map(route -> {

                            RouteResponse routeResponse = RouteMapper.fromRoute(route).agencyTag(routeList.agencyTag())
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

                                        StopDTO stopWithDistanceFromUserDestination = StopDTO.Builder.from(stop)
                                                .distanceFromUserDestination(distance).build();

                                        return stopWithDistanceFromUserDestination;
                                    }).collect(Collectors.toList());

                            return Tuples.of(routeResponse, stopsWithDistance);
                        }));

        Flux<Tuple2<RouteResponse, List<StopDTO>>> filteredRoutes = fluxRoute.filter(tuple -> tuple.getT2().stream()
                .anyMatch(stopDistance -> stopDistance.distanceFromUserDestination() <= 1000));

        Flux<RouteResponse> orderedRoutes = filteredRoutes.map(tuple -> {
            RouteResponse routeResponse = RouteResponse.Builder.from(tuple.getT1()).stops(tuple.getT2()).build();

            double minDistance = tuple.getT2().stream().mapToDouble(StopDTO::distanceFromUserDestination)
                    .min().orElse(Double.MAX_VALUE);

            return Tuples.of(routeResponse, minDistance);
        }).sort(Comparator.comparingDouble(Tuple2::getT2))
                .map(Tuple2::getT1);

        return orderedRoutes;
    }
}
