package com.example.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.example.model.TickerData;
import com.example.model.TickerResponse;
import com.example.model.OHLCVData;

import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.timeseries.Aggregation;
import io.quarkus.redis.datasource.timeseries.CreateArgs;
import io.quarkus.redis.datasource.timeseries.ReactiveTimeSeriesCommands;
import io.quarkus.redis.datasource.timeseries.RangeArgs;
import io.quarkus.redis.datasource.timeseries.Sample;
import io.quarkus.redis.datasource.timeseries.SeriesSample;
import io.quarkus.redis.datasource.timeseries.TimeSeriesRange;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class TimeSeriesService {

    @Inject
    ReactiveRedisDataSource redisDataSource;

    @ConfigProperty(name = "binance.symbols")
    List<String> symbols;

    private ReactiveTimeSeriesCommands<String> tsCommands;

    private final Set<String> initializedSeries = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, Uni<Void>> initTasks = new ConcurrentHashMap<>();
    private final Object initQueueLock = new Object();
    private Uni<Void> initQueue = Uni.createFrom().voidItem();

    void onStart(@Observes StartupEvent ev) {
        tsCommands = redisDataSource.timeseries(String.class);
        Log.info("TimeSeriesService initialized");

        // Pre-create base + aggregated series and compaction rules for configured
        // symbols.
        if (symbols != null && !symbols.isEmpty()) {
            initializeAllSymbols()
                    .subscribe().with(
                            ignored -> Log.info("Time series initialization complete"),
                            t -> Log.warn("Time series initialization encountered errors", t));
        }
    }

    public Uni<Void> addTick(TickerData ticker) {
        String symbol = ticker.getSymbol().toLowerCase();
        long timestamp = ticker.getEventTime();

        // Ensure compaction rules exist (startup pre-creates, but we also lazily init
        // new symbols).
        String priceKey = priceKey(symbol);
        String volumeKey = volumeKey(symbol);
        String tradesKey = tradesKey(symbol);

        // Use a single TS.MADD call to reduce pressure on the Redis connection pool.
        @SuppressWarnings("unchecked")
        SeriesSample<String>[] samples = (SeriesSample<String>[]) new SeriesSample[] {
                SeriesSample.from(priceKey, timestamp, ticker.getLastPrice().doubleValue()),
                SeriesSample.from(volumeKey, timestamp, ticker.getVolume().doubleValue()),
                SeriesSample.from(tradesKey, timestamp, ticker.getNumberOfTrades().doubleValue())
        };

        return ensureInitialized(symbol)
                .chain(() -> tsCommands.tsMAdd(samples))
                .onFailure().recoverWithUni(t -> {
                    String msg = t.getMessage();
                    if (msg != null && msg.contains("TSDB: the key does not exist")) {
                        // Safety net: if keys weren't created for some reason, create and retry.
                        return chainAll(List.of(
                                createTimeSeriesIfNotExists(priceKey,
                                        Map.of("symbol", symbol, "type", "price", "aggregation", "raw"),
                                        Duration.ofHours(24)),
                                createTimeSeriesIfNotExists(volumeKey,
                                        Map.of("symbol", symbol, "type", "volume", "aggregation", "raw"),
                                        Duration.ofHours(24)),
                                createTimeSeriesIfNotExists(tradesKey,
                                        Map.of("symbol", symbol, "type", "trades", "aggregation", "raw"),
                                        Duration.ofHours(24))))
                                .chain(() -> tsCommands.tsMAdd(samples));
                    }
                    return Uni.createFrom().failure(t);
                })
                .onFailure().invoke(t -> Log.errorf("Error adding tick data for %s: %s", symbol, t.getMessage()));
    }

    private Uni<Void> initializeAllSymbols() {
        // IMPORTANT: initialize sequentially to avoid exhausting the Redis connection
        // pool.
        return chainAll(symbols.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(this::ensureInitialized)
                .toList());
    }

    private Uni<Void> ensureInitialized(String symbol) {
        String normalized = symbol.toLowerCase();
        if (initializedSeries.contains(normalized)) {
            return Uni.createFrom().voidItem();
        }

        // Ensure concurrent callers share the same initialization work and wait for it.
        return initTasks.computeIfAbsent(normalized, s -> {
            // Serialize initialization across symbols to avoid exhausting the Redis pool.
            synchronized (initQueueLock) {
                initQueue = initQueue
                        .chain(() -> initializeTimeSeriesForSymbol(s))
                        .memoize().indefinitely();
                return initQueue
                        .onItem().invoke(() -> initializedSeries.add(s))
                        .onTermination().invoke(() -> initTasks.remove(s))
                        .memoize().indefinitely();
            }
        });
    }

    private Uni<Void> initializeTimeSeriesForSymbol(String symbol) {
        String normalizedSymbol = symbol.toLowerCase();

        if (initializedSeries.contains(normalizedSymbol)) {
            return Uni.createFrom().voidItem();
        }

        Log.infof("Initializing time series for symbol: %s", normalizedSymbol);

        return createPriceTimeSeries(normalizedSymbol)
                .chain(() -> createVolumeTimeSeries(normalizedSymbol))
                .chain(() -> createTradesTimeSeries(normalizedSymbol))
                .invoke(() -> {
                    Log.infof("Successfully initialized time series for: %s", normalizedSymbol);
                })
                .onFailure()
                .invoke(t -> Log.errorf("Error initializing time series for %s: %s", normalizedSymbol, t.getMessage()));
    }

    private Uni<Void> createPriceTimeSeries(String symbol) {
        String baseKey = priceKey(symbol);

        return createTimeSeriesIfNotExists(
                baseKey,
                Map.of("symbol", symbol, "type", "price", "aggregation", "raw"),
                Duration.ofHours(24))
                .chain(() -> chainAll(List.of(
                        // 1-minute aggregates (7 days retention) with OHLC
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofMinutes(1), Duration.ofDays(7),
                                Aggregation.FIRST, "open"),
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofMinutes(1), Duration.ofDays(7),
                                Aggregation.MAX, "high"),
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofMinutes(1), Duration.ofDays(7),
                                Aggregation.MIN, "low"),
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofMinutes(1), Duration.ofDays(7),
                                Aggregation.LAST, "close"),

                        // 5-minute aggregates (30 days retention) with OHLC
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofMinutes(5), Duration.ofDays(30),
                                Aggregation.FIRST, "open"),
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofMinutes(5), Duration.ofDays(30),
                                Aggregation.MAX, "high"),
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofMinutes(5), Duration.ofDays(30),
                                Aggregation.MIN, "low"),
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofMinutes(5), Duration.ofDays(30),
                                Aggregation.LAST, "close"),

                        // 1-hour aggregates (1 year retention) with OHLC
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofHours(1), Duration.ofDays(365),
                                Aggregation.FIRST, "open"),
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofHours(1), Duration.ofDays(365),
                                Aggregation.MAX, "high"),
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofHours(1), Duration.ofDays(365),
                                Aggregation.MIN, "low"),
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofHours(1), Duration.ofDays(365),
                                Aggregation.LAST, "close"),

                        // 1-day aggregates (10 years retention) with OHLC
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofDays(1), Duration.ofDays(3650),
                                Aggregation.FIRST, "open"),
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofDays(1), Duration.ofDays(3650),
                                Aggregation.MAX, "high"),
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofDays(1), Duration.ofDays(3650),
                                Aggregation.MIN, "low"),
                        createAggregateTimeSeries(baseKey, symbol, "price", Duration.ofDays(1), Duration.ofDays(3650),
                                Aggregation.LAST, "close"))));
    }

    private Uni<Void> createVolumeTimeSeries(String symbol) {
        String baseKey = volumeKey(symbol);

        return createTimeSeriesIfNotExists(
                baseKey,
                Map.of("symbol", symbol, "type", "volume", "aggregation", "raw"),
                Duration.ofHours(24))
                .chain(() -> chainAll(List.of(
                        // Volume uses SUM aggregation
                        createAggregateTimeSeries(baseKey, symbol, "volume", Duration.ofMinutes(1), Duration.ofDays(7),
                                Aggregation.SUM, "sum"),
                        createAggregateTimeSeries(baseKey, symbol, "volume", Duration.ofMinutes(5), Duration.ofDays(30),
                                Aggregation.SUM, "sum"),
                        createAggregateTimeSeries(baseKey, symbol, "volume", Duration.ofHours(1), Duration.ofDays(365),
                                Aggregation.SUM, "sum"),
                        createAggregateTimeSeries(baseKey, symbol, "volume", Duration.ofDays(1), Duration.ofDays(3650),
                                Aggregation.SUM, "sum"))));
    }

    private Uni<Void> createTradesTimeSeries(String symbol) {
        String baseKey = tradesKey(symbol);

        return createTimeSeriesIfNotExists(
                baseKey,
                Map.of("symbol", symbol, "type", "trades", "aggregation", "raw"),
                Duration.ofHours(24))
                .chain(() -> chainAll(List.of(
                        // Trades uses SUM aggregation
                        createAggregateTimeSeries(baseKey, symbol, "trades", Duration.ofMinutes(1), Duration.ofDays(7),
                                Aggregation.SUM, "sum"),
                        createAggregateTimeSeries(baseKey, symbol, "trades", Duration.ofMinutes(5), Duration.ofDays(30),
                                Aggregation.SUM, "sum"),
                        createAggregateTimeSeries(baseKey, symbol, "trades", Duration.ofHours(1), Duration.ofDays(365),
                                Aggregation.SUM, "sum"),
                        createAggregateTimeSeries(baseKey, symbol, "trades", Duration.ofDays(1), Duration.ofDays(3650),
                                Aggregation.SUM, "sum"))));
    }

    private Uni<Void> createAggregateTimeSeries(
            String sourceKey,
            String symbol,
            String type,
            Duration bucketDuration,
            Duration retention,
            Aggregation aggregation,
            String aggregationLabel) {
        String destKey = aggregateKey(symbol, type, bucketDuration, aggregationLabel);

        // Create destination time-series, then create compaction rule.
        return createTimeSeriesIfNotExists(
                destKey,
                Map.of(
                        "symbol", symbol,
                        "type", type,
                        "aggregation", aggregationLabel,
                        "bucket", formatDuration(bucketDuration)),
                retention)
                .chain(() -> tsCommands.tsCreateRule(sourceKey, destKey, aggregation, bucketDuration)
                        .invoke(() -> Log.debugf("Created compaction rule: %s -> %s (%s, %s)",
                                sourceKey, destKey, aggregation, bucketDuration))
                        .onFailure().recoverWithUni(t -> {
                            String msg = t.getMessage();
                            // Rule might already exist, that's fine.
                            if (msg != null && msg.toLowerCase().contains("compaction rule")
                                    && msg.toLowerCase().contains("already")) {
                                return Uni.createFrom().voidItem();
                            }
                            return Uni.createFrom().failure(t);
                        }));
    }

    private Uni<Void> createTimeSeriesIfNotExists(String key, Map<String, String> labels, Duration retention) {
        CreateArgs args = new CreateArgs().setRetention(retention);
        labels.forEach(args::label);

        return tsCommands.tsCreate(key, args)
                .invoke(() -> Log.debugf("Created time series: %s (retention: %s)", key, retention))
                // If another concurrent initializer already created it, ignore and continue.
                .onFailure().recoverWithUni(t -> {
                    String msg = t.getMessage();
                    if (msg != null && msg.contains("TSDB: key already exists")) {
                        return Uni.createFrom().voidItem();
                    }
                    return Uni.createFrom().failure(t);
                });
    }

    private Uni<Void> chainAll(List<Uni<Void>> tasks) {
        Uni<Void> uni = Uni.createFrom().voidItem();
        for (Uni<Void> task : tasks) {
            uni = uni.chain(() -> task);
        }
        return uni;
    }

    /**
     * Blocking convenience wrapper. Prefer {@link #getLatestTickerAsync(String)} in
     * reactive routes.
     */
    public TickerResponse getLatestTicker(String symbol) {
        return getLatestTickerAsync(symbol)
                .await().atMost(Duration.ofSeconds(2));
    }

    public Uni<TickerResponse> getLatestTickerAsync(String symbol) {
        String normalized = normalizeSymbol(symbol);

        long now = Instant.now().toEpochMilli();
        long start = now - Duration.ofHours(24).toMillis();

        String priceKey = priceKey(normalized);
        String volumeKey = volumeKey(normalized);

        Uni<Sample> latestPrice = tsCommands.tsGet(priceKey);
        Uni<Sample> latestVolume = tsCommands.tsGet(volumeKey);
        Uni<List<Sample>> firstPriceInWindow = tsCommands.tsRange(
                priceKey,
                TimeSeriesRange.fromTimestamps(start, now),
                new RangeArgs().count(1));

        return ensureInitialized(normalized)
                .chain(() -> Uni.combine().all().unis(latestPrice, latestVolume, firstPriceInWindow).asTuple())
                .map(tuple -> {
                    Sample lastPrice = tuple.getItem1();
                    Sample lastVol = tuple.getItem2();
                    List<Sample> firstPrices = tuple.getItem3();

                    if (lastPrice == null) {
                        return new TickerResponse(normalized, 0, 0, 0, lastVol != null ? lastVol.value() : 0, now);
                    }

                    double price = lastPrice.value();
                    double first = !firstPrices.isEmpty() ? firstPrices.get(0).value() : price;
                    double change = price - first;
                    double pct = first != 0 ? (change / first) * 100.0 : 0.0;

                    return new TickerResponse(
                            normalized,
                            price,
                            change,
                            pct,
                            lastVol != null ? lastVol.value() : 0,
                            lastPrice.timestamp());
                });
    }

    /**
     * Blocking convenience wrapper. Prefer {@link #getHistoricalDataAsync(String, String)}
     * in reactive routes.
     */
    public OHLCVData getHistoricalData(String symbol, String range) {
        return getHistoricalDataAsync(symbol, range)
                .await().atMost(Duration.ofSeconds(5));
    }

    public Uni<OHLCVData> getHistoricalDataAsync(String symbol, String range) {
        String normalized = normalizeSymbol(symbol);

        Duration window = parseWindow(range);
        Duration bucket = chooseBucket(window);

        long now = Instant.now().toEpochMilli();
        long start = now - window.toMillis();

        return ensureInitialized(normalized)
                .chain(() -> fetchCandles(normalized, bucket, start, now)
                        .chain(resp -> {
                            if (resp.getCandles() != null && !resp.getCandles().isEmpty()) {
                                return Uni.createFrom().item(resp);
                            }

                            // RedisTimeSeries compactions only emit once a bucket completes.
                            // In dev mode, that can mean 24h/7d/30d views look empty for a long time.
                            // Fallback: fetch a smaller bucket and aggregate in-app.
                            Duration sourceBucket = fallbackSourceBucket(bucket);
                            if (sourceBucket == null) {
                                // For 1m (and any other non-fallback bucket), use raw as a last resort.
                                return fetchRawCandles(normalized, start, now)
                                        .map(raw -> aggregateResponse(normalized, bucket, raw.getCandles()));
                            }

                            return fetchCandles(normalized, sourceBucket, start, now)
                                    .chain(sourceResp -> {
                                        if (sourceResp.getCandles() != null && !sourceResp.getCandles().isEmpty()) {
                                            return Uni.createFrom().item(aggregateResponse(normalized, bucket,
                                                    sourceResp.getCandles()));
                                        }

                                        // If the preferred source bucket is also empty (common right after startup),
                                        // fall back one more level to 1m and aggregate.
                                        if (!sourceBucket.equals(Duration.ofMinutes(1))) {
                                            return fetchCandles(normalized, Duration.ofMinutes(1), start, now)
                                                    .chain(oneMin -> {
                                                        if (oneMin.getCandles() != null && !oneMin.getCandles().isEmpty()) {
                                                            return Uni.createFrom().item(aggregateResponse(normalized, bucket,
                                                                    oneMin.getCandles()));
                                                        }
                                                        return fetchRawCandles(normalized, start, now)
                                                                .map(raw -> aggregateResponse(normalized, bucket, raw.getCandles()));
                                                    });
                                        }

                                        // Last resort: aggregate raw samples so the UI isn't empty right after startup.
                                        return fetchRawCandles(normalized, start, now)
                                                .map(raw -> aggregateResponse(normalized, bucket, raw.getCandles()));
                                    });
                        }));
    }

    private Duration fallbackSourceBucket(Duration targetBucket) {
        if (targetBucket.equals(Duration.ofHours(1))) {
            return Duration.ofMinutes(1);
        }
        if (targetBucket.equals(Duration.ofDays(1))) {
            return Duration.ofHours(1);
        }
        return null;
    }

    private Uni<OHLCVData> fetchCandles(String symbol, Duration bucket, long start, long now) {
        String openKey = aggregateKey(symbol, "price", bucket, "open");
        String highKey = aggregateKey(symbol, "price", bucket, "high");
        String lowKey = aggregateKey(symbol, "price", bucket, "low");
        String closeKey = aggregateKey(symbol, "price", bucket, "close");
        String volKey = aggregateKey(symbol, "volume", bucket, "sum");

        TimeSeriesRange tsRange = TimeSeriesRange.fromTimestamps(start, now);

        Uni<List<Sample>> opens = tsCommands.tsRange(openKey, tsRange);
        Uni<List<Sample>> highs = tsCommands.tsRange(highKey, tsRange);
        Uni<List<Sample>> lows = tsCommands.tsRange(lowKey, tsRange);
        Uni<List<Sample>> closes = tsCommands.tsRange(closeKey, tsRange);
        Uni<List<Sample>> vols = tsCommands.tsRange(volKey, tsRange);

        return Uni.combine().all().unis(opens, highs, lows, closes, vols).asTuple()
                .map(tuple -> {
                    Map<Long, Double> openMap = toMap(tuple.getItem1());
                    Map<Long, Double> highMap = toMap(tuple.getItem2());
                    Map<Long, Double> lowMap = toMap(tuple.getItem3());
                    List<Sample> closeSamples = tuple.getItem4();
                    Map<Long, Double> volMap = toMap(tuple.getItem5());

                    List<OHLCVData.Candle> candles = new ArrayList<>(closeSamples.size());
                    for (Sample close : closeSamples) {
                        long ts = close.timestamp();
                        double c = close.value();
                        double o = openMap.getOrDefault(ts, c);
                        double h = highMap.getOrDefault(ts, c);
                        double l = lowMap.getOrDefault(ts, c);
                        double v = volMap.getOrDefault(ts, 0.0);

                        candles.add(new OHLCVData.Candle(ts, o, h, l, c, v));
                    }

                    OHLCVData resp = new OHLCVData();
                    resp.setSymbol(symbol);
                    resp.setInterval(formatDuration(bucket));
                    resp.setCandles(candles);
                    return resp;
                })
                .onFailure().recoverWithItem(t -> {
                    // If aggregated series don't exist yet (early in dev mode), return empty response.
                    OHLCVData resp = new OHLCVData();
                    resp.setSymbol(symbol);
                    resp.setInterval(formatDuration(bucket));
                    resp.setCandles(List.of());
                    return resp;
                });
    }

    private Uni<OHLCVData> fetchRawCandles(String symbol, long start, long now) {
        String priceKey = priceKey(symbol);
        TimeSeriesRange tsRange = TimeSeriesRange.fromTimestamps(start, now);

        return tsCommands.tsRange(priceKey, tsRange)
                .map(samples -> {
                    List<OHLCVData.Candle> candles = new ArrayList<>(samples != null ? samples.size() : 0);
                    if (samples != null) {
                        for (Sample s : samples) {
                            double v = s.value();
                            candles.add(new OHLCVData.Candle(s.timestamp(), v, v, v, v, 0.0));
                        }
                    }
                    OHLCVData resp = new OHLCVData();
                    resp.setSymbol(symbol);
                    resp.setInterval("raw");
                    resp.setCandles(candles);
                    return resp;
                })
                .onFailure().recoverWithItem(t -> {
                    OHLCVData resp = new OHLCVData();
                    resp.setSymbol(symbol);
                    resp.setInterval("raw");
                    resp.setCandles(List.of());
                    return resp;
                });
    }

    private OHLCVData aggregateResponse(String symbol, Duration targetBucket, List<OHLCVData.Candle> sourceCandles) {
        List<OHLCVData.Candle> aggregated = aggregateCandles(sourceCandles, targetBucket);
        OHLCVData resp = new OHLCVData();
        resp.setSymbol(symbol);
        resp.setInterval(formatDuration(targetBucket));
        resp.setCandles(aggregated);
        return resp;
    }

    private List<OHLCVData.Candle> aggregateCandles(List<OHLCVData.Candle> source, Duration targetBucket) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        long bucketMs = targetBucket.toMillis();
        if (bucketMs <= 0) {
            return source;
        }

        // Ensure input is ordered by timestamp.
        List<OHLCVData.Candle> sorted = new ArrayList<>(source);
        sorted.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

        Map<Long, OHLCVData.Candle> out = new HashMap<>();
        List<Long> order = new ArrayList<>();

        for (OHLCVData.Candle c : sorted) {
            long bucketStart = (c.getTimestamp() / bucketMs) * bucketMs;
            OHLCVData.Candle agg = out.get(bucketStart);
            if (agg == null) {
                out.put(bucketStart, new OHLCVData.Candle(
                        bucketStart,
                        c.getOpen(),
                        c.getHigh(),
                        c.getLow(),
                        c.getClose(),
                        c.getVolume()));
                order.add(bucketStart);
            } else {
                agg.setHigh(Math.max(agg.getHigh(), c.getHigh()));
                agg.setLow(Math.min(agg.getLow(), c.getLow()));
                agg.setClose(c.getClose());
                agg.setVolume(agg.getVolume() + c.getVolume());
            }
        }

        order.sort(Long::compare);
        List<OHLCVData.Candle> aggregated = new ArrayList<>(order.size());
        for (Long ts : order) {
            aggregated.add(out.get(ts));
        }
        return aggregated;
    }

    private Map<Long, Double> toMap(List<Sample> samples) {
        Map<Long, Double> map = new HashMap<>();
        if (samples == null) {
            return map;
        }
        for (Sample s : samples) {
            map.put(s.timestamp(), s.value());
        }
        return map;
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return "";
        }
        return symbol.trim().toLowerCase();
    }

    private Duration parseWindow(String range) {
        if (range == null || range.isBlank()) {
            return Duration.ofHours(24);
        }

        String r = range.trim().toLowerCase();
        switch (r) {
            case "1h":
                return Duration.ofHours(1);
            case "6h":
                return Duration.ofHours(6);
            case "12h":
                return Duration.ofHours(12);
            case "24h":
            case "1d":
                return Duration.ofHours(24);
            case "7d":
            case "1w":
                return Duration.ofDays(7);
            case "30d":
                return Duration.ofDays(30);
            case "365d":
            case "1y":
                return Duration.ofDays(365);
            default:
                break;
        }

        // Basic duration parser: <number><unit>, units: s,m,h,d,w,y
        long n;
        String numPart = r.replaceAll("[^0-9]", "");
        if (numPart.isEmpty()) {
            return Duration.ofHours(24);
        }
        try {
            n = Long.parseLong(numPart);
        } catch (NumberFormatException e) {
            return Duration.ofHours(24);
        }

        if (r.endsWith("ms")) {
            return Duration.ofMillis(n);
        } else if (r.endsWith("s")) {
            return Duration.ofSeconds(n);
        } else if (r.endsWith("m")) {
            return Duration.ofMinutes(n);
        } else if (r.endsWith("h")) {
            return Duration.ofHours(n);
        } else if (r.endsWith("d")) {
            return Duration.ofDays(n);
        } else if (r.endsWith("w")) {
            return Duration.ofDays(n * 7);
        } else if (r.endsWith("y")) {
            return Duration.ofDays(n * 365);
        }

        return Duration.ofHours(24);
    }

    private Duration chooseBucket(Duration window) {
        // Buckets for UI:
        // - 1h view: 1-minute points (line chart)
        // - 24h view: 1-hour candles
        // - 7d/30d views: 1-day candles
        if (window.compareTo(Duration.ofHours(1)) <= 0) {
            return Duration.ofMinutes(1);
        }
        if (window.compareTo(Duration.ofHours(24)) <= 0) {
            return Duration.ofHours(1);
        }
        return Duration.ofDays(1);
    }

    // Key generation methods
    private String priceKey(String symbol) {
        return "ts:price:" + symbol;
    }

    private String volumeKey(String symbol) {
        return "ts:volume:" + symbol;
    }

    private String tradesKey(String symbol) {
        return "ts:trades:" + symbol;
    }

    private String aggregateKey(String symbol, String type, Duration bucket, String aggregation) {
        return String.format("ts:%s:%s:%s:%s", type, symbol, formatDuration(bucket), aggregation);
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds >= 86_400 && seconds % 86_400 == 0) {
            return (seconds / 86_400) + "d";
        }
        if (seconds >= 3_600 && seconds % 3_600 == 0) {
            return (seconds / 3_600) + "h";
        }
        if (seconds >= 60 && seconds % 60 == 0) {
            return (seconds / 60) + "m";
        }
        return seconds + "s";
    }
}