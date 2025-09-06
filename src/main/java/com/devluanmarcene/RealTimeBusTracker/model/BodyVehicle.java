package com.devluanmarcene.RealTimeBusTracker.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "body")
@JsonIgnoreProperties(value = "copyright")
public record BodyVehicle(
                @JacksonXmlProperty(localName = "vehicle") @JacksonXmlElementWrapper(useWrapping = false) List<Vehicle> vehicles) {

}
