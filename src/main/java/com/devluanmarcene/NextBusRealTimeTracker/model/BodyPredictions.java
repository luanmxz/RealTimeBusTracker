package com.devluanmarcene.NextBusRealTimeTracker.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JacksonXmlRootElement(localName = "body")
@JsonIgnoreProperties(value = "copyright")
public record BodyPredictions(
        @JacksonXmlProperty(localName = "predictions", isAttribute = true) Predictions predictions) {
}