package com.devluanmarcene.RealTimeBusTracker.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devluanmarcene.RealTimeBusTracker.model.Route;
import com.devluanmarcene.RealTimeBusTracker.model.Travel;
import com.devluanmarcene.RealTimeBusTracker.service.BusTrackService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("api/bustrack")
public class BusTrackController {

   private BusTrackService busTrackService;

   public BusTrackController(BusTrackService busTrackService) {
      this.busTrackService = busTrackService;
   }

   @PostMapping("/search")
   public Flux<Route> searchNearbyBuses(@RequestBody Travel travel)
         throws JsonMappingException, JsonProcessingException {
      return busTrackService.getNearbyBuses(travel);
   }
}
