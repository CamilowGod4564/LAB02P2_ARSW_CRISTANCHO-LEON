package edu.eci.arsw.blacklistvalidator;

import edu.eci.arsw.spamkeywordsdatasource.HostBlacklistsDataSourceFacade;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Validates a host against blacklists concurrently. */
public class HostBlackListsValidator {

    private static final int BLACK_LIST_ALARM_COUNT = 5;
    private static final Logger LOG = Logger.getLogger(HostBlackListsValidator.class.getName());

    public List<Integer> checkHost(String ipaddress) {
        return checkHost(ipaddress, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Splits the blacklist servers among {@code threadCount} workers. All workers
     * stop as soon as the shared result reaches the alarm threshold.
     */
    public List<Integer> checkHost(String ipaddress, int threadCount) {
        if (threadCount <= 0) {
            throw new IllegalArgumentException("threadCount must be greater than zero");
        }

        HostBlacklistsDataSourceFacade dataSource = HostBlacklistsDataSourceFacade.getInstance();
        int serverCount = dataSource.getRegisteredServersCount();
        int workersCount = Math.min(threadCount, serverCount);
        List<Integer> occurrences = new ArrayList<>(BLACK_LIST_ALARM_COUNT);
        Object occurrencesLock = new Object();
        AtomicBoolean alarmReached = new AtomicBoolean(false);
        AtomicInteger checkedListsCount = new AtomicInteger();
        List<Thread> workers = new ArrayList<>(workersCount);

        for (int worker = 0; worker < workersCount; worker++) {
            int start = worker * serverCount / workersCount;
            int end = (worker + 1) * serverCount / workersCount;
            Thread task = new Thread(() -> searchRange(ipaddress, start, end, dataSource,
                    occurrences, occurrencesLock, alarmReached, checkedListsCount),
                    "blacklist-worker-" + worker);
            workers.add(task);
            task.start();
        }

        boolean interrupted = false;
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException ex) {
                interrupted = true;
                alarmReached.set(true);
                for (Thread pendingWorker : workers) {
                    pendingWorker.interrupt();
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        List<Integer> result;
        synchronized (occurrencesLock) {
            result = new ArrayList<>(occurrences);
        }
        if (alarmReached.get() && result.size() >= BLACK_LIST_ALARM_COUNT) {
            dataSource.reportAsNotTrustworthy(ipaddress);
        } else {
            dataSource.reportAsTrustworthy(ipaddress);
        }

        LOG.log(Level.INFO, "Checked Black Lists:{0} of {1}",
                new Object[]{checkedListsCount.get(), serverCount});
        return Collections.unmodifiableList(result);
    }

    private void searchRange(String ipaddress, int start, int end,
            HostBlacklistsDataSourceFacade dataSource, List<Integer> occurrences,
            Object occurrencesLock, AtomicBoolean alarmReached,
            AtomicInteger checkedListsCount) {
        for (int server = start; server < end && !alarmReached.get(); server++) {
            checkedListsCount.incrementAndGet();
            if (dataSource.isInBlackListServer(server, ipaddress)) {
                synchronized (occurrencesLock) {
                    if (alarmReached.get()) {
                        return;
                    }
                    occurrences.add(server);
                    if (occurrences.size() == BLACK_LIST_ALARM_COUNT) {
                        alarmReached.set(true);
                        return;
                    }
                }
            }
        }
    }
}