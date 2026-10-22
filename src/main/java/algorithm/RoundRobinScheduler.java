package scheduling;

import java.util.ArrayList;
import java.util.List;

/** Baseline sederhana; juga dipakai sebagai acuan normalisasi fitness. */
public class RoundRobinScheduler implements Scheduler {
    public String name() { return "RR"; }
    public int[] schedule(Problem p) {
        int[] a = new int[p.n];
        for (int i = 0; i < p.n; i++) a[i] = i % p.m;
        return a;
    }
    public List<Double> history() { return new ArrayList<>(); }
}
