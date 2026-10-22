# COA & TLBO untuk Cloud Task Scheduling (CloudSim Plus 8.x, Java 17)

## Struktur
- `Problem` – model + fungsi fitness bersama (makespan, cost, penalti deadline/budget/RAM); dipakai COA dan TLBO agar adil. Menghitung jumlah evaluasi (`evals`).
- `CoaScheduler` – Cuckoo Optimization Algorithm (Rajabioun, 2011): bertelur dalam ELR, buang 10% telur, kontrol populasi (Nmax), klaster k-means, imigrasi ke habitat terbaik. Default: Ninit=10, Nmax=10, telur 5–10, alpha=2, k=2, 100 iterasi.
- `TlboScheduler` – Teaching-Learning-Based Optimization (Rao dkk., 2011): fase guru, fase murid, seleksi greedy. Default: 50 peserta didik, 100 iterasi.
- `RoundRobinScheduler` – baseline sekaligus acuan normalisasi fitness.
- `Workload` – GoCJ-like sintetis, file panjang task (MI per baris), dan format SWF (HPC2N, NASA iPSC).
- `Scenario` – 20 VM heterogen (8 Small, 8 Medium, 4 Large) + harga per detik.
- `Main` – simulasi CloudSim Plus (2 datacenter x 5 host), output CSV satu baris.
- `CoreDemo` – uji cepat tanpa CloudSim.

Representasi kedua algoritma: posisi kontinu di [0, m), decode `floor` -> indeks VM.

## Menjalankan
    mvn -q compile exec:java -Dexec.args="algo=COA  workload=gocj n=1000 seed=1"
    mvn -q compile exec:java -Dexec.args="algo=TLBO workload=swf:HPC2N.swf n=1000 seed=1"
    mvn -q compile exec:java -Dexec.args="algo=RR   workload=data/gocj.txt n=1000"

Tanpa CloudSim: `javac -d out src/main/java/scheduling/{Problem,Scheduler,RoundRobinScheduler,CoaScheduler,TlboScheduler,Workload,Scenario,CoreDemo}.java && java -cp out scheduling.CoreDemo 1000`

## Catatan
- Deviasi sudut phi pada imigrasi COA diterapkan sebagai gangguan acak per dimensi (rotasi 2D asli tidak terdefinisi di n dimensi).
- Jumlah VM/harga/host ada di `Scenario` dan `Main.buildDatacenter`; deadline/budget default tanpa batas.
- Jika versi CloudSim Plus berbeda, ubah import `org.cloudsimplus.*`.
