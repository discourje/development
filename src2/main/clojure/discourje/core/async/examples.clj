(ns discourje.core.async.examples
  (:gen-class)
  (:refer-clojure :exclude [compare])
  (:require [clojure.string :refer [join last-index-of]]
            [discourje.core.async.examples.config :as config])
  (:import (java.time LocalDateTime)
           (org.apache.commons.math3.distribution TDistribution)))

;;;;
;;;; Execution
;;;;

(defn configs
  ([m]
   (configs (:lib m) (:program m) (:input m)))
  ([libs programs inputs]
   {:pre  [(vector? libs)
           (vector? programs)
           (map? inputs) (every? vector? (vals inputs))]
    :post [(vec %) (every? map? %)]}
   (let [f (fn [k vals m] (mapv #(merge m {k %}) vals))
         inputs (loop [inputs inputs
                       result [{}]]
                  (if (empty? inputs)
                    result
                    (let [[k vals] (first inputs)]
                      (recur (rest inputs) (reduce into (mapv (partial f k vals) result))))))
         configs [{}]
         configs (reduce into (mapv (partial f :lib libs) configs))
         configs (reduce into (mapv (partial f :program programs) configs))
         configs (reduce into (mapv (partial f :input inputs) configs))]
     configs)))

(defn run
  ([config]
   (run (:lib config) (:program config) (:input config)))
  ([lib program input]
   (binding [config/*lib* lib
             config/*input* (merge {:resolution 1} input)
             config/*output* nil
             config/*time* nil]
     (try
       (require program :reload)
       {:lib     lib
        :program program
        :input   input
        :output  config/*output*
        :time    config/*time*}
       (catch Throwable t (.printStackTrace t))))))

(defn run-all
  ([configs]
   (mapv #(run %) configs))
  ([libs programs inputs]
   (run-all (configs libs programs inputs))))

(defn start [lib program input]
  (.start (Thread. ^Runnable (fn [] (println (run lib program input))))))

;;;;
;;;; Post-processing
;;;;

(defn samples [data]
  (loop [data data
         samples {}]
    (if (empty? data)
      samples
      (let [m (first data)
            lib (:lib m)
            x [(:program m) (:input m)]
            point (:ticks (:output m))
            ys (if-let [ys-old (get samples x)]
                 (if-let [sample' (get ys-old lib)]
                   (merge ys-old {lib (conj sample' point)})
                   (merge ys-old {lib [point]}))
                 {lib [point]})]
        (recur (rest data) (assoc samples x ys))))))

(defn stats [samples]
  (loop [samples samples
         stats {}]
    (if (empty? samples)
      stats
      (let [[x ys] (first samples)
            ys' (apply merge (map (fn [[lib sample]]
                                    (let [n (count sample)
                                          μ-hat (/ (apply + sample) n)
                                          σ2-hat (if (= n 1)
                                                   0
                                                   (/ (apply + (map #(* (- % μ-hat) (- % μ-hat)) sample)) (dec n)))]
                                      {lib {:sample sample
                                            :n      n
                                            :μ-hat  (long μ-hat)
                                            :σ2-hat (long σ2-hat)}}))
                                  ys))]
        (recur (rest samples) (assoc stats x ys'))))))

;; References:
;;  * Cochran: Sampling Techniques, 3rd Edition. John Wiley & Sons, 1977. (§2.5--§2.8, §2.11, §6.4--§6.5)
;;  * Luo, John: Efficiently Evaluating Speedup Using Sampled Processor Simulation. IEEE Comput. Archit. Lett. 3, 2004.

(defn ratios [stats]
  (loop [stats stats
         ratios {}]
    (if (empty? stats)
      ratios
      (let [[x ys] (first stats)
            [base-lib base-stats] (first ys)
            ys' (apply merge (map (fn [[lib stats]]
                                    (let [y base-stats
                                          x stats
                                          n (min (:n x) (:n y))
                                          μ-hat (/ (:μ-hat y) (:μ-hat x))
                                          σ2-hat (* (/ 1 n)
                                                    (/ 1 (:μ-hat x))
                                                    (/ 1 (:μ-hat x))
                                                    (+ (:σ2-hat y) (* μ-hat μ-hat (:σ2-hat x)) (* -2 0)))
                                          t (.inverseCumulativeProbability (TDistribution. n) 0.975)]
                                      {[base-lib lib] {:μ-hat      (float μ-hat)
                                                       :σ2-hat     (float σ2-hat)
                                                       :confidence (* t (Math/sqrt σ2-hat))}}))
                                  (rest ys)))]
        (recur (rest stats) (assoc ratios x ys'))))))

(defn chart [stats ratios]
  (let [bars (mapv (fn [[y0 y1]]
                     (str "\\textsc{" (name y0) "}/\\textsc{" (name y1) "}"))
                   (keys (second (first ratios))))

        barnames (for [i (range (count bars))]
                   (str "\\newcommand{\\barname" (join (for [_ (range (inc i))] "i")) "}{" (nth bars i) "}"))

        barpoints (mapv (fn [[[program input] m]]
                          (str "{\\texttt{" (subs (str program) (inc (last-index-of (str program) "."))) "} ("
                               (join ", " (map (fn [[k v]]
                                                 (case v
                                                   true (name k)
                                                   false (str "\\sout{" (name k) "}")
                                                   (str (name k) "=" v)))
                                               input)) ")} "
                               (join " " (map (fn [[_ ratio]] (str (:μ-hat ratio) " " (:confidence ratio))) m))))
                        ratios)

        ymin (dec (apply min (conj (reduce into (mapv (fn [[_ m]] (mapv (fn [[_ ratio]] (:μ-hat ratio)) m)) ratios)) 1)))]
    (str "
\\documentclass{standalone}

\\usepackage{pgfplots}
\\usepackage{pgfplotstable}
\\usepackage{ulem}
\\usetikzlibrary{calc}
\\usetikzlibrary{patterns}

\\begin{document}

\\begin{tikzpicture}
\t\\scriptsize
\t
\t% % %
\t% % % Simple stats
\t% % %
\t
\t% " stats "
\t
\t% % %
\t% % % Ratios
\t% % %
\t
\t% " ratios "
\t
\t% % %
\t% % % Chart
\t% % %
\t
\t" (join "\n\t" barnames) "
\t
\t\\pgfplotstableread{
\t\tx y1 y1-error y2 y2-error
\t\t" (join "\n\t\t" barpoints) "
\t}\\table
\t
\t\\begin{axis}[
\t\t\tybar,
\t\t\tlegend style={draw=none, fill=none, legend columns=-1, inner sep=0pt},
\t\t\tvisualization depends on=y \\as \\rawy,
\t\t\t%nodes near coords,
\t\t\t%nodes near coords=\\pgfmathparse{\\rawy+1}\\pgfmathresult,
\t\t\t%every node near coord/.append style={shift={(axis direction cs:0,-\\rawy)}, anchor={sign(\\rawy)*90}, font=\\tiny},
\t\t\t%
\t\t\tx=1.5cm,
\t\t\tenlarge x limits={true, abs value=.5},
\t\t\txtick=data,
\t\t\txticklabels from table={\\table}{x},
\t\t\txticklabel style = {align=center, text width=1.25cm},
\t\t\tminor y tick num=4,
\t\t\t%
\t\t\tymin=" ymin ",
\t\t\tenlarge y limits={true, value=.125},
\t\t\tyticklabel={\\pgfmathparse{(\\tick+1)}\\pgfmathresult},
\t\t\ty filter/.code={\\pgfmathparse{#1-1}\\pgfmathresult},
\t\t\t%
\t\t\tmajor grid style={dashed, black},
\t\t\textra y ticks={0},
\t\t\textra y tick style={grid=major}]
\t\t
\t\t\\addlegendentry{\\barnamei}
\t\t\\addplot plot [black, fill=lightgray!50, error bars/.cd, y dir=both, y explicit] table [x expr=\\coordindex, y=y1, y error=y1-error] {\\table};
\t\t
\t\t\\addlegendentry{\\barnameii}
\t\t\\addplot plot [black, fill=lightgray, error bars/.cd, y dir=both, y explicit] table [x expr=\\coordindex, y=y2, y error=y2-error] {\\table};
\t\\end{axis}
\\end{tikzpicture}
\\end{document}")))

(println (chart nil '{[discourje.core.async.examples.micro.ring {:buffered true, :secs 10, :k 2}] {[:clj :dcj-nil] {:μ-hat 1.5242262, :σ2-hat 3.1103522E-5, :confidence 0.011389861746674834}, [:clj :dcj] {:μ-hat 1.7601341, :σ2-hat 2.87667E-4, :confidence 0.03463847050718687}}, [discourje.core.async.examples.micro.ring {:buffered true, :secs 10, :k 3}] {[:clj :dcj-nil] {:μ-hat 1.5784104, :σ2-hat 1.2164457E-4, :confidence 0.02252475315033561}, [:clj :dcj] {:μ-hat 1.85297, :σ2-hat 4.5212702E-4, :confidence 0.0434254081842686}}, [discourje.core.async.examples.micro.ring {:buffered true, :secs 10, :k 4}] {[:clj :dcj-nil] {:μ-hat 1.5907134, :σ2-hat 1.3422611E-4, :confidence 0.02366094981944861}, [:clj :dcj] {:μ-hat 1.8445338, :σ2-hat 1.9847706E-4, :confidence 0.028771919560185332}}}))

;;;;
;;;; Main
;;;;

(defmacro version []
  (str (LocalDateTime/now)))

(defn -main [& args]
  (try
    (case (first args)
      "run"
      (let [args (rest args)]
        (if (< (count args) 2)
          (throw (ex-info "" {::message "Not enough arguments"})))

        (let [lib (keyword (first args))
              program (symbol (str "discourje.core.async.examples" "." (second args)))
              input (read-string (join " " (rest (rest args))))]

          (if (not (contains? #{:clj :dcj :dcj-nil} lib))
            (throw (ex-info "" {::message "Unknown lib"})))

          (if (not (contains? #{'discourje.core.async.examples.micro.ring
                                'discourje.core.async.examples.micro.mesh
                                'discourje.core.async.examples.micro.star}
                              program))
            (throw (ex-info "" {::message "Unknown program"})))

          (prn (run lib program input))))

      "benchmark"
      (let [args (rest args)]
        (if (< (count args) 1)
          (throw (ex-info "" {::message "Not enough arguments"})))

        (let [n (read-string (first args))]
          (doseq [config (configs (read-string (join " " (rest args))))]
            (doseq [_ (range n)]
              (println (str "java -jar discourje-examples.jar run "
                            (name (:lib config)) " "
                            (clojure.string/replace (str (:program config)) "discourje.core.async.examples." "") " "
                            (:input config)))))))

      "chart"
      (let [args (rest args)]
        (let [data (read-string (str "[" (join " " args) "]"))
              samples (samples data)
              stats (stats samples)
              ratios (ratios stats)
              chart (chart stats ratios)]
          (println chart)))

      "experiment"
      (let [args (rest args)]
        (let [id (System/currentTimeMillis)]
          (println (str "echo \"*** Begin: Experiment #" id " ***\""))
          (println (str "echo \"Generating benchmark...\""))
          (println (str "java -jar discourje-examples.jar benchmark " (join " " args) " > benchmark-" id ".sh"))
          (println (str "echo \"Running benchmark and generating data...\""))
          (println (str "sh benchmark-" id ".sh > data-" id ".txt"))
          (println (str "rm benchmark-" id ".sh"))
          (println (str "echo \"Processing data and generating chart...\""))
          (println (str "java -jar discourje-examples.jar chart \"$(cat data-" id ".txt)\" > chart-" id ".tex"))
          (println (str "if [ -x \"$(command -v pdflatex)\" ]; then"))
          (println (str "\techo \"Compiling chart...\""))
          (println (str "\tpdflatex chart-" id ".tex > /dev/null"))
          (println (str "\trm chart-" id ".aux chart-" id ".log"))
          (println (str "else"))
          (println (str "\techo \"Not compiling chart (pdflatex required but not provided)\""))
          (println (str "fi"))
          (println (str "echo \"*** End: Experiment #" id " ***\""))))

      (throw (ex-info "" {::message "Unknown command"})))

    (catch Throwable t
      (let [m (ex-data t)]
        (println)
        (if (contains? m ::message)
          (do (println (str "Discourje Examples (" (version) ")"))
              (println (str "Error: " (::message m)))
              (println (str "Usage 1: java -jar discourje-examples.jar run <lib> <program> <input>"))
              (println (str "  <lib>     \u2208 {clj, dcj, dcj-nil}"))
              (println (str "  <program> \u2208 {micro.ring, micro.mesh, micro.star}"))
              (println (str "Usage 2: java -jar discourje-examples.jar script <n> <configs>"))
              (println (str "Usage 3: java -jar discourje-examples.jar chart <data>"))
              (println (str "Usage 4: java -jar discourje-examples.jar experiment <n> <configs>")))
          (.printStackTrace t))
        (println)))))