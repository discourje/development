(ns discourje.examples.config)

(def ^:dynamic *run* nil)
(def ^:dynamic *lint* nil)
(def ^:dynamic *input* nil)
(def ^:dynamic *output* nil)
(def ^:dynamic *time* nil)

(defn run? [x]
  (contains? #{:clj :dcj :dcj-nil} x))

(defn lint? [x]
  (contains? #{:mcrl2 :dcj} x))

(defn clj-or-dcj []
  (if (contains? (ns-aliases *ns*) 'a)
    (ns-unalias *ns* 'a))
  (case *run*
    :clj (do (alias 'a 'clojure.core.async)
             (intern 'clojure.core.async 'monitor)
             (intern 'clojure.core.async 'link))
    :dcj (alias 'a 'discourje.core.async)
    :dcj-nil (alias 'a 'discourje.core.async)
    (alias 'a 'discourje.core.async)))