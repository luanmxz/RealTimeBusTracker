package com.devluanmarcene.RealTimeBusTracker.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record Stop(
                @JacksonXmlProperty(isAttribute = true) String tag,
                @JacksonXmlProperty(isAttribute = true) String title,
                @JacksonXmlProperty(isAttribute = true) String shortTitle,
                @JacksonXmlProperty(isAttribute = true) double lat,
                @JacksonXmlProperty(isAttribute = true) double lon,
                @JacksonXmlProperty(isAttribute = true) long stopId,
                double distanceFromUserDestination,
                Predictions predictions) {

        public static Stop withDistanceFromUserDestination(Stop stop, double distanceFromUserDestination) {
                return new Stop(stop.tag(), stop.title(), stop.shortTitle(), stop.lat(), stop.lon(), stop.stopId(),
                                distanceFromUserDestination, stop.predictions());
        }

        public static Stop withPredictions(Stop stop, Predictions predictions) {
                return new Stop(stop.tag(), stop.title(), stop.shortTitle(), stop.lat(), stop.lon(), stop.stopId(),
                                0, predictions);
        }

}
