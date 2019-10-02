package de.jlab.cardroid.utils;

import java.util.ArrayList;

public class UsageStatistics {
    private ArrayList<UsageStatisticsListener> listeners = new ArrayList<>();

    private ArrayList<Integer> history = new ArrayList<>();
    private long firstTime = 0;
    private int count = 0;

    private long interval;
    private int historyLength;

    public UsageStatistics(long interval, int historyLength) {
        this.interval = interval;
        this.historyLength = historyLength;
    }

    public void count(int amount) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.firstTime > this.interval) {
            this.history.add(0, this.count);
            if (this.history.size() > this.historyLength) {
                this.history.subList(this.historyLength, this.history.size()).clear();
            }

            for (UsageStatisticsListener listener : this.listeners) {
                listener.onInterval(this.count, this);
            }

            this.count = 0;
            this.firstTime = currentTime;
        }

        this.count += amount;
    }

    public void count() {
        this.count(1);
    }

    public float getAverage() {
        if (this.history.isEmpty()) {
            return 0;
        }

        float average = 0;
        for (int entry : this.history) {
            average += entry;
        }
        return average / this.history.size();
    }

    public float getAverageReliability() {
        return this.history.size() / (float)this.historyLength;
    }

    public interface UsageStatisticsListener {
        void onInterval(int count, UsageStatistics statistics);
    }

    public void addListener(UsageStatisticsListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(UsageStatisticsListener listener) {
        this.listeners.remove(listener);
    }
}
