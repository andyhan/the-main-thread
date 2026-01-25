package com.example.model;

public class TickerResponse {
    private String symbol;
    private double price;
    private double priceChange;
    private double priceChangePercent;
    private double volume;
    private long timestamp;

    public TickerResponse() {
    }

    public TickerResponse(String symbol, double price, double priceChange,
            double priceChangePercent, double volume, long timestamp) {
        this.symbol = symbol;
        this.price = price;
        this.priceChange = priceChange;
        this.priceChangePercent = priceChangePercent;
        this.volume = volume;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getPriceChange() {
        return priceChange;
    }

    public void setPriceChange(double priceChange) {
        this.priceChange = priceChange;
    }

    public double getPriceChangePercent() {
        return priceChangePercent;
    }

    public void setPriceChangePercent(double priceChangePercent) {
        this.priceChangePercent = priceChangePercent;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}