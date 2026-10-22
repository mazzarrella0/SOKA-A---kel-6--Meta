package scheduling;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Pembuat/pembaca workload (panjang task dalam MI). */
public class Workload {
    /** Format sederhana: satu angka MI per baris (atau CSV, kolom pertama). Baris '#'/';' diabaikan. */
    public static double[] fromLengthFile(String path, int max) throws IOException {
        List<Double> l = new ArrayList<>();
        for (String s : Files.readAllLines(Paths.get(path))) {
            s = s.trim();
            if (s.isEmpty() || s.startsWith("#") || s.startsWith(";")) continue;
            try { l.add(Double.parseDouble(s.split("[,\\s]+")[0])); } catch (NumberFormatException e) { /* header */ }
            if (l.size() >= max) break;
        }
        return l.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /** Standard Workload Format (HPC2N, NASA iPSC/860): MI = run time (detik, kolom ke-4) x refMips. */
    public static double[] fromSwf(String path, int max, double refMips) throws IOException {
        List<Double> l = new ArrayList<>();
        for (String s : Files.readAllLines(Paths.get(path))) {
            s = s.trim();
            if (s.isEmpty() || s.startsWith(";")) continue;
            String[] f = s.split("\\s+");
            if (f.length < 4) continue;
            double run = Double.parseDouble(f[3]);
            if (run > 0) l.add(run * refMips);
            if (l.size() >= max) break;
        }
        return l.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /** Sintetis bergaya GoCJ: kategori small/medium/large/extra-large (rentang MI mengikuti paper GoCJ). */
    public static double[] gocjLike(int n, long seed) {
        Random r = new Random(seed);
        double[][] range = {{15000, 55000}, {59000, 99000}, {101000, 135000}, {150000, 337500}};
        double[] share = {0.30, 0.40, 0.20, 0.10};
        double[] len = new double[n];
        for (int i = 0; i < n; i++) {
            double u = r.nextDouble(), acc = 0; int c = 3;
            for (int k = 0; k < 4; k++) { acc += share[k]; if (u < acc) { c = k; break; } }
            len[i] = range[c][0] + r.nextDouble() * (range[c][1] - range[c][0]);
        }
        return len;
    }
}
