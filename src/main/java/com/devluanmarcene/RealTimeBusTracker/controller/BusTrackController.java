package com.devluanmarcene.RealTimeBusTracker.controller;

import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devluanmarcene.RealTimeBusTracker.model.AgencyList;
import com.devluanmarcene.RealTimeBusTracker.model.Route;
import com.devluanmarcene.RealTimeBusTracker.model.request.AgencyRoutesRequest;
import com.devluanmarcene.RealTimeBusTracker.service.BusTrackService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/bustrack")
public class BusTrackController {

   private BusTrackService busTrackService;

   public BusTrackController(BusTrackService busTrackService) {
      this.busTrackService = busTrackService;
   }

   @GetMapping(path = "/routes", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
   public Flux<Route> getRoutesByAgencyTag(
         @RequestParam(name = "agency") AgencyRoutesRequest agencyRoutesRequest)
         throws JsonMappingException, JsonProcessingException {

      return Flux.interval(Duration.ofSeconds(15))
            .flatMap(tick -> busTrackService.getRoutesByAgencyTagWithPredictions(agencyRoutesRequest.agencyTag()));
   }

   @GetMapping("/agencies")
   public Mono<AgencyList> getAgencies() {
      return busTrackService.getAgencies();
   }
}
