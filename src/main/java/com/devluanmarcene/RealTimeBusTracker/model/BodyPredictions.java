package com.devluanmarcene.RealTimeBusTracker.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JacksonXmlRootElement(localName = "body")
@JsonIgnoreProperties(value = "copyright")
public class BodyPredictions {

    @JacksonXmlProperty(localName = "predictions", isAttribute = true)
    private Predictions predictions;

    public Predictions getPredictions() {
        return predictions;
    }

    public void setPredictions(Predictions predictions) {
        this.predictions = predictions;
    }
}