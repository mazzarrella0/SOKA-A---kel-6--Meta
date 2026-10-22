package scheduling;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.util.*;

/**
 * Simulasi CloudSim Plus 8.x dengan penjadwalan task oleh RR / COA / TLBO.
 *
 * Argumen (opsional, format key=value):
 *   algo=RR|COA|TLBO    (default COA)
 *   workload=gocj|<path file MI>|swf:<path file .swf>   (default gocj)
 *   n=1000  alpha=0.5  seed=42  mips=1000 (acuan konversi SWF)
 *
 * Output satu baris CSV: algo,seed,n,makespan,cost,utilization,DI,est_makespan,est_cost,schedMs
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Map<String, String> o = new HashMap<>();
        for (String s : args) { String[] kv = s.split("=", 2); if (kv.length == 2) o.put(kv[0], kv[1]); }
        String algo = o.getOrDefault("algo", "COA").toUpperCase();
        int n = Integer.parseInt(o.getOrDefault("n", "1000"));
        double alpha = Double.parseDouble(o.getOrDefault("alpha", "0.5"));
        long seed = Long.parseLong(o.getOrDefault("seed", "42"));
        String wl = o.getOrDefault("workload", "gocj");

        double[] len;
        if (wl.equals("gocj")) len = Workload.gocjLike(n, seed);
        else if (wl.startsWith("swf:")) len = Workload.fromSwf(wl.substring(4), n, Double.parseDouble(o.getOrDefault("mips", "1000")));
        else len = Workload.fromLengthFile(wl, n);

        Problem prob = Scenario.build(len, alpha);
        Scheduler sch = switch (algo) {
            case "COA" -> new CoaScheduler(seed);
            case "TLBO" -> new TlboScheduler(seed);
            default -> new RoundRobinScheduler();
        };
        long t0 = System.nanoTime();
        int[] assign = sch.schedule(prob);
        double schedMs = (System.nanoTime() - t0) / 1e6;
        Problem.Result est = prob.evaluate(assign);

        // ---- Simulasi CloudSim Plus ----
        CloudSimPlus sim = new CloudSimPlus();
        buildDatacenter(sim);   // DC1
        buildDatacenter(sim);   // DC2
        DatacenterBroker broker = new DatacenterBrokerSimple(sim);

        List<Vm> vms = new ArrayList<>();
        for (int j = 0; j < prob.m; j++) {
            int t = Scenario.vmTypeOf(j);
            Vm vm = new VmSimple(Scenario.MIPS[t], Scenario.PES[t]);
            vm.setRam((long) Scenario.RAM[t]).setBw(1000).setSize(10_000);
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            vms.add(vm);
        }
        List<Cloudlet> cls = new ArrayList<>();
        for (int i = 0; i < prob.n; i++) {
            Cloudlet c = new CloudletSimple((long) prob.length[i], 1);
            c.setUtilizationModelCpu(new UtilizationModelFull());
            c.setVm(vms.get(assign[i]));          // pemetaan hasil algoritma
            cls.add(c);
        }
        broker.submitVmList(vms);
        broker.submitCloudletList(cls);
        sim.start();

        // ---- Metrik dari hasil simulasi ----
        double[] vmFinish = new double[prob.m], busy = new double[prob.m];
        for (Cloudlet c : broker.getCloudletFinishedList()) {
            int j = (int) c.getVm().getId();
            vmFinish[j] = Math.max(vmFinish[j], c.getFinishTime());
            busy[j] += c.getTotalExecutionTime();
        }
        double mk = 0, cost = 0, busySum = 0, cap = 0, tmin = Double.MAX_VALUE, tavg = 0;
        for (int j = 0; j < prob.m; j++) {
            mk = Math.max(mk, vmFinish[j]);
            cost += prob.vmPrice[j] * vmFinish[j];
            busySum += busy[j]; tmin = Math.min(tmin, vmFinish[j]); tavg += vmFinish[j] / prob.m;
        }
        for (int j = 0; j < prob.m; j++) cap += prob.vmPes[j] * mk;
        System.out.println("algo,seed,n,makespan,cost,utilization,DI,est_makespan,est_cost,schedMs");
        System.out.printf(Locale.US, "%s,%d,%d,%.3f,%.3f,%.4f,%.4f,%.3f,%.3f,%.1f%n",
            sch.name(), seed, prob.n, mk, cost, busySum / cap, (mk - tmin) / tavg, est.makespan, est.cost, schedMs);
    }

    /** 1 datacenter: 5 host heterogen (2x TipeA 8core@2660, 2x TipeB 4core@2000, 1x TipeC 16core@3000). */
    private static Datacenter buildDatacenter(CloudSimPlus sim) {
        List<Host> hosts = new ArrayList<>();
        int[][] spec = {{8, 2660, 16384, 2}, {4, 2000, 8192, 2}, {16, 3000, 32768, 1}};   // core, MIPS, RAM MB, jumlah
        for (int[] s : spec)
            for (int k = 0; k < s[3]; k++) {
                List<Pe> pes = new ArrayList<>();
                for (int c = 0; c < s[0]; c++) pes.add(new PeSimple(s[1]));
                hosts.add(new HostSimple(s[2], 10_000, 1_000_000, pes));
            }
        return new DatacenterSimple(sim, hosts);
    }
}
