package com.cinehub.models;

public class Movie {
    private String id;
    private String title;
    private int duration;

    public Movie(String id, String title, int duration) {
        this.id = id;
        this.title = title;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }
}
