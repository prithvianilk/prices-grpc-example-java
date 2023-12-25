package com.prithvianilk.prices;

import com.google.protobuf.Empty;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.random.RandomGenerator;

public class PriceApplication {
    private final PriceServiceGrpc.PriceServiceStub stub;

    private final PriceServiceGrpc.PriceServiceBlockingStub blockingStub;

    private final PriceServiceGrpc.PriceServiceFutureStub asyncStub;

    public PriceApplication(String host, int port) {
        Channel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        stub = PriceServiceGrpc.newStub(channel);
        blockingStub = PriceServiceGrpc.newBlockingStub(channel);
        asyncStub = PriceServiceGrpc.newFutureStub(channel);
    }

    public void getEachPricePriceIndividually() {
        getAndPrintPrice(blockingStub, "1"); // This is blocking
        getAndPrintPriceAsync(asyncStub, "2"); // This is blocking but the call itself is async. Timeout of 1s
        getAndPrintPrice(blockingStub, "3"); // This should throw an exception
    }

    private void getAndPrintPriceAsync(PriceServiceGrpc.PriceServiceFutureStub asyncStub, String id) {
        try {
            System.out.println(asyncStub.getPrice(getPriceRequest(id)).get(1, TimeUnit.SECONDS));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.println("Failed to get price: " + e.getMessage());
            System.out.println();
        }
    }

    private void getAndPrintPrice(PriceServiceGrpc.PriceServiceBlockingStub blockingStub, String id) {
        try {
            System.out.println((blockingStub.getPrice(getPriceRequest(id))));
        } catch (StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();
            switch (code) {
                case NOT_FOUND -> System.out.println(e.getStatus().getDescription());
                default -> System.out.println("Unknown error code: " + code + ", " + e.getMessage());
            }
            System.out.println();
        }
    }

    private Prices.GetPriceRequest getPriceRequest(String id) {
        return Prices.GetPriceRequest
                .newBuilder()
                .setId(id)
                .build();
    }

    public void streamAllPricesFromServer() {
        blockingStub.getPrices(Empty.getDefaultInstance()).forEachRemaining(System.out::println);
    }

    public void streamUpdatedValuesOfRandomPriceToServer() throws InterruptedException {
        StreamObserver<Prices.Price> streamObserver = stub.addPrices(getEmptyStreamObserver());

        for (int i = 0; i < 10; ++i) {
            streamObserver.onNext(getRandomPrice());
            Thread.sleep(Duration.of(1, ChronoUnit.SECONDS));
        }
    }

    private StreamObserver<Empty> getEmptyStreamObserver() {
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

    private Prices.Price getRandomPrice() {
        long value = RandomGenerator.getDefault().nextLong(1, 100);
        return Prices.Price
                .newBuilder()
                .setValue(value)
                .setId("random")
                .build();
    }
}
