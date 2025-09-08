package com.devluanmarcene.RealTimeBusTracker;

import java.util.List;

import com.devluanmarcene.RealTimeBusTracker.model.Agency;
import com.devluanmarcene.RealTimeBusTracker.model.AgencyList;
import com.devluanmarcene.RealTimeBusTracker.model.Predictions;
import com.devluanmarcene.RealTimeBusTracker.model.Route;
import com.devluanmarcene.RealTimeBusTracker.model.Stop;

class ModelBuilder {

    public static AgencyList createAgencyList() {
        Agency agency1 = new Agency("tag", "title", "regionTitle", "shortTitle");
        AgencyList agencyList = new AgencyList();
        agencyList.setAgencies(List.of(agency1));

        return agencyList;
    }

    public static Route createRoute() {
        Predictions predictions1 = new Predictions("predictions1", "route1", "routeTitle1", "stopTitle1", "stop1", null,
                null, null);

        Stop stop1 = new Stop("stop1", "stopTitle1", "shortTitle1", 39.1628136,
                -76.891511, 1L, 0, predictions1);

        Route route1 = new Route(
                "route1",
                "routeTitle1",
                "shortTitle1",
                "#ffff",
                "#000",
                "agencyTag1",
                39.1628136, -76.891511, 39.159605, -76.893621, List.of(stop1), null, null, null);

        return route1;
    }
}
