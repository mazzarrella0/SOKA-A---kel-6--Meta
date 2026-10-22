package scheduling;

import java.util.List;

public interface Scheduler {
    String name();
    /** Mengembalikan assign[i] = indeks VM untuk task i. */
    int[] schedule(Problem p);
    /** Fitness terbaik per iterasi/generasi (untuk kurva konvergensi). */
    List<Double> history();
}
