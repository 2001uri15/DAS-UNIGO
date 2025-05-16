package com.example.unigo.model;

public class RutaBus {
    private String routeShortName;
    private String tripId;
    private String origen;
    private String destino;
    private String departureTime;

    public RutaBus(String routeShortName, String tripId, String origen, String destino, String departureTime) {
        this.routeShortName = routeShortName;
        this.tripId = tripId;
        this.origen = origen;
        this.destino = destino;
        this.departureTime = departureTime;
    }

    public String getRouteShortName() {
        return routeShortName;
    }

    public String getTripId() {
        return tripId;
    }

    public String getOrigen() {
        return origen;
    }

    public String getDestino() {
        return destino;
    }

    public String getDepartureTime() {
        return departureTime;
    }
}
