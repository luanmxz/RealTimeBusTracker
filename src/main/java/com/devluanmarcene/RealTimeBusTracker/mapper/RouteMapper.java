package com.devluanmarcene.RealTimeBusTracker.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.devluanmarcene.RealTimeBusTracker.dto.internal.StopDTO;
import com.devluanmarcene.RealTimeBusTracker.dto.response.RouteResponse;
import com.devluanmarcene.RealTimeBusTracker.model.Route;

public class RouteMapper {

    public static RouteResponse.Builder fromRoute(Route route) {
        List<StopDTO> stopsDTO = route.stops().stream()
                .map(stop -> StopMapper.fromStop(stop).build())
                .collect(Collectors.toList());

        return new RouteResponse.Builder()
                .tag(route.tag())
                .title(route.title())
                .color(route.color())
                .latMin(route.latMin() != null ? route.latMin() : 0)
                .latMax(route.latMax() != null ? route.latMax() : 0)
                .lonMin(route.lonMin() != null ? route.lonMin() : 0)
                .lonMax(route.lonMax() != null ? route.lonMax() : 0)
                .stops(stopsDTO)
                .direction(route.direction())
                .paths(route.paths());
    }
}
