package com.prithvianilk.prices;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class PriceServer {
    private final Server server;

    public PriceServer(int port) {
        this.server = ServerBuilder
                .forPort(port)
                .addService(new PriceService())
                .build();
    }

    public void start() {
        try {
            server.start().awaitTermination();
        } catch (IOException | InterruptedException e) {
            System.out.println("Received exception on server: " + e.getMessage());
        }
    }
}
