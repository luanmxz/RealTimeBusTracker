package com.devluanmarcene.RealTimeBusTracker.model;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record Predictions(
        @JacksonXmlProperty(isAttribute = true) String agencyTitle,
        @JacksonXmlProperty(isAttribute = true) String routeTag,
        @JacksonXmlProperty(isAttribute = true) String routeTitle,
        @JacksonXmlProperty(isAttribute = true) String stopTitle,
        @JacksonXmlProperty(isAttribute = true) String stopTag,
        @JacksonXmlProperty(isAttribute = true) String dirTitleBecauseNoPredictions,
        @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "direction") List<Direction> directions,
        @JacksonXmlProperty(localName = "message") Message message) {

}
