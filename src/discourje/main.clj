(ns discourje.main
  (:gen-class)
  (:require [discourje.core.async :refer :all]
            [discourje.benchmarks.benchmarkRunner :refer :all]
            [str-to-argv :refer (split-args)]))

(defn- find-value "find a value in parsed arguments, and set optional default value when it is not found"
  ([arguments key]
   (some #(when (= (first %) key) (second %)) arguments))
  ([arguments key default-value]
   (let [value (find-value arguments key)]
     (if (nil? value)
       default-value
       value))))

(defn now "get current date time" [] (new java.util.Date))
(println (clojure.string/replace (str (now)) #" " "-"))

(defn- run-discourje-benchmark [function amount iterations]
  (cond
    (= function "pl") (start-discourje-pipeline amount iterations)
    (= function "sg") (start-discourje-scattergather amount iterations)
    (= function "ob") (start-discourje-one-buyer iterations)
    (= function "tb") (start-discourje-two-buyers iterations)
    :else "No valid function command given for discourje benchmark!"))

(defn- run-clojure-benchmark [function amount iterations]
  (cond
    (= function "pl") (start-clojure-pipeline amount iterations)
    (= function "sg") (start-clojure-scattergather amount iterations)
    (= function "ob") (start-clojure-one-buyer iterations)
    (= function "tb") (start-clojure-two-buyers iterations)
    :else "No valid function command given for clojure benchmark!"))

(defn parse-int "parse string to integer" [key s]
  (try (Integer. (re-find #"[0-9]*" s))
       (catch Exception e (println "No valid argument for" key "setting value to 1") 1)))

(defn- parse-arguments [input]
  (let [arguments (vec (for [arg (partition 2 (split-args input))] (vec arg)))
        amount (parse-int "-a" (find-value arguments "-a" "1"))
        iterations (parse-int "-i" (find-value arguments "-i" "1"))
        output-dir (find-value arguments "-o")
        -d (find-value arguments "-d")
        -c (find-value arguments "-c")
        now (clojure.string/replace (str (now)) #" " "-")]
    (println "parsed arguments:" arguments)
    (set-exceptions-only)
    (cond
      (not (nil? -d)) (spit
                        (format "%s/StartTime:%s_-d_%s-a_%s_-i_%s.txt" output-dir now -d amount iterations)
                        (format "[%s] : %s" input (run-discourje-benchmark -d amount iterations)))
      (not (nil? -c)) (spit
                        (format "%s/StartTime:%s_-c_%s-a_%s_-i_%s.txt" output-dir now -c amount iterations)
                        (format "[%s] : %s" input (run-clojure-benchmark -c amount iterations)))
      :else (spit
              (format "%s/StartTime:%s_-d_%s-a_%s_-i_b%s.txt" output-dir now "" amount iterations)
              (format "Invalid input given! [%s]" input)))))

(defn -main
  "Run benchmarks for Discourje and clojure:

  The main function will parse the following arguments:
  -c or -d to target clojure(-c) or discourje (-d)

  with benchmark functions:
  pl = pipeline
  sc = scatter gather
  ob = one buyer protocol
  tb = two buyer protocol

  amount for pipeline and scatter gather can be set through (ignored for one and two buyer)
  -a <amount>: integer

  iterations for all benchmarks can be set through
  -i <iterations>: integer

  -o to set the output dir target

  Example: -c pl -a 200 -i 1000 -o /home/<username>/Documents
  Will start Clojure pipeline with 200 elements for 1000 iterations.

  All examples:
  Clojure:
  -c pl -a 200 -i 1000 -o /home/<username>/Documents
  -c sg -a 200 -i 1000 -o /home/<username>/Documents
  -c ob -i 1000 -o /home/<username>/Documents
  -c tb -i 1000 -o /home/<username>/Documents

  Discourje:
  -d pl -a 200 -i 1000 -o /home/<username>/Documents
  -d sg -a 200 -i 1000 -o /home/<username>/Documents
  -d ob -i 1000 -o /home/<username>/Documents
  -d tb -i 1000 -o /home/<username>/Documents
  "
  [& args]
  (parse-arguments (clojure.string/join " " args)))