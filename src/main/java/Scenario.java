package scheduling;

/** Konfigurasi VM (heterogen) dan pembentukan Problem; dipakai CoreDemo dan Main. */
public class Scenario {
    // Tipe VM: Small(1 PE x1000), Medium(2 PE x1500), Large(4 PE x2000)
    static final double[] MIPS = {1000, 1500, 2000};
    static final int[] PES = {1, 2, 4};
    static final double[] RAM = {1024, 2048, 4096};
    static final double[] PRICE = {0.01, 0.025, 0.05};   // per detik
    public static final int[] COUNT = {8, 8, 4};          // 20 VM

    public static int vmTypeOf(int vmIndex) {
        return vmIndex < COUNT[0] ? 0 : vmIndex < COUNT[0] + COUNT[1] ? 1 : 2;
    }

    public static Problem build(double[] length, double alpha) {
        int m = COUNT[0] + COUNT[1] + COUNT[2], n = length.length;
        double[] mips = new double[m], ram = new double[m], price = new double[m];
        int[] pes = new int[m];
        for (int j = 0; j < m; j++) {
            int t = vmTypeOf(j);
            mips[j] = MIPS[t]; pes[j] = PES[t]; ram[j] = RAM[t]; price[j] = PRICE[t];
        }
        double[] dl = new double[n], tr = new double[n];
        java.util.Arrays.fill(dl, Double.POSITIVE_INFINITY);   // deadline per task: isi sesuai dataset bila ada
        java.util.Arrays.fill(tr, 512);                        // kebutuhan RAM task (MB)
        Problem p = new Problem(length, dl, tr, mips, pes, ram, price, Double.MAX_VALUE);
        p.alpha = alpha;
        int[] rr = new RoundRobinScheduler().schedule(p);
        p.calibrate(rr);                                       // normalisasi dengan Round Robin
        return p;
    }
}
