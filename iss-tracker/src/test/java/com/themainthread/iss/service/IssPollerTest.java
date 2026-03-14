package com.themainthread.iss.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import com.themainthread.iss.client.IssApiClient;
import com.themainthread.iss.client.IssNowResponse;
import com.themainthread.iss.client.IssPosition;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class IssPollerTest {

    @InjectMock
    @RestClient
    IssApiClient apiClient;

    @Inject
    IssPoller poller;

    @Inject
    IssPositionCache cache;

    @Test
    void pollPopulatesCacheOnSuccess() {
        IssNowResponse response = new IssNowResponse(
                "success",
                1716835200L,
                new IssPosition("51.5074", "-0.1278"));

        when(apiClient.fetchPosition()).thenReturn(response);

        poller.poll();

        assertTrue(cache.hasData());

        var fix = cache.latest();
        assertEquals(51.5074, fix.latitude());
        assertEquals(-0.1278, fix.longitude());
    }

    @Test
    void pollSwallowsNetworkErrors() {
        when(apiClient.fetchPosition()).thenThrow(new RuntimeException("timeout"));

        assertDoesNotThrow(() -> poller.poll());
    }
}