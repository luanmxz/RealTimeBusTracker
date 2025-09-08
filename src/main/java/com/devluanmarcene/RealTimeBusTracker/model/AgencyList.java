package com.devluanmarcene.RealTimeBusTracker.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "body")
@JsonIgnoreProperties(value = "copyright")
public record AgencyList(
        @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "agency") List<Agency> agencies) {

}
