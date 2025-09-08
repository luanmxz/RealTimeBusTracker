package com.devluanmarcene.RealTimeBusTracker.mapper;

import com.devluanmarcene.RealTimeBusTracker.dto.response.RouteListResponse;
import com.devluanmarcene.RealTimeBusTracker.model.RouteList;

public class RouteListMapper {

    public static RouteListResponse.Builder fromRouteList(RouteList routeList) {
        return new RouteListResponse.Builder()
                .agencyTag(routeList.agencyTag())
                .routes(routeList.routes());
    }
}
