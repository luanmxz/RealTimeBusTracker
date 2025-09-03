package com.devluanmarcene.RealTimeBusTracker.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record Message(@JacksonXmlProperty(isAttribute = true) String text) {

}
