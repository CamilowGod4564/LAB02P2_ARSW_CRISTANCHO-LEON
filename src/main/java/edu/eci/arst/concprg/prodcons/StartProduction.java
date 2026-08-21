package edu.eci.arst.concprg.prodcons;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class StartProduction {

    public static void main(String[] args) throws InterruptedException {
        int stockLimit = Integer.getInteger("stockLimit", 5);
        long productionDelayMillis = Long.getLong("productionDelayMillis", 0L);
        long consumptionDelayMillis = Long.getLong("consumptionDelayMillis", 1_000L);

        if (stockLimit <= 0 || productionDelayMillis < 0 || consumptionDelayMillis < 0) {
            throw new IllegalArgumentException("Stock limit and delays must be non-negative; stockLimit must be greater than zero.");
        }

        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(stockLimit);
        new Producer(queue, productionDelayMillis).start();

        // Let the producer build initial stock before the consumer begins.
        Thread.sleep(5_000);
        new Consumer(queue, consumptionDelayMillis).start();
    }
}