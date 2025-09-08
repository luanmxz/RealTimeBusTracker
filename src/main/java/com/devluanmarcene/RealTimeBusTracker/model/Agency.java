package com.devluanmarcene.RealTimeBusTracker.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record Agency(@JacksonXmlProperty(isAttribute = true) String tag,
        @JacksonXmlProperty(isAttribute = true) String title,
        @JacksonXmlProperty(isAttribute = true) String regionTitle,
        @JacksonXmlProperty(isAttribute = true) String shortTitle) {

}
