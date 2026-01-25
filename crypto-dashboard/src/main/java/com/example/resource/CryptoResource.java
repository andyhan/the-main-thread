package com.example.resource;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestStreamElementType;

import com.example.model.OHLCVData;
import com.example.model.TickerResponse;
import com.example.service.TimeSeriesService;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CryptoResource {

    @Inject
    TimeSeriesService timeSeriesService;

    @ConfigProperty(name = "binance.symbols")
    List<String> symbols;

    @GET
    @Path("/symbols")
    public List<String> getSymbols() {
        return symbols.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/ticker/{symbol}")
    public TickerResponse getTicker(@PathParam("symbol") String symbol) {
        return timeSeriesService.getLatestTicker(symbol);
    }

    @GET
    @Path("/tickers")
    public List<TickerResponse> getAllTickers() {
        return symbols.stream()
                .map(symbol -> timeSeriesService.getLatestTicker(symbol.toLowerCase()))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/history/{symbol}")
    public OHLCVData getHistory(
            @PathParam("symbol") String symbol,
            @QueryParam("range") @DefaultValue("24h") String range) {
        return timeSeriesService.getHistoricalData(symbol, range);
    }

    @GET
    @Path("/stream/tickers")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<List<TickerResponse>> streamTickers() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                .map(tick -> symbols.stream()
                        .map(symbol -> {
                            try {
                                return timeSeriesService.getLatestTicker(symbol.toLowerCase());
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList()));
    }
}