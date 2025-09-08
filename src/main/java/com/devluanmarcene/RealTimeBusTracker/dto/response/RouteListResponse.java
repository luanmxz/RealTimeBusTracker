package com.devluanmarcene.RealTimeBusTracker.dto.response;

import java.util.List;

import com.devluanmarcene.RealTimeBusTracker.model.Route;

public record RouteListResponse(
        List<Route> routes,
        String agencyTag) {

    public static class Builder {
        private List<Route> routes;
        private String agencyTag;

        public Builder routes(List<Route> routes) {
            this.routes = routes;
            return this;
        }

        public Builder agencyTag(String agencyTag) {
            this.agencyTag = agencyTag;
            return this;
        }

        public static Builder from(RouteListResponse original) {
            return new Builder()
                    .agencyTag(original.agencyTag())
                    .routes(original.routes());
        }

        public RouteListResponse build() {
            return new RouteListResponse(routes, agencyTag);
        }
    }

}
