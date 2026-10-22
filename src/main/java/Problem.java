package scheduling;

import java.util.Arrays;

/**
 * Model masalah: n task (MI) dipetakan ke m VM. Dipakai bersama oleh COA dan TLBO
 * sehingga fungsi fitness identik. Tidak bergantung pada CloudSim.
 *
 * Model eksekusi meniru CloudSim Plus dengan CloudletSchedulerSpaceShared:
 * VM berisi `pes` slot paralel; task dijalankan FCFS sesuai urutan indeks task,
 * masing-masing memakai 1 PE dengan kecepatan `mips`.
 */
public class Problem {
    public final double[] length;      // MI per task
    public final double[] deadline;    // detik, per task (Infinity = tanpa deadline)
    public final double[] taskRam;     // MB per task
    public final double[] vmMips;      // MIPS per PE
    public final int[] vmPes;          // jumlah PE per VM
    public final double[] vmRam;       // MB
    public final double[] vmPrice;     // biaya per detik
    public final double budget;        // total biaya maksimum
    public final int n, m;

    // bobot fitness
    public double alpha = 0.5;         // bobot makespan; (1-alpha) untuk cost
    public double lambda = 10.0;       // bobot penalti pelanggaran batasan
    public double makespanRef = 1, costRef = 1; // normalisasi (lihat calibrate)
    public long evals = 0;             // jumlah evaluasi fitness (untuk perbandingan adil)

    public Problem(double[] length, double[] deadline, double[] taskRam, double[] vmMips,
                   int[] vmPes, double[] vmRam, double[] vmPrice, double budget) {
        this.length = length; this.deadline = deadline; this.taskRam = taskRam;
        this.vmMips = vmMips; this.vmPes = vmPes; this.vmRam = vmRam; this.vmPrice = vmPrice;
        this.budget = budget; this.n = length.length; this.m = vmMips.length;
    }

    public static class Result {
        public double makespan, cost, utilization, imbalance, penalty, fitness;
        public int deadlineViolations, ramViolations;
        public double[] vmFinish;
        @Override public String toString() {
            return String.format("makespan=%.2f cost=%.2f util=%.3f DI=%.3f dlViol=%d ramViol=%d fitness=%.4f",
                makespan, cost, utilization, imbalance, deadlineViolations, ramViolations, fitness);
        }
    }

    /** Evaluasi satu jadwal: assign[i] = indeks VM untuk task i. */
    public Result evaluate(int[] assign) {
        evals++;
        double[][] slot = new double[m][];
        for (int j = 0; j < m; j++) slot[j] = new double[vmPes[j]];
        double[] busy = new double[m], vmFinish = new double[m];
        int dl = 0, ramV = 0;
        for (int i = 0; i < n; i++) {
            int j = assign[i];
            double[] s = slot[j];
            int k = 0;
            for (int q = 1; q < s.length; q++) if (s[q] < s[k]) k = q;
            double dur = length[i] / vmMips[j];
            s[k] += dur;
            busy[j] += dur;
            if (s[k] > vmFinish[j]) vmFinish[j] = s[k];
            if (s[k] > deadline[i]) dl++;
            if (taskRam[i] > vmRam[j]) ramV++;
        }
        Result r = new Result();
        r.vmFinish = vmFinish;
        double cost = 0, busySum = 0, capSum = 0, tmin = Double.MAX_VALUE, tavg = 0;
        for (int j = 0; j < m; j++) {
            r.makespan = Math.max(r.makespan, vmFinish[j]);
            cost += vmPrice[j] * vmFinish[j];          // VM disewa sampai task terakhirnya selesai
            busySum += busy[j];
            tmin = Math.min(tmin, vmFinish[j]);
            tavg += vmFinish[j] / m;
        }
        for (int j = 0; j < m; j++) capSum += vmPes[j] * r.makespan;
        r.cost = cost;
        r.utilization = capSum > 0 ? busySum / capSum : 0;
        r.imbalance = tavg > 0 ? (r.makespan - tmin) / tavg : 0;   // (Tmax - Tmin) / Tavg
        r.deadlineViolations = dl; r.ramViolations = ramV;
        double budgetViol = budget < Double.MAX_VALUE ? Math.max(0, cost - budget) / budget : 0;
        r.penalty = (dl + ramV) / (double) n + budgetViol;
        r.fitness = alpha * r.makespan / makespanRef + (1 - alpha) * cost / costRef + lambda * r.penalty;
        return r;
    }

    public double fitness(int[] assign) { return evaluate(assign).fitness; }

    /** Set nilai normalisasi dari jadwal acuan (mis. Round Robin) agar kedua objektif sebanding. */
    public void calibrate(int[] reference) {
        Result r = evaluate(reference);
        makespanRef = r.makespan; costRef = r.cost;
    }

    public static int[] floorDecode(double[] x, int m) {
        int[] a = new int[x.length];
        for (int i = 0; i < x.length; i++) a[i] = Math.min(m - 1, Math.max(0, (int) Math.floor(x[i])));
        return a;
    }

    @Override public String toString() { return "Problem(n=" + n + ", m=" + m + ", vmPes=" + Arrays.toString(vmPes) + ")"; }
}
