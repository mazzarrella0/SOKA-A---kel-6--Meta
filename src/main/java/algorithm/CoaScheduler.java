package scheduling;

import java.util.*;

/**
 * Cuckoo Optimization Algorithm (Rajabioun, 2011) untuk task scheduling.
 * Habitat = vektor kontinu x[i] in [0, m); decode: VM = floor(x[i]).
 * Alur: bertelur dalam ELR -> buang ~10% telur terburuk -> menetas + kontrol populasi (Nmax)
 *       -> klaster (k-means) -> imigrasi ke habitat terbaik kelompok terbaik.
 * Catatan adaptasi: deviasi sudut phi pada imigrasi (aslinya rotasi 2D) diterapkan sebagai
 * gangguan acak per dimensi sebesar sin(phi)*|langkah|, karena rotasi tidak terdefinisi di n dimensi.
 */
public class CoaScheduler implements Scheduler {
    public int nInit = 10, nMax = 10, minEggs = 5, maxEggs = 10, iterations = 100, clusters = 2;
    public double alpha = 2.0, killRatio = 0.10, omega = Math.PI / 6;
    private final Random rnd;
    private final List<Double> hist = new ArrayList<>();

    public CoaScheduler(long seed) { rnd = new Random(seed); }
    public String name() { return "COA"; }
    public List<Double> history() { return hist; }

    private static class Hab { double[] x; double f; Hab(double[] x, double f) { this.x = x; this.f = f; } }

    public int[] schedule(Problem p) {
        hist.clear();
        int n = p.n, m = p.m; double top = Math.nextDown((double) m);
        List<Hab> cuckoos = new ArrayList<>();
        Hab best = null;
        // 1. Inisialisasi habitat awal
        for (int k = 0; k < nInit; k++) {
            double[] x = new double[n];
            for (int i = 0; i < n; i++) x[i] = rnd.nextDouble() * m;
            Hab h = new Hab(x, p.fitness(Problem.floorDecode(x, m)));
            cuckoos.add(h);
            if (best == null || h.f < best.f) best = h;
        }
        hist.add(best.f);

        for (int it = 0; it < iterations; it++) {
            // 2. Bertelur: jumlah telur acak per kuku, ELR = alpha * (telur kuku / total telur) * m
            int[] eggs = new int[cuckoos.size()]; int total = 0;
            for (int c = 0; c < eggs.length; c++) { eggs[c] = minEggs + rnd.nextInt(maxEggs - minEggs + 1); total += eggs[c]; }
            List<Hab> laid = new ArrayList<>();
            for (int c = 0; c < eggs.length; c++) {
                double elr = alpha * eggs[c] / total * m;
                double[] base = cuckoos.get(c).x;
                for (int e = 0; e < eggs[c]; e++) {
                    double[] x = new double[n];
                    for (int i = 0; i < n; i++) x[i] = clamp(base[i] + (2 * rnd.nextDouble() - 1) * elr, top);
                    laid.add(new Hab(x, p.fitness(Problem.floorDecode(x, m))));
                }
            }
            // 3. Seleksi telur: ~10% terburuk terdeteksi inang dan dibuang
            laid.sort(Comparator.comparingDouble(h -> h.f));
            int keep = laid.size() - (int) Math.round(killRatio * laid.size());
            laid = new ArrayList<>(laid.subList(0, keep));
            // 4. Menetas -> gabung dengan kuku lama, kontrol populasi: simpan nMax terbaik
            List<Hab> pop = new ArrayList<>(cuckoos); pop.addAll(laid);
            pop.sort(Comparator.comparingDouble(h -> h.f));
            cuckoos = new ArrayList<>(pop.subList(0, Math.min(nMax, pop.size())));
            if (cuckoos.get(0).f < best.f) best = cuckoos.get(0);
            // 5. Klaster + imigrasi ke habitat terbaik dari kelompok terbaik
            double[] goal = goalPoint(cuckoos, n);
            for (Hab c : cuckoos) {
                double lambda = rnd.nextDouble(), phi = (2 * rnd.nextDouble() - 1) * omega;
                double[] x = new double[n];
                for (int i = 0; i < n; i++) {
                    double step = lambda * (goal[i] - c.x[i]);
                    x[i] = clamp(c.x[i] + step + Math.sin(phi) * Math.abs(step) * (2 * rnd.nextDouble() - 1), top);
                }
                c.x = x; c.f = p.fitness(Problem.floorDecode(x, m));
                if (c.f < best.f) best = new Hab(x.clone(), c.f);
            }
            hist.add(best.f);   // 6. ulangi sampai iterasi maksimum
        }
        return Problem.floorDecode(best.x, m);
    }

    /** K-means sederhana (centroid awal = habitat acak); goal = habitat terbaik di kelompok dengan rata-rata fitness terbaik. */
    private double[] goalPoint(List<Hab> pop, int n) {
        int k = Math.min(clusters, pop.size());
        if (k <= 1) return pop.get(0).x;
        double[][] cen = new double[k][];
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < pop.size(); i++) idx.add(i);
        Collections.shuffle(idx, rnd);
        for (int c = 0; c < k; c++) cen[c] = pop.get(idx.get(c)).x.clone();
        int[] lab = new int[pop.size()];
        for (int round = 0; round < 5; round++) {
            for (int i = 0; i < pop.size(); i++) {
                double bd = Double.MAX_VALUE;
                for (int c = 0; c < k; c++) {
                    double d = 0; double[] a = pop.get(i).x;
                    for (int q = 0; q < n; q++) { double t = a[q] - cen[c][q]; d += t * t; }
                    if (d < bd) { bd = d; lab[i] = c; }
                }
            }
            for (int c = 0; c < k; c++) {
                double[] s = new double[n]; int cnt = 0;
                for (int i = 0; i < pop.size(); i++) if (lab[i] == c) { cnt++; for (int q = 0; q < n; q++) s[q] += pop.get(i).x[q]; }
                if (cnt > 0) { for (int q = 0; q < n; q++) s[q] /= cnt; cen[c] = s; }
            }
        }
        double bestMean = Double.MAX_VALUE; Hab goal = pop.get(0);
        for (int c = 0; c < k; c++) {
            double sum = 0; int cnt = 0; Hab gb = null;
            for (int i = 0; i < pop.size(); i++) if (lab[i] == c) { sum += pop.get(i).f; cnt++; if (gb == null || pop.get(i).f < gb.f) gb = pop.get(i); }
            if (cnt > 0 && sum / cnt < bestMean) { bestMean = sum / cnt; goal = gb; }
        }
        return goal.x;
    }

    private static double clamp(double v, double top) { return Math.max(0, Math.min(top, v)); }
}
