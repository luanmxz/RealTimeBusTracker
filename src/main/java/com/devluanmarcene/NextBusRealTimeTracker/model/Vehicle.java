package com.devluanmarcene.NextBusRealTimeTracker.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record Vehicle(
        @JacksonXmlProperty(isAttribute = true) String id,
        @JacksonXmlProperty(isAttribute = true) String routeTag,
        @JacksonXmlProperty(isAttribute = true) String dirTag,
        @JacksonXmlProperty(isAttribute = true) double lat,
        @JacksonXmlProperty(isAttribute = true) double lon,
        @JacksonXmlProperty(isAttribute = true) int secsSinceReport,
        @JacksonXmlProperty(isAttribute = true) boolean predictable,
        @JacksonXmlProperty(isAttribute = true) int heading) {

}
