package com.devluanmarcene.NextBusRealTimeTracker.mapper;

import com.devluanmarcene.NextBusRealTimeTracker.dto.response.RouteListResponse;
import com.devluanmarcene.NextBusRealTimeTracker.model.RouteList;

public class RouteListMapper {

    public static RouteListResponse.Builder fromRouteList(RouteList routeList) {
        return new RouteListResponse.Builder()
                .agencyTag(routeList.agencyTag())
                .routes(routeList.routes());
    }
}
