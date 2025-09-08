package com.devluanmarcene.NextBusRealTimeTracker.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record Stop(
                @JacksonXmlProperty(isAttribute = true) String tag,
                @JacksonXmlProperty(isAttribute = true) String title,
                @JacksonXmlProperty(isAttribute = true) String shortTitle,
                @JacksonXmlProperty(isAttribute = true) double lat,
                @JacksonXmlProperty(isAttribute = true) double lon,
                @JacksonXmlProperty(isAttribute = true) long stopId) {
}
