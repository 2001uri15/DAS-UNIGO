package com.example.unigo.model;

public class RutaBusTransb {
    private String ruta1;
    private String viaje1;
    private String origen;
    private String transbordo;
    private String salidaOrigen;
    private String ruta2;
    private String viaje2;
    private String transbordoConfirmado;
    private String destino;
    private String salidaTransbordo;

    public RutaBusTransb(String ruta1, String viaje1, String origen, String transbordo, String salidaOrigen,
                         String ruta2, String viaje2, String transbordoConfirmado, String destino, String salidaTransbordo) {
        this.ruta1 = ruta1;
        this.viaje1 = viaje1;
        this.origen = origen;
        this.transbordo = transbordo;
        this.salidaOrigen = salidaOrigen;
        this.ruta2 = ruta2;
        this.viaje2 = viaje2;
        this.transbordoConfirmado = transbordoConfirmado;
        this.destino = destino;
        this.salidaTransbordo = salidaTransbordo;
    }

    // Getters
    public String getRuta1() { return ruta1; }
    public String getViaje1() { return viaje1; }
    public String getOrigen() { return origen; }
    public String getTransbordo() { return transbordo; }
    public String getSalidaOrigen() { return salidaOrigen; }
    public String getRuta2() { return ruta2; }
    public String getViaje2() { return viaje2; }
    public String getTransbordoConfirmado() { return transbordoConfirmado; }
    public String getDestino() { return destino; }
    public String getSalidaTransbordo() { return salidaTransbordo; }
}