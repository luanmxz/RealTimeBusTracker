package com.devluanmarcene.RealTimeBusTracker.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Predictions(
                @JacksonXmlProperty(isAttribute = true) String agencyTitle,
                @JacksonXmlProperty(isAttribute = true) String routeTag,
                @JacksonXmlProperty(isAttribute = true) String routeTitle,
                @JacksonXmlProperty(isAttribute = true) String stopTitle,
                @JacksonXmlProperty(isAttribute = true) String stopTag,
                @JacksonXmlProperty(isAttribute = true) String dirTitleBecauseNoPredictions,
                @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "direction") List<Direction> directions,
                @JacksonXmlProperty(localName = "message") Message message) {

        public Predictions {
                agencyTitle = agencyTitle == null ? "" : agencyTitle;
                routeTag = routeTag == null ? "" : routeTag;
                routeTitle = routeTitle == null ? "" : routeTitle;
                stopTitle = stopTitle == null ? "" : stopTitle;
                stopTag = stopTag == null ? "" : stopTag;
                dirTitleBecauseNoPredictions = dirTitleBecauseNoPredictions == null ? "" : dirTitleBecauseNoPredictions;

                directions = directions == null ? List.of() : List.copyOf(directions);

                if (message == null) {
                        message = new Message("");
                }
        }

}
