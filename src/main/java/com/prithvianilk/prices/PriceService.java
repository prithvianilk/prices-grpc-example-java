package com.prithvianilk.prices;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

public class PriceService extends PriceServiceGrpc.PriceServiceImplBase {
    private final Map<String, Long> idToPriceMap;

    public PriceService() {
        idToPriceMap = new HashMap<>();
        insertDefaultPrices();
    }

    private void insertDefaultPrices() {
        List.of("1", "2").forEach(id -> {
            idToPriceMap.put(id, getRandomPrice());
        });
    }

    @Override
    public void getPrice(Prices.GetPriceRequest request, StreamObserver<Prices.Price> responseObserver) {
        Long value = idToPriceMap.get(request.getId());

        if (Objects.isNull(value)) {
            var ex = new PriceNotFoundException();
            responseObserver.onError(ex);
            throw ex;
        }


        var price = Prices.Price
                .newBuilder()
                .setId(request.getId())
                .setValue(value)
                .build();

        responseObserver.onNext(price);
        responseObserver.onCompleted();
    }

    @Override
    public void getPrices(Empty request, StreamObserver<Prices.Price> responseObserver) {
        idToPriceMap.entrySet()
                .stream()
                .map(PriceService::getPrice)
                .forEach(value -> {
                    responseObserver.onNext(value);
                    sleep();
                });

        responseObserver.onCompleted();
    }

    private static void sleep() {
        try {
            Thread.sleep(Duration.of(3, ChronoUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new InternalServerError(e);
        }
    }

    private static Prices.Price getPrice(Map.Entry<String, Long> entry) {
        return Prices.Price
                .newBuilder()
                .setId(entry.getKey())
                .setValue(entry.getValue())
                .build();
    }

    @Override
    public StreamObserver<Prices.Price> addPrices(StreamObserver<Empty> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(Prices.Price value) {
                System.out.println("Received: " + value);
                idToPriceMap.put(value.getId(), value.getValue());
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
                System.out.println("Inserted all prices");
            }
        };
    }

    private static long getRandomPrice() {
        return RandomGenerator.getDefault().nextLong(1, 100);
    }

    public static class PriceNotFoundException extends RuntimeException {
    }

    public static class InternalServerError extends RuntimeException {
        public InternalServerError(Exception e) {
            super(e);
        }
    }
}
