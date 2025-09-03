package com.devluanmarcene.RealTimeBusTracker.model;

/**
 * Representa um par de coordenadas (latitude, longitude) em graus.
 *
 * @param lat latitude em graus (-90 .. 90)
 * @param lon longitude em graus (-180 .. 180)
 */
public record LatLng(double lat, double lon) {
    public LatLng {
        if (Double.isNaN(lat) || lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("Latitude inválida: " + lat);
        }
        if (Double.isNaN(lon) || lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException("Longitude inválida: " + lon);
        }
    }
}
