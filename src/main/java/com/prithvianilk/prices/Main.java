package com.prithvianilk.prices;

public class Main {
    private static final String HOST = "localhost";

    private static final int PORT = 3000;

    public static void main(String[] args) throws InterruptedException {
        Thread.ofVirtual().start(() -> new PriceServer(PORT).start());
        var app = new PriceApplication(HOST, PORT);
        app.getEachPricePriceIndividually();
        app.streamUpdatedValuesOfRandomPriceToServer();
        app.streamAllPricesFromServer();
    }
}