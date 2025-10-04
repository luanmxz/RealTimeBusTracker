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
import com.devluanmarcene.NextBusRealTimeTracker.helpers.XmlUtils;
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
import com.fasterxml.jackson.databind.JsonMappingException;

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

        /**
         * Gets a list of agencies from retroumoiq api
         * 
         * @return Mono<AgencyList> -> contains a list of Agency
         */
        @Cacheable("agencies")
        public Mono<AgencyList> getAgencies() {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "agencyList")
                                                .build())
                                .accept(MediaType.APPLICATION_XML)
                                .retrieve().bodyToMono(String.class)
                                .map(xmlString -> XmlUtils.doXmlMapping(xmlString, AgencyList.class))
                                .doOnNext(agencyList -> {
                                        System.out.println("Agencies retrivied: [");
                                        for (Agency agency : agencyList.agencies()) {
                                                System.out.println(String.format("Agency tag %s - Agency Title %s",
                                                                agency.tag(), agency.title()));
                                        }
                                        System.out.println("]");
                                });
        }

        /**
         * Gets all routes from agency
         * 
         * @param agency
         * @return Mono<RouteList>
         */
        public Mono<RouteList> getAgencyRoutes(Agency agency) {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "routeConfig")
                                                .queryParam("a", agency)
                                                .build())
                                .retrieve().bodyToMono(String.class)
                                .flatMap(xml -> Mono.fromCallable(() -> XmlUtils.doXmlMapping(xml, RouteList.class))
                                                .subscribeOn(Schedulers.boundedElastic()));
        }

        /**
         * Gets all routes from agency
         * 
         * @param agencyTag
         * @return Flux<Route>
         */
        public Flux<Route> getRoutesByAgencyTag(String agencyTag) {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "routeConfig")
                                                .queryParam("a", agencyTag)
                                                .build())
                                .retrieve().bodyToMono(String.class)
                                .flatMap(xml -> Mono.fromCallable(() -> XmlUtils.doXmlMapping(xml, RouteList.class))
                                                .subscribeOn(Schedulers.boundedElastic()))
                                .flatMapMany(routeList -> Flux
                                                .fromIterable(routeList.routes() == null
                                                                ? Collections.emptyList()
                                                                : routeList.routes())
                                                .subscribeOn(Schedulers.parallel()));

        }

        /**
         * Gets all routes from agency, and enrich the route with their stop predictions
         * and info about the bus vehicle doing that route
         * 
         * @param agencyTag
         * @return Flux<RouteResponse>
         */
        public Flux<RouteResponse> getRoutesByAgencyTagWithPredictions(String agencyTag) {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "routeConfig")
                                                .queryParam("a", agencyTag)
                                                .build())
                                .retrieve().bodyToMono(String.class)
                                .flatMap(xml -> Mono.fromCallable(() -> XmlUtils.doXmlMapping(xml, RouteList.class))
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

        /**
         * Enrich a route with stop predictions
         * 
         * @param routeResponse
         * @param agencyTag
         * @return Mono<RouteResponse>
         */
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

        /**
         * Get the predictions of and stop by the stop id
         * 
         * @param agencyTag
         * @param stopId
         * @return Mono<String>
         */
        public Mono<String> getPredictionByStopId(String agencyTag, long stopId) {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "predictions")
                                                .queryParam("a", agencyTag)
                                                .queryParam("stopId", stopId)
                                                .build())
                                .retrieve().bodyToMono(String.class);
        }

        /**
         * Get the predictions of and stop by the stop id and route tag
         * 
         * @param agencyTag
         * @param stopId
         * @param routeTag
         * @return Mono<String>
         */
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

        /**
         * Get the predictions of and stop by the stop tag and route tag
         * 
         * @param agencyTag
         * @param stopTag
         * @param routeTag
         * @return Mono<BodyPredictions>
         */
        public Mono<BodyPredictions> getPredictionByStopTagAndRoute(String agencyTag, String stopTag, String routeTag) {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "predictions")
                                                .queryParam("a", agencyTag)
                                                .queryParam("r", routeTag)
                                                .queryParam("s", stopTag)
                                                .build())
                                .retrieve().bodyToMono(String.class)
                                .map(xml -> XmlUtils.doXmlMapping(xml, BodyPredictions.class));

        }

        /**
         * Get info about all vehicles of a route
         * 
         * @param agencyTag
         * @param routeTag
         * @return Mono<BodyVehicle>
         */
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
                                .map(xml -> XmlUtils.doXmlMapping(xml, BodyVehicle.class));
        }

        /**
         * Get info about a specificy vehicle
         * 
         * @param agencyTag
         * @param vehicleId
         * @return Mono<BodyVehicle>
         */
        public Mono<BodyVehicle> getVehicleById(String agencyTag, String vehicleId) {
                return webClientConfig.getWebClient().get()
                                .uri(uriBuilder -> uriBuilder
                                                .queryParam("command", "vehicleLocations")
                                                .queryParam("a", agencyTag)
                                                .queryParam("v", vehicleId)
                                                .build())
                                .retrieve()
                                .bodyToMono(String.class)
                                .map(xml -> XmlUtils.doXmlMapping(xml, BodyVehicle.class));
        }

        /**
         * Search the nearby stop from the user destination (travel latTo - lonTo),
         * return the route that contain the stop. The returned route is ordered by the
         * Stop distance from the user destination
         * 
         * @param travel
         * @return Flux<RouteResponse>
         * @throws JsonMappingException
         * @throws JsonProcessingException
         */
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
