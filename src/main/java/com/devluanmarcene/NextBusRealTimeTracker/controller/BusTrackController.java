package com.devluanmarcene.NextBusRealTimeTracker.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devluanmarcene.NextBusRealTimeTracker.dto.request.AgencyRoutesRequest;
import com.devluanmarcene.NextBusRealTimeTracker.dto.response.RouteResponse;
import com.devluanmarcene.NextBusRealTimeTracker.model.AgencyList;
import com.devluanmarcene.NextBusRealTimeTracker.service.BusTrackService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/bustrack")
public class BusTrackController {

   private BusTrackService busTrackService;

   @Value("${routes.poll.interval:15}")
   private int interval;

   public BusTrackController(BusTrackService busTrackService) {
      this.busTrackService = busTrackService;
   }

   @GetMapping(path = "/routes", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
   public Flux<RouteResponse> getRoutesByAgencyTag(
         @RequestParam(name = "agency") AgencyRoutesRequest agencyRoutesRequest)
         throws JsonMappingException, JsonProcessingException {

      return Flux.interval(Duration.ofSeconds(interval))
            .flatMap(tick -> busTrackService.getRoutesByAgencyTagWithPredictions(agencyRoutesRequest.agencyTag()));
   }

   @GetMapping("/agencies")
   public Mono<AgencyList> getAgencies() {
      return busTrackService.getAgencies();
   }
}
