package com.devluanmarcene.NextBusRealTimeTracker.helpers;

import java.util.Objects;

import com.devluanmarcene.NextBusRealTimeTracker.model.LatLng;

/**
 * Utilitários para cálculo de distância/bearing usando a fórmula de Haversine
 * (aproximação esférica).
 *
 * <p>
 * Escolha Haversine quando precisar de uma implementação simples, eficiente e
 * com precisão aceitável para pequenas/medianas distâncias (metros a algumas
 * centenas de km).
 * Para precisão em longas distâncias (ou requisitos críticos), use um método
 * elipsoidal
 * (GeographicLib, GeoTools, Apache SIS).
 * </p>
 *
 * <p>
 * Complexidade: O(1) por chamada. Não utiliza estado (thread-safe).
 * </p>
 */
public final class HaversineUtils {

    /** Raio médio da Terra em metros utilizado por muitos serviços (aprox.). */
    public static final double EARTH_RADIUS_METERS = 6_371_000.0d;

    private HaversineUtils() {
        /* utilitário */ }

    /**
     * Calcula a distância entre dois pontos usando a fórmula de Haversine.
     *
     * @param a ponto A (lat/lon em graus)
     * @param b ponto B (lat/lon em graus)
     * @return distância em metros
     * @throws NullPointerException se {@code a} ou {@code b} for nulo
     */
    public static double distanceMeters(LatLng a, LatLng b) {
        Objects.requireNonNull(a, "a não pode ser nulo");
        Objects.requireNonNull(b, "b não pode ser nulo");

        // Converter para radianos — faça isso uma vez por chamada para precisão
        double phi1 = Math.toRadians(a.lat());
        double phi2 = Math.toRadians(b.lat());
        double dPhi = Math.toRadians(b.lat() - a.lat());
        double dLambda = Math.toRadians(b.lon() - a.lon());

        // Haversine
        double sinDphi2 = Math.sin(dPhi / 2.0);
        double sinDlam2 = Math.sin(dLambda / 2.0);

        double aHarv = sinDphi2 * sinDphi2
                + Math.cos(phi1) * Math.cos(phi2) * sinDlam2 * sinDlam2;

        // Proteção numérica: aHarv deve estar em [0,1]
        aHarv = Math.min(1.0, Math.max(0.0, aHarv));

        double c = 2.0 * Math.atan2(Math.sqrt(aHarv), Math.sqrt(1.0 - aHarv));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Calcula a distância em quilômetros.
     *
     * @param a ponto A
     * @param b ponto B
     * @return distância em quilômetros
     */
    public static double distanceKm(LatLng a, LatLng b) {
        return distanceMeters(a, b) / 1000.0d;
    }

    /**
     * Calcula a distância em milhas.
     *
     * @param a ponto A
     * @param b ponto B
     * @return distância em milhas
     */
    public static double distanceMiles(LatLng a, LatLng b) {
        return distanceMeters(a, b) * 0.000621371192237334; // 1 meter -> miles
    }

    /**
     * Calcula o rumo (bearing) inicial de A para B em graus relativos ao Norte
     * verdadeiro.
     * O valor está no intervalo [0, 360).
     *
     * @param a ponto de partida
     * @param b ponto de destino
     * @return bearing inicial em graus
     */
    public static double initialBearingDegrees(LatLng a, LatLng b) {
        Objects.requireNonNull(a, "a não pode ser nulo");
        Objects.requireNonNull(b, "b não pode ser nulo");

        double phi1 = Math.toRadians(a.lat());
        double phi2 = Math.toRadians(b.lat());
        double lambda1 = Math.toRadians(a.lon());
        double lambda2 = Math.toRadians(b.lon());

        double dLambda = lambda2 - lambda1;

        double y = Math.sin(dLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLambda);

        double theta = Math.toDegrees(Math.atan2(y, x));
        return (theta + 360.0) % 360.0;
    }
}
