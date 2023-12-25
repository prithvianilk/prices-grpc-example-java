package com.prithvianilk.prices;

import com.google.protobuf.Empty;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.random.RandomGenerator;

public class Main {
    private static final String HOST = "localhost";

    private static final int PORT = 3000;

    private static PriceServiceGrpc.PriceServiceStub stub;

    private static PriceServiceGrpc.PriceServiceBlockingStub blockingStub;

    private static PriceServiceGrpc.PriceServiceFutureStub asyncStub;

    public static void main(String[] args) throws InterruptedException {
        startServer();
        initStubs();
        getEachPricePriceIndividually();
        streamUpdatedValuesOfRandomPriceToServer();
        streamAllPricesFromServer();
    }

    private static void startServer() {
        Thread.ofVirtual().start(() -> new PriceServer(Main.PORT).start());
    }

    private static void initStubs() {
        Channel channel = ManagedChannelBuilder
                .forAddress(HOST, PORT)
                .usePlaintext()
                .build();

        stub = PriceServiceGrpc.newStub(channel);
        blockingStub = PriceServiceGrpc.newBlockingStub(channel);
        asyncStub = PriceServiceGrpc.newFutureStub(channel);
    }

    private static void getEachPricePriceIndividually() {
        getAndPrintPrice(blockingStub, "1"); // This is blocking
        getAndPrintPriceAsync(asyncStub, "2"); // This is blocking but the call itself is async. Timeout of 1s
        getAndPrintPrice(blockingStub, "3"); // This should throw an exception
    }

    private static void getAndPrintPriceAsync(PriceServiceGrpc.PriceServiceFutureStub asyncStub, String id) {
        try {
            asyncStub.getPrice(getPriceRequest(id)).get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.println("Failed to get price: " + e.getMessage());
            System.out.println();
        }
    }

    private static void getAndPrintPrice(PriceServiceGrpc.PriceServiceBlockingStub blockingStub, String id) {
        try {
            System.out.println((blockingStub.getPrice(getPriceRequest(id))));
        } catch (StatusRuntimeException e) {
            Code code = e.getStatus().getCode();
            switch (code) {
                case NOT_FOUND -> System.out.println(e.getStatus().getDescription());
                default -> System.out.println("Unknown error code: " + code + ", " + e.getMessage());
            }
        }
    }

    private static Prices.GetPriceRequest getPriceRequest(String id) {
        return Prices.GetPriceRequest
                .newBuilder()
                .setId(id)
                .build();
    }

    private static void streamAllPricesFromServer() {
        blockingStub.getPrices(Empty.getDefaultInstance()).forEachRemaining(System.out::println);
    }

    private static void streamUpdatedValuesOfRandomPriceToServer() throws InterruptedException {
        StreamObserver<Prices.Price> streamObserver = stub.addPrices(getEmptyStreamObserver());

        for (int i = 0; i < 10; ++i) {
            streamObserver.onNext(getRandomPrice());
            Thread.sleep(Duration.of(1, ChronoUnit.SECONDS));
        }
    }

    private static StreamObserver<Empty> getEmptyStreamObserver() {
        return new StreamObserver<>() {
            @Override
            public void onNext(Empty value) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
            }
        };
    }

    private static Prices.Price getRandomPrice() {
        long value = RandomGenerator.getDefault().nextLong(1, 100);
        return Prices.Price
                .newBuilder()
                .setValue(value)
                .setId("random")
                .build();
    }
}