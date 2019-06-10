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

(defn- exists-argument? "find argument" [arguments key]
  (not (nil? (find-value arguments key))))

(defn- run-discourje-benchmark [function amount iterations]
  (cond
    (= function "pl") (start-discourje-pipeline amount iterations)
    (= function "sg") (start-discourje-scattergather amount iterations)
    (= function "ob") (start-discourje-one-buyer iterations)
    (= function "tb") (start-discourje-two-buyers iterations)
    :else (println "No valid function command given for discourje benchmark!")))

(defn- run-clojure-benchmark [function amount iterations]
  (cond
    (= function "pl") (start-clojure-pipeline amount iterations)
    (= function "sg") (start-clojure-scattergather amount iterations)
    (= function "ob") (start-clojure-one-buyer iterations)
    (= function "tb") (start-clojure-two-buyers iterations)
    :else (println "No valid function command given for clojure benchmark!")))

(defn parse-int "parse string to integer"[key s]
  (try (Integer. (re-find #"[0-9]*" s))
       (catch Exception e (println "No valid argument for" key "setting value to 1") 1)))

(defn- parse-arguments [input]
  (let [arguments (vec (for [arg (partition 2 (split-args input))] (vec arg)))
        amount (parse-int "-a" (find-value arguments "-a" "1"))
        iterations (parse-int "-i" (find-value arguments "-i" "1"))
        is-discourje (exists-argument? arguments "-d")]
    (println "parsed arguments:" arguments)
    (if is-discourje
      (run-discourje-benchmark (find-value arguments "-d") amount iterations)
      (run-clojure-benchmark (find-value arguments "-c") amount iterations))))

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

  Example: -c pl -a 200 -i 1000
  Will start Clojure pipeline with 200 elements for 1000 iterations.

  All examples:
  Clojure:
  -c pl -a 200 -i 1000
  -c sg -a 200 -i 1000
  -c ob -i 1000
  -c tb -i 1000

  Discourje:
  -d pl -a 200 -i 1000
  -d sg -a 200 -i 1000
  -d ob -i 1000
  -d tb -i 1000
  "
  [& args]
  (parse-arguments (clojure.string/join " " args)))