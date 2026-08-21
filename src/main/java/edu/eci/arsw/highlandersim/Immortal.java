package edu.eci.arsw.highlandersim;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Immortal extends Thread {

    private static final AtomicInteger idGenerator = new AtomicInteger(0);
    private final int id = idGenerator.getAndIncrement();

    private ImmortalUpdateReportCallback updateCallback=null;
    
    private int health;
    
    private int defaultDamageValue;

    private final List<Immortal> immortalsPopulation;

    private final String name;

    private final Random r = new Random(System.currentTimeMillis());

    private volatile boolean pauseRequested = false;
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();
    private volatile boolean stopRequested = false;


    public Immortal(String name, List<Immortal> immortalsPopulation, int health, int defaultDamageValue, ImmortalUpdateReportCallback ucb) {
        super(name);
        this.updateCallback=ucb;
        this.name = name;
        this.immortalsPopulation = immortalsPopulation;
        this.health = health;
        this.defaultDamageValue=defaultDamageValue;
    }

    public void run() {

        while (!stopRequested) {
            checkPause();
            if (stopRequested) break;
            Immortal im;

            int myIndex = immortalsPopulation.indexOf(this);

            int nextFighterIndex = r.nextInt(immortalsPopulation.size());

            //avoid self-fight
            if (nextFighterIndex == myIndex) {
                nextFighterIndex = ((nextFighterIndex + 1) % immortalsPopulation.size());
            }

            im = immortalsPopulation.get(nextFighterIndex);

            this.fight(im);

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    private void checkPause() {
        synchronized (pauseLock) {
            if (pauseRequested) {
                paused = true;
                while (pauseRequested) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                paused = false;
            }
        }
    }

    public void requestPause() { pauseRequested = true; }

    public boolean isPaused() { return paused; }

    public void resumeImmortal() {
        synchronized (pauseLock) {
            pauseRequested = false;
            pauseLock.notifyAll();
        }
    }

    public void fight(Immortal i2) {

        Immortal first  = (this.id < i2.id) ? this : i2;
        Immortal second = (this.id < i2.id) ? i2 : this;

        synchronized (first) {
            synchronized (second) {

                if (i2.getHealth() > 0) {
                    i2.changeHealth(i2.getHealth() - defaultDamageValue);
                    this.health += defaultDamageValue;
                    updateCallback.processReport("Fight: " + this + " vs " + i2 + "\n");
                } else {
                    updateCallback.processReport(this + " says:" + i2 + " is already dead!\n");
                }
            }
        }
    }

    public synchronized void changeHealth(int v) {
        health = v;
    }

    public synchronized int getHealth() {
        return health;
    }

    public void stopImmortal() {
        stopRequested = true;
        synchronized (pauseLock) {
            pauseRequested = false;
            pauseLock.notifyAll();
        }
        this.interrupt();
    }

    @Override
    public String toString() {

        return name + "[" + health + "]";
    }

}
