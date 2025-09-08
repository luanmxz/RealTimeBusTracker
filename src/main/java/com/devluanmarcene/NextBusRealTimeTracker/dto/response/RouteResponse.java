package com.devluanmarcene.NextBusRealTimeTracker.dto.response;

import java.util.List;

import com.devluanmarcene.NextBusRealTimeTracker.dto.internal.StopDTO;
import com.devluanmarcene.NextBusRealTimeTracker.model.Direction;
import com.devluanmarcene.NextBusRealTimeTracker.model.Path;
import com.devluanmarcene.NextBusRealTimeTracker.model.Vehicle;

public record RouteResponse(
        String tag,
        String title,
        String color,
        String agencyTag,
        double latMin,
        double latMax,
        double lonMin,
        double lonMax,
        List<StopDTO> stops,
        Direction direction,
        List<Path> paths,
        List<Vehicle> vehicles) {

    public static class Builder {
        private String tag;
        private String title;
        private String color;
        private String agencyTag;
        private double latMin;
        private double latMax;
        private double lonMin;
        private double lonMax;
        private List<StopDTO> stops;
        private Direction direction;
        private List<Path> paths;
        private List<Vehicle> vehicles;

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Builder agencyTag(String agencyTag) {
            this.agencyTag = agencyTag;
            return this;
        }

        public Builder latMin(double latMin) {
            this.latMin = latMin;
            return this;
        }

        public Builder latMax(double latMax) {
            this.latMax = latMax;
            return this;
        }

        public Builder lonMin(double lonMin) {
            this.lonMin = lonMin;
            return this;
        }

        public Builder lonMax(double lonMax) {
            this.lonMax = lonMax;
            return this;
        }

        public Builder stops(List<StopDTO> stops) {
            this.stops = stops;
            return this;
        }

        public Builder direction(Direction direction) {
            this.direction = direction;
            return this;
        }

        public Builder paths(List<Path> paths) {
            this.paths = paths;
            return this;
        }

        public Builder vehicles(List<Vehicle> vehicles) {
            this.vehicles = vehicles;
            return this;
        }

        public static Builder from(RouteResponse original) {
            return new Builder()
                    .tag(original.tag())
                    .title(original.title())
                    .color(original.color())
                    .agencyTag(original.agencyTag())
                    .latMin(original.latMin())
                    .latMax(original.latMax())
                    .lonMin(original.lonMin())
                    .lonMax(original.lonMax())
                    .stops(original.stops())
                    .direction(original.direction())
                    .paths(original.paths())
                    .vehicles(original.vehicles());
        }

        public RouteResponse build() {
            return new RouteResponse(tag, title, color, agencyTag, latMin, latMax, lonMin, lonMax, stops, direction,
                    paths, vehicles);
        }

    }
}
