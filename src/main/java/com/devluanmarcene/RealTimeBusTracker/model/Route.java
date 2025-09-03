package com.devluanmarcene.RealTimeBusTracker.model;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record Route(
        @JacksonXmlProperty(isAttribute = true) String tag,
        @JacksonXmlProperty(isAttribute = true) String title,
        @JacksonXmlProperty(isAttribute = true) String shortTitle,
        @JacksonXmlProperty(isAttribute = true) String color,
        @JacksonXmlProperty(isAttribute = true) String oppositeColor,
        @JacksonXmlProperty(isAttribute = true) Double latMin,
        @JacksonXmlProperty(isAttribute = true) Double latMax,
        @JacksonXmlProperty(isAttribute = true) Double lonMin,
        @JacksonXmlProperty(isAttribute = true) Double lonMax,
        @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "stop") List<Stop> stops,
        @JacksonXmlProperty(localName = "direction") Direction direction,
        @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "path") List<Path> paths) {
}
