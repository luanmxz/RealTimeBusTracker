package com.devluanmarcene.NextBusRealTimeTracker.dto.internal;

import com.devluanmarcene.NextBusRealTimeTracker.model.Predictions;

public record StopDTO(
        String tag, String title, double lat, double lon, long stopId, double distanceFromUserDestination,
        Predictions predictions) {

    public static class Builder {
        private String tag;
        private String title;
        private double lat;
        private double lon;
        private long stopId;
        private double distanceFromUserDestination;
        private Predictions predictions;

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder lat(double lat) {
            this.lat = lat;
            return this;
        }

        public Builder lon(double lon) {
            this.lon = lon;
            return this;
        }

        public Builder stopId(long stopId) {
            this.stopId = stopId;
            return this;
        }

        public Builder distanceFromUserDestination(double distanceFromUserDestination) {
            this.distanceFromUserDestination = distanceFromUserDestination;
            return this;
        }

        public Builder predictions(Predictions predictions) {
            this.predictions = predictions;
            return this;
        }

        public static Builder from(StopDTO original) {
            return new Builder()
                    .tag(original.tag())
                    .title(original.title())
                    .stopId(original.stopId())
                    .lat(original.lat())
                    .lon(original.lon())
                    .distanceFromUserDestination(original.distanceFromUserDestination())
                    .predictions(original.predictions());
        }

        public StopDTO build() {
            return new StopDTO(tag, title, lat, lon, stopId, distanceFromUserDestination, predictions);
        }
    }
}
