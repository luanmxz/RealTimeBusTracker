package com.devluanmarcene.RealTimeBusTracker.websocket;

public class Message {

    private String name;

    public Message() {
    }

    public Message(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
