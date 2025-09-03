package com.devluanmarcene.RealTimeBusTracker.model;

public record Prediction(
        long seconds,
        long minutes,
        long epochTime,
        boolean isDeparture,
        String dirTag,
        String tripTag,
        String branch,
        boolean affectedByLayover,
        boolean isScheduleBased,
        boolean delayed,
        String block) {

}
