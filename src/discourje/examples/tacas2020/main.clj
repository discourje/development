(ns discourje.examples.tacas2020.main
  (:gen-class))

(import discourje.examples.tacas2020.clbg.spectralnorm.spectralnorm)
(import discourje.examples.tacas2020.Benchmarks)

(defn bench
  [time f]
  (let [begin (System/nanoTime)
        deadline (+ begin (* time 1000 1000 1000))]
    (loop [n 0]
      (f)
      (let [end (System/nanoTime)
            n' (+ n 1)]
        (if (< end deadline)
          (recur n')
          (binding [*out* *err*]
            (println (- end begin) "ns,"
                     n' "runs,"
                     (quot (- end begin) n') "ns/run")))))))

(defn -main
  [& args]
  (try
    (when (< (count args) 4)
      (throw (Exception. "Not enough arguments")))

    (let [verify (nth args 0)
          k (nth args 1)
          time (nth args 2)
          program (nth args 3)]

      (cond
        (= verify "no")
        (Benchmarks/useClojure)
        (= verify "yes")
        (Benchmarks/useDiscourje)
        :else
        (throw (Exception. "<language>")))

      (try
        (if (> (Integer/parseInt k) 0)
          (Benchmarks/setK (Integer/parseInt k))
          (throw (Exception. "<k>")))
        (catch NumberFormatException _
          (throw (Exception. "<k>"))))

      (try
        (if (< (Integer/parseInt time) 0)
          (throw (Exception. "<time>")))
        (catch NumberFormatException _
          (throw (Exception. "<time>"))))

      (cond
        (= program "spectral-norm")
        (do
          (binding [*out* *err*] (println args))
          (bench (Integer/parseInt time)
                 #(spectralnorm/main (into-array String [(nth args 4)]))))
        :else
        (throw (Exception. "<program>"))))

    (catch Exception e
      (println "Error:" (.getMessage e))
      (println "Usage: java -jar tacas2020.jar <verify?> <k> <time> <program> ...")
      (println "  <verify?> in {no, yes}")
      (println "  <k>       in {0, 1, 2, ...}")
      (println "  <time>    in {0, 1, 2, ...}")
      (println "  <program> in {spectral-norm}"))))

;(-main "yes" "2" "10" "spectral-norm" "5500")