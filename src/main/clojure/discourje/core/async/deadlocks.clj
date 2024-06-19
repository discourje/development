(ns discourje.core.async.deadlocks
  (:require [clojure.set :refer [intersection]]
            [clojure.core.async :as a]))

(defn- runtime-exception []
  (let [message "[SESSION FAILURE] Deadlock!"]
    (ex-info message {})))

(def ^:private permit 0000)

(defn- semaphore []
  (let [ret (a/chan 1)]
    (a/>!! ret permit)
    ret))

(def ^:private checkers (atom {}))

(defn- checker [n]
  {:threads (atom #{})
   :i (atom 0)
   :n (atom n)
   :semaphore (semaphore)
   :barriers (atom [])})

(defn create-checker [monitor n]
  (when (some? n)
    (swap! checkers assoc monitor (checker n))))

(defn- install-barrier [barriers ports]
  (let [barrier (a/chan)
        chans (mapv #(if (vector? %) (first %) %) ports)]
    (swap! barriers conj [barrier chans])
    barrier))

(defn- uninstall-barrier [barriers chan]
  (loop []
    (let [pred (fn [[_ chans]] (some #{chan} chans))
          old @barriers
          new (filterv (complement pred) old)]
      (if (compare-and-set! barriers old new)
        (first (first (filterv pred old)))
        (recur)))))

(def threads (atom {}))
(def ^:dynamic *thread-name* nil)

(defn spawn-thread []
  (swap! threads assoc *thread-name* nil))

(defn kill-thread []
  (if-let [x (get @threads *thread-name*)]
    (let [checker (first x)
          i (:i checker)
          n (:n checker)
          semaphore (:semaphore checker)
          barriers (:barriers checker)]

      (a/<!! semaphore)
      (swap! n dec)
      (if (or (<= @n 0) (< @i @n))
        (do (a/>!! semaphore permit))
        (do (a/>!! semaphore permit)
            (throw (runtime-exception)))))))

(defn alts!!-live [monitor ports & {:keys [default priority] :as opts}]
  (if-let [checker (get @checkers monitor)]
    (let [i (:i checker)
          n (:n checker)
          semaphore (:semaphore checker)
          barriers (:barriers checker)]

      (if-let [x (get @threads *thread-name*)]
        (swap! threads assoc *thread-name* (conj x checker))
        (swap! threads assoc *thread-name* #{checker}))

      (a/<!! semaphore)
      (let [[v c] (a/alts!! ports {:default nil :priority priority})]
        (if (= c :default)
          (if (contains? opts :default)
            default
            (if (< (swap! i inc) @n)
              (let [p (promise)
                    barrier (install-barrier barriers ports)
                    ret (a/do-alts (partial deliver p) ports {:priority priority})]
                (a/>!! semaphore permit)
                (let [[v c] (if ret @ret (deref p))]
                  (swap! i dec)
                  (if (some? v)
                    (a/>!! barrier "")
                    (a/close! (uninstall-barrier barriers c)))
                  [v c]))
              (do
                (reset! n 0)
                (a/>!! semaphore permit)
                (throw (runtime-exception)))))
          (do
            (if-let [barrier (uninstall-barrier barriers c)]
              (do
                (a/<!! barrier)
                (a/close! barrier)))
            (a/>!! semaphore permit)
            [v c]))))
    (a/alts!! ports opts)))

(defn >!!-live [monitor port val]
  (alts!!-live monitor [[port val]]))

(defn <!!-live [monitor port]
  (alts!!-live monitor [port]))