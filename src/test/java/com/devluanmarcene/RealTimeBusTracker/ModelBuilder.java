package com.devluanmarcene.RealTimeBusTracker;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

import com.devluanmarcene.RealTimeBusTracker.dto.internal.StopDTO;
import com.devluanmarcene.RealTimeBusTracker.dto.response.RouteResponse;
import com.devluanmarcene.RealTimeBusTracker.model.Agency;
import com.devluanmarcene.RealTimeBusTracker.model.AgencyList;
import com.devluanmarcene.RealTimeBusTracker.model.Predictions;

class ModelBuilder {

    public static AgencyList createAgencyList() {
        Agency agency1 = new Agency("tag", "title", "regionTitle", "shortTitle");
        AgencyList agencyList = new AgencyList(List.of(agency1));

        return agencyList;
    }

    public static RouteResponse createRouteResponse() {
        Predictions predictions1 = new Predictions("predictions1", "route1", "routeTitle1", "stopTitle1", "stop1", null,
                null, null);

        StopDTO stop1 = new StopDTO.Builder()
                .tag("stop1")
                .title("stopTitle1")
                .lat(39.1628136)
                .lon(-76.891511)
                .stopId(1L)
                .distanceFromUserDestination(0)
                .predictions(predictions1).build();

        RouteResponse route1 = new RouteResponse.Builder()
                .tag("route1")
                .title("routeTitle1")
                .color("#fff")
                .agencyTag("agencyTag1")
                .latMax(Random.from(RandomGenerator.getDefault()).nextDouble())
                .latMin(Random.from(RandomGenerator.getDefault()).nextDouble())
                .lonMax(Random.from(RandomGenerator.getDefault()).nextDouble())
                .lonMin(Random.from(RandomGenerator.getDefault()).nextDouble())
                .stops(List.of(stop1))
                .build();

        return route1;
    }
}
