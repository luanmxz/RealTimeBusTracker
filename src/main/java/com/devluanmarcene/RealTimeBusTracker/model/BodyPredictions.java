package com.devluanmarcene.RealTimeBusTracker.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JacksonXmlRootElement(localName = "body")
public class BodyPredictions {

    @JacksonXmlProperty(localName = "predictions")
    private Predictions predictions;

    public Predictions getPredictions() {
        return predictions;
    }

    public void setPredictions(Predictions predictions) {
        this.predictions = predictions;
    }
}