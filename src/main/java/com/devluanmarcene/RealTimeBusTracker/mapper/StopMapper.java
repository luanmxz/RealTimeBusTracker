package com.devluanmarcene.RealTimeBusTracker.mapper;

import com.devluanmarcene.RealTimeBusTracker.dto.internal.StopDTO;
import com.devluanmarcene.RealTimeBusTracker.model.Stop;

public class StopMapper {

    public static StopDTO.Builder fromStop(Stop stop) {
        return new StopDTO.Builder().tag(stop.tag()).title(stop.title()).stopId(stop.stopId()).lat(stop.lat())
                .lon(stop.lon());
    }
}
