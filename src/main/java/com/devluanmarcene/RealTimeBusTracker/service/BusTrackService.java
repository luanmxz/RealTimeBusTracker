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
import com.devluanmarcene.RealTimeBusTracker.helpers.HaversineUtils;
import com.devluanmarcene.RealTimeBusTracker.model.Agency;
import com.devluanmarcene.RealTimeBusTracker.model.AgencyList;
import com.devluanmarcene.RealTimeBusTracker.model.BodyPredictions;
import com.devluanmarcene.RealTimeBusTracker.model.LatLng;
import com.devluanmarcene.RealTimeBusTracker.model.Route;
import com.devluanmarcene.RealTimeBusTracker.model.RouteList;
import com.devluanmarcene.RealTimeBusTracker.model.Stop;
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
                                                agency.getTag())
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

        public Flux<Route> getRoutesByAgencyTagWithPredictions(String agencyTag) {
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
                                                .flatMap(route -> enrichRouteWithPredictions(route,
                                                                agencyTag), 8));
        }

        private Mono<Route> enrichRouteWithPredictions(Route route, String agencyTag) {
                final int stopConcurrency = 16;

                return Flux.fromIterable(route.stops() == null ? Collections.emptyList() : route.stops())
                                .flatMap(stop -> getPredictionByStopTagAndRoute(agencyTag, stop.tag(), route.tag())
                                                .timeout(Duration.ofSeconds(3))
                                                .map(bodyPredictions -> Stop.withPredictions(stop,
                                                                bodyPredictions.getPredictions())),
                                                stopConcurrency)
                                .collectList()
                                .map(enrichedStops -> {
                                        Route routeWithAgency = Route.createWithAgencyTag(route, agencyTag);
                                        Route routeWithStops = Route.createWithStops(routeWithAgency, enrichedStops);
                                        return routeWithStops;
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
                                .map(xml -> {
                                        return doXmlMapping(xml, BodyPredictions.class);
                                });

        }

        public <T> T doXmlMapping(String xmlString, Class<T> desiredClass) {
                try {

                        XmlMapper xmlMapper = XmlMapper.builder()
                                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                        .configure(MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES, true)
                                        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                                        .build();
                        xmlMapper.registerModule(new ParameterNamesModule());

                        T clazz = xmlMapper.readValue(xmlString, desiredClass);
                        System.out.println(clazz);

                        return clazz;
                } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                }
        }

        public Flux<Route> searchBestRouteForUserDestination(Travel travel)
                        throws JsonMappingException, JsonProcessingException {

                Mono<AgencyList> monoAgencyList = getAgencies();

                Mono<List<RouteList>> routeListMono = monoAgencyList
                                .flatMap(agencyList -> Flux.fromIterable(agencyList.getAgencies())
                                                .flatMap(agency -> getAgencyRoutes(agency)
                                                                .map(routeList -> {
                                                                        return RouteList.createWithAgencyTag(routeList,
                                                                                        agency.getTag());
                                                                }))
                                                .collectList());

                Flux<Tuple2<Route, List<Stop>>> fluxRoute = routeListMono
                                .flatMapMany(list -> Flux.fromIterable(list == null ? Collections.emptyList() : list))
                                .flatMap(routeList -> Flux
                                                .fromIterable(routeList.routes() == null ? Collections.emptyList()
                                                                : routeList.routes())
                                                .publishOn(Schedulers.parallel())
                                                .map(route -> {
                                                        Route newRoute = Route.createWithAgencyTag(route,
                                                                        routeList.agencyTag());

                                                        List<Stop> stopsWithDistance = newRoute.stops().stream()
                                                                        .map(stop -> {
                                                                                double distance = HaversineUtils
                                                                                                .distanceMeters(
                                                                                                                new LatLng(travel
                                                                                                                                .latTo(),
                                                                                                                                travel.lonTo()),
                                                                                                                new LatLng(stop.lat(),
                                                                                                                                stop.lon()));

                                                                                Stop enriched = Stop
                                                                                                .withDistanceFromUserDestination(
                                                                                                                stop,
                                                                                                                distance);

                                                                                return enriched;
                                                                        }).collect(Collectors.toList());

                                                        return Tuples.of(newRoute, stopsWithDistance);
                                                }));

                Flux<Tuple2<Route, List<Stop>>> filteredRoutes = fluxRoute.filter(tuple -> tuple.getT2().stream()
                                .anyMatch(stopDistance -> stopDistance.distanceFromUserDestination() <= 1000));

                Flux<Route> orderedRoutes = filteredRoutes.map(tuple -> {
                        Route newRoute = Route.createWithStops(tuple.getT1(), tuple.getT2());

                        double minDistance = tuple.getT2().stream().mapToDouble(Stop::distanceFromUserDestination)
                                        .min().orElse(Double.MAX_VALUE);

                        return Tuples.of(newRoute, minDistance);
                }).sort(Comparator.comparingDouble(Tuple2::getT2))
                                .map(Tuple2::getT1);

                return orderedRoutes;
        }
}
