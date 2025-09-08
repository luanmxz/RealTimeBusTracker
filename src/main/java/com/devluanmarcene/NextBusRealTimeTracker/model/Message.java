package com.devluanmarcene.NextBusRealTimeTracker.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record Message(@JacksonXmlProperty(isAttribute = true) String text) {

}
