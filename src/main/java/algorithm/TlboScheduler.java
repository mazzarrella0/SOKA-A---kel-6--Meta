package scheduling;

import java.util.*;

/**
 * Teaching-Learning-Based Optimization (Rao dkk., 2011) untuk task scheduling.
 * Peserta didik = posisi kontinu x[i] in [0, m); decode: VM = floor(x[i]).
 * Fase guru : Xnew = X + r*(Xteacher - TF*Mean), TF in {1,2}
 * Fase murid: Xnew = Xi + r*(Xi - Xj) jika Xi lebih baik dari Xj, selain itu Xi + r*(Xj - Xi)
 * Solusi baru diterima hanya jika fitness lebih baik (greedy).
 */
public class TlboScheduler implements Scheduler {
    public int learners = 50, iterations = 100;
    private final Random rnd;
    private final List<Double> hist = new ArrayList<>();

    public TlboScheduler(long seed) { rnd = new Random(seed); }
    public String name() { return "TLBO"; }
    public List<Double> history() { return hist; }

    public int[] schedule(Problem p) {
        hist.clear();
        int n = p.n, m = p.m, N = learners; double top = Math.nextDown((double) m);
        double[][] x = new double[N][n]; double[] f = new double[N];
        // 1. Inisialisasi kelas
        for (int k = 0; k < N; k++) {
            for (int i = 0; i < n; i++) x[k][i] = rnd.nextDouble() * m;
            f[k] = p.fitness(Problem.floorDecode(x[k], m));
        }
        hist.add(f[argMin(f)]);
        for (int it = 0; it < iterations; it++) {
            // 2. Guru = terbaik; rata-rata kelas per dimensi
            int t = argMin(f);
            double[] mean = new double[n];
            for (int k = 0; k < N; k++) for (int i = 0; i < n; i++) mean[i] += x[k][i] / N;
            double[] teacher = x[t].clone();
            // 3. Fase guru
            for (int k = 0; k < N; k++) {
                int tf = 1 + rnd.nextInt(2);
                double[] nx = new double[n];
                for (int i = 0; i < n; i++) nx[i] = clamp(x[k][i] + rnd.nextDouble() * (teacher[i] - tf * mean[i]), top);
                accept(p, x, f, k, nx, m);   // 5. seleksi greedy
            }
            // 4. Fase murid
            for (int k = 0; k < N; k++) {
                int j; do { j = rnd.nextInt(N); } while (j == k);
                boolean iBetter = f[k] < f[j];
                double[] nx = new double[n];
                for (int i = 0; i < n; i++) {
                    double r = rnd.nextDouble();
                    nx[i] = clamp(iBetter ? x[k][i] + r * (x[k][i] - x[j][i]) : x[k][i] + r * (x[j][i] - x[k][i]), top);
                }
                accept(p, x, f, k, nx, m);
            }
            hist.add(f[argMin(f)]);   // 6. ulangi sampai iterasi maksimum
        }
        return Problem.floorDecode(x[argMin(f)], m);
    }

    private void accept(Problem p, double[][] x, double[] f, int k, double[] nx, int m) {
        double nf = p.fitness(Problem.floorDecode(nx, m));
        if (nf < f[k]) { x[k] = nx; f[k] = nf; }
    }
    private static int argMin(double[] a) { int b = 0; for (int i = 1; i < a.length; i++) if (a[i] < a[b]) b = i; return b; }
    private static double clamp(double v, double top) { return Math.max(0, Math.min(top, v)); }
}
