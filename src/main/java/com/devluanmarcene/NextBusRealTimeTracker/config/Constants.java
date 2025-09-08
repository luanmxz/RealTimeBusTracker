package com.devluanmarcene.NextBusRealTimeTracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Constants {

    @Value("${API_IP_LOCATION_URL}")
    private String API_IP_LOCATION;
    @Value("${API_AGENCY_LIST_URL}")
    private String API_AGENCY_LIST;

    public String API_IP_LOCATION() {
        return API_IP_LOCATION;
    }

    public String API_AGENCY_LIST() {
        return API_AGENCY_LIST;
    }

}
