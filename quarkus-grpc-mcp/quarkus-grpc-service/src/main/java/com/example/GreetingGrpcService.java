package com.example;

import java.time.Duration;
import java.time.Instant;

import com.example.GreetingService;
import com.example.HelloReply;
import com.example.HelloRequest;
import com.example.ProfileReply;
import com.example.ProfileRequest;
import com.example.StreamRequest;

import io.quarkus.grpc.GrpcService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@GrpcService
public class GreetingGrpcService implements GreetingService {

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        Log.info("Hello request: " + request.getName());
        return Uni.createFrom().item(
                HelloReply.newBuilder()
                        .setMessage("Hello " + request.getName())
                        .setTimestamp(Instant.now().toEpochMilli())
                        .build());
    }

    @Override
    public Uni<ProfileReply> getProfile(ProfileRequest request) {
        return Uni.createFrom().item(
                ProfileReply.newBuilder()
                        .setUserId(request.getUserId())
                        .setName("John Doe")
                        .setEmail("john.doe@example.com")
                        .addRoles("user")
                        .addRoles("admin")
                        .build());
    }

    @Override
    public Multi<HelloReply> streamGreetings(StreamRequest request) {
        return Multi.createFrom().range(1, request.getCount() + 1)
                .onItem().transform(i -> HelloReply.newBuilder()
                        .setMessage("Hello " + request.getName() + " #" + i)
                        .setTimestamp(Instant.now().toEpochMilli())
                        .build())
                .onItem().call(x -> Uni.createFrom().nullItem().onItem().delayIt().by(Duration.ofSeconds(1)));
    }
}