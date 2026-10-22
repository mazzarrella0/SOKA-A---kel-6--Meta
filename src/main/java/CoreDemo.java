package scheduling;

/** Uji tanpa CloudSim: membandingkan RR, COA, TLBO dengan evaluator Problem. */
public class CoreDemo {
    public static void main(String[] a) {
        int n = a.length > 0 ? Integer.parseInt(a[0]) : 1000;
        Problem p = Scenario.build(Workload.gocjLike(n, 1), 0.5);
        Scheduler[] all = {new RoundRobinScheduler(), new CoaScheduler(42), new TlboScheduler(42)};
        for (Scheduler s : all) {
            p.evals = 0;
            long t = System.currentTimeMillis();
            int[] sol = s.schedule(p);
            Problem.Result r = p.evaluate(sol);
            System.out.printf("%-4s %s  [%d ms, %d evaluasi]%n", s.name(), r, System.currentTimeMillis() - t, p.evals - 1);
            if (!s.history().isEmpty()) {
                var h = s.history();
                System.out.printf("     fitness awal=%.4f akhir=%.4f%n", h.get(0), h.get(h.size() - 1));
            }
        }
    }
}
