/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arst.concprg.prodcons;

import java.util.concurrent.BlockingQueue;

/**
 *
 * @author hcadavid
 */
public class Consumer extends Thread {

    private final BlockingQueue<Integer> queue;
    private final long consumptionDelayMillis;

    public Consumer(BlockingQueue<Integer> queue, long consumptionDelayMillis) {
        this.queue = queue;
        this.consumptionDelayMillis = consumptionDelayMillis;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                // take blocks until a product is available; it avoids busy waiting.
                int elem = queue.take();
                System.out.println("Consumer consumes " + elem);
                if (consumptionDelayMillis > 0) {
                    Thread.sleep(consumptionDelayMillis);
                }
            } catch (InterruptedException ex) {
                interrupt();
            }
        }
    }
}