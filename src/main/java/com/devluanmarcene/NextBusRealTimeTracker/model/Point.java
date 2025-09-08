package com.devluanmarcene.NextBusRealTimeTracker.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record Point(
        @JacksonXmlProperty(isAttribute = true) double lat,
        @JacksonXmlProperty(isAttribute = true) double lon) {

}
