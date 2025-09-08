package com.devluanmarcene.NextBusRealTimeTracker.model;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "path")
public record Path(
        @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "point") List<Point> points) {

}
