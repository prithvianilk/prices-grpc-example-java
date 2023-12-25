package com.prithvianilk.prices;

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

import static com.google.rpc.Code.NOT_FOUND;

public class PriceService extends PriceServiceGrpc.PriceServiceImplBase {
    private static final StatusRuntimeException PRICE_NOT_FOUND_EXCEPTION = getPriceNotFoundException();

    private static StatusRuntimeException getPriceNotFoundException() {
        Status status = Status.newBuilder()
                .setCode(NOT_FOUND.getNumber())
                .setMessage("Price not found")
                .build();
        return StatusProto.toStatusRuntimeException(status);
    }

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
            System.out.printf("Failed to find price for id: %s\n", request.getId());
            responseObserver.onError(PRICE_NOT_FOUND_EXCEPTION);
            return;
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
                System.out.printf("Received: %s\n", value);
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
