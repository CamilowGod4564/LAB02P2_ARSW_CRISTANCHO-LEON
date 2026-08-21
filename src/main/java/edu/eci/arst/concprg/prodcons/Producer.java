/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arst.concprg.prodcons;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author hcadavid
 */
public class Producer extends Thread {

    private final BlockingQueue<Integer> queue;

    private int dataSeed = 0;
    private final Random rand;
    private final long productionDelayMillis;

    public Producer(BlockingQueue<Integer> queue, long productionDelayMillis) {
        this.queue = queue;
        rand = new Random(System.currentTimeMillis());
        this.productionDelayMillis = productionDelayMillis;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            dataSeed = dataSeed + rand.nextInt(100);
            try {
                // put waits when the bounded queue is full, avoiding busy waiting
                // and ensuring that its capacity (stock limit) is never exceeded.
                queue.put(dataSeed);
                System.out.println("Producer added " + dataSeed);

                if (productionDelayMillis > 0) {
                    Thread.sleep(productionDelayMillis);
                }
            } catch (InterruptedException ex) {
                interrupt();
            }
        }
    }
}
