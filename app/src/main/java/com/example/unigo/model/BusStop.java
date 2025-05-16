package com.example.unigo.model;

public class BusStop {
    private String stop_id;
    private String stop_name;
    private double stop_lat;
    private double stop_lon;

    public BusStop(String stop_id, String stop_name, double stop_lat, double stop_lon) {
        this.stop_id = stop_id;
        this.stop_name = stop_name;
        this.stop_lat = stop_lat;
        this.stop_lon = stop_lon;
    }

    // Getters y Setters
    public String getStop_id() { return stop_id; }
    public String getStop_name() { return stop_name; }
    public double getStop_lat() { return stop_lat; }
    public double getStop_lon() { return stop_lon; }
}