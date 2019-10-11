(ns discourje.examples.tacas2020.main
  (:gen-class))

(import discourje.examples.tacas2020.Benchmarks)
(import discourje.examples.tacas2020.clbg.spectralnorm.spectralnorm)

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
        (do (Benchmarks/useClojure)
            (require '[clojure.core.async :refer [<!! >!! close! chan thread]]))
        (= verify "yes")
        (do (Benchmarks/useDiscourje)
            (require '[discourje.core.async :refer :all]))
        :else
        (throw (Exception. "<language>")))

      (try
        (if (> (Integer/parseInt k) 0)
          (do (Benchmarks/setK (Integer/parseInt k))
              (def K (Benchmarks/K)))
          (throw (Exception. "<k>")))
        (catch NumberFormatException _
          (throw (Exception. "<k>"))))

      (try
        (if (>= (Integer/parseInt time) 0)
          (do (Benchmarks/setTime (Long/parseLong time))
              (def TIME (Benchmarks/TIME)))
          (throw (Exception. "<time>")))
        (catch NumberFormatException _
          (throw (Exception. "<time>"))))

      (cond

        ;;
        ;; Micro benchmarks
        ;;

        (and (= program "micro/ring") (= verify "no"))
        (do (binding [*out* *err*] (print args "-> "))
            (require '[discourje.examples.tacas2020.micro.ring.clojure :refer :all])
            (def n-iter (Integer/parseInt (nth args 4)))
            (eval '(discourje.examples.tacas2020.micro.ring.clojure/run
                     discourje.examples.tacas2020.main/K
                     discourje.examples.tacas2020.main/TIME
                     discourje.examples.tacas2020.main/n-iter)))

        (and (= program "micro/ring") (= verify "yes"))
        (do (binding [*out* *err*] (print args "-> "))
            (require '[discourje.examples.tacas2020.micro.ring.discourje :refer :all])
            (def n-iter (Integer/parseInt (nth args 4)))
            (eval '(discourje.examples.tacas2020.micro.ring.discourje/run
                     discourje.examples.tacas2020.main/K
                     discourje.examples.tacas2020.main/TIME
                     discourje.examples.tacas2020.main/n-iter)))

        ;;
        ;; CLBG benchmarks
        ;;

        (= program "clbg/spectral-norm")
        (do (binding [*out* *err*] (print args "-> "))
            (bench (Benchmarks/TIME)
                   #(spectralnorm/main (into-array String [(nth args 4)]))))

        ;;
        ;; Misc
        ;;

        (and (= program "misc/ttt") (= verify "no"))
        (do (binding [*out* *err*] (print args "-> "))
            (bench (Benchmarks/TIME)
                   #(load "/discourje/examples/tacas2020/misc/ttt/clojure")))

        (and (= program "misc/ttt") (= verify "yes"))
        (do (binding [*out* *err*] (print args "-> "))
            (bench (Benchmarks/TIME)
                   #(load "/discourje/examples/tacas2020/misc/ttt/discourje")))

        :else
        (throw (Exception. "<program>"))))

    (catch Exception e
      (println "Error:" (.getMessage e))
      (println "Usage: java -jar tacas2020.jar <verify?> <k> <time> <program> ...")
      (println "  <verify?> in {no, yes}")
      (println "  <k>       in {0, 1, 2, ...}")
      (println "  <time>    in {0, 1, 2, ...}")
      (println "  <program> in {clbg/spectral-norm, micro/ring, misc/ttt}"))))

;(-main "yes" "2" "5" "clbg/spectral-norm" "5500")
;(-main "yes" "2" "5" "micro/ring" "1")
;(-main "no" "2" "5" "misc/ttt")