package com.devluanmarcene.RealTimeBusTracker.service;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.devluanmarcene.RealTimeBusTracker.config.Constants;
import com.devluanmarcene.RealTimeBusTracker.config.WebClientConfig;
import com.devluanmarcene.RealTimeBusTracker.model.Agency;
import com.devluanmarcene.RealTimeBusTracker.model.AgencyList;
import com.devluanmarcene.RealTimeBusTracker.model.BodyPredictions;
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

@Service
public class BusTrackService {

    private WebClientConfig webClientConfig;
    private Constants constants;

    public BusTrackService(WebClientConfig webClientConfig, Constants constants) {
        this.webClientConfig = webClientConfig;
        this.constants = constants;
    }

    public Mono<BodyPredictions> getNearbyBuses(Travel travel)
            throws JsonMappingException, JsonProcessingException {

        System.out
                .println(String.format("\nUser in:\n - Latitude: %f\n - Longitude: %f\n", travel.latFrom(),
                        travel.lonFrom()));

        System.out
                .println(String.format("\nDestination in:\n - Latitude: %f\n - Longitude: %f\n", travel.latTo(),
                        travel.lonTo()));

        Mono<AgencyList> monoAgencyList = getAgencies().map(xmlString -> {
            return doXmlMapping(xmlString, AgencyList.class);
        });

        Mono<List<RouteList>> routeListMono = monoAgencyList
                .flatMap(agencyList -> Flux.fromIterable(agencyList.getAgencies())
                        .flatMap(this::getAgencyRoutes)
                        .map(xml -> doXmlMapping(xml, RouteList.class))
                        .collectList());

        routeListMono.doOnNext(list -> list.forEach(r -> System.out.println(r)))
                .subscribe();

        /*
         * System.out.println("AGENCY TAG -> " +
         * monoAgencyList.block().getAgencies().get(0).getTag());
         * Mono<String> xmlRouteList = monoAgencyList
         * .flatMap(agencyList -> getAgencyRoutes(agencyList.getAgencies().get(0)));
         * Mono<RouteList> monoRouteList = xmlRouteList.map(xmlString -> {
         * return doXmlMapping(xmlString, RouteList.class);
         * });
         * System.out.println(
         * "ROUTE LIST 0 STOP 0 STOP ID -> " +
         * monoRouteList.block().routes().get(0).stops().get(0).stopId());
         * 
         * // Mono<String> xmlBodyPredicitons = monoRouteList
         * // .flatMap(routeList -> getPredictionByStopId("jhu-apl",
         * // routeList.routes().get(3).stops().get(0).stopId()));
         * 
         * // Mono<BodyPredictions> monoPredictions = xmlBodyPredicitons.map(xmlString
         * -> {
         * // return doXmlMapping(xmlString, BodyPredictions.class);
         * // });
         * 
         */
        return null;
    }

    public Mono<String> getAgencies() {
        return webClientConfig.getWebClient().get()
                .uri(constants.API_AGENCY_LIST())
                .accept(MediaType.APPLICATION_XML)
                .retrieve().bodyToMono(String.class);
    }

    public Mono<String> getAgencyRoutes(Agency agency) {
        return webClientConfig.getWebClient().get()
                .uri("https://retro.umoiq.com/service/publicXMLFeed?command=routeConfig&a={agencyTag}", agency.getTag())
                .retrieve().bodyToMono(String.class);
    }

    /**
     * There are two ways to specify the stop:
     * 1) using a stopId
     * Um stopId é util para quando o usuário quer dizer: Quero ir até ali (stopID)
     * e ai retornamos multiplas rotas possiveis de acordo com o stopID (porém nao é
     * garantido que o stopID se refira a um local destino unico.)
     * or
     * 2) by specifying the route and stop tags
     * Por outro lado, uma stop tag é um identificador unico para um stop fisico. É
     * usado junto com um route specifier para identificar uma parada.
     * It is typically used when creating a User Interface where a
     * user selects a route, direction, and then stop. It is also used when
     * predictions are needed for only
     * a single route served by a stop.
     * 
     * @return
     */
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

    public Mono<String> getPredictionByStopTagAndRoute(String agencyTag, String stopTag, String routeTag) {
        return webClientConfig.getWebClient().get()
                .uri(" https://retro.umoiq.com/service/publicXMLFeed?command=predictions&a={agencyTag}&r={routeTag}&s={stopTag}",
                        agencyTag, stopTag, routeTag)
                .retrieve().bodyToMono(String.class);
    }

    public <T> T doXmlMapping(String xmlString, Class<T> desiredClass) {
        try {

            XmlMapper xmlMapper = XmlMapper.builder()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES, true)
                    .build();
            xmlMapper.registerModule(new ParameterNamesModule()); // vincula nomes de parâmetros ao construtor
            // (records)
            return xmlMapper.readValue(xmlString, desiredClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
