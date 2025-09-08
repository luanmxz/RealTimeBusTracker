package com.devluanmarcene.NextBusRealTimeTracker.model;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "direction")
public record Direction(
                @JacksonXmlProperty(isAttribute = true) String tag,
                @JacksonXmlProperty(isAttribute = true) String title,
                @JacksonXmlProperty(isAttribute = true) String name,
                @JacksonXmlProperty(isAttribute = true) boolean useForUI,
                @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "stop") List<Stop> stops,
                @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "prediction") List<Prediction> predictions) {
}
