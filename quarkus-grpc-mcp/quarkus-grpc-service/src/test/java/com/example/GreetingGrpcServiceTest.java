package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GreetingGrpcServiceTest {
    @GrpcClient
    GreetingService greetingService;

    @Test
    void testHello() {
        HelloReply reply = greetingService
                .sayHello(HelloRequest.newBuilder().setName("Neo").build()).await().atMost(Duration.ofSeconds(5));
        assertEquals("Hello Neo", reply.getMessage());
    }

}
