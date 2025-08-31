package com.devluanmarcene.RealTimeBusTracker.controller;

import java.util.StringTokenizer;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devluanmarcene.RealTimeBusTracker.config.Constants;
import com.devluanmarcene.RealTimeBusTracker.config.WebClientConfig;
import com.devluanmarcene.RealTimeBusTracker.model.AgencyList;
import com.devluanmarcene.RealTimeBusTracker.model.GeoLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/location")
public class UserController {

    private WebClientConfig webClientConfig;
    private Constants constants;

    public UserController(WebClientConfig webClientConfig, Constants constants) {
        this.webClientConfig = webClientConfig;
        this.constants = constants;
    }

    @RequestMapping("/get")
    public Mono<AgencyList> getNearbyVehicles(HttpServletRequest request)
            throws JsonMappingException, JsonProcessingException {
        String ip = getIp(request);
        Mono<GeoLocation> userLocation = getUserLocation(ip);
        Mono<String> xmlAgencyList = userLocation.flatMap(user -> getAgencies());
        Mono<AgencyList> monoAgencyList = xmlAgencyList.map(xmlString -> {
            try {
                XmlMapper xmlMapper = new XmlMapper();
                return xmlMapper.readValue(xmlString, AgencyList.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        return monoAgencyList;
    }

    public Mono<String> getAgencies() {
        return webClientConfig.getWebClient().get()
                .uri(constants.API_AGENCY_LIST())
                .accept(MediaType.APPLICATION_XML)
                .retrieve().bodyToMono(String.class);
    }

    public Mono<GeoLocation> getUserLocation(String ip) {
        return webClientConfig.getWebClient().get()
                .uri(constants.API_IP_LOCATION() + "{ip}", ip)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(GeoLocation.class);
    }

    public String getIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");

        if (ip != null) {
            ip = new StringTokenizer(ip, "").nextToken().trim();
            System.out.println(String.format("IP Address: %s", ip));
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }
}
