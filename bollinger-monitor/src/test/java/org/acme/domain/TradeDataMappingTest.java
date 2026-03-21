package org.acme.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Binance trade stream uses short field names; price is often a JSON string.
 */
class TradeDataMappingTest {

    @Test
    void parsesBinanceCompactTradePayload() throws Exception {
        String json = """
                {"e":"trade","E":1690000000000,"s":"BTCUSDT","t":12345,"p":"65234.50","q":"0.001","T":1690000000001,"m":false}
                """;

        TradeData t = new ObjectMapper().readValue(json, TradeData.class);

        assertEquals(65234.50, t.price(), 1e-6);
        assertEquals(1690000000001L, t.timestamp());
    }
}
