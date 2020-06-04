(ns discourje.examples.config)

(def ^:dynamic *lib* nil)
(def ^:dynamic *input* nil)
(def ^:dynamic *output* nil)
(def ^:dynamic *time* nil)

(defn lib? [x]
  (contains? #{:clj :dcj :dcj-nil} x))

(defn clj-or-dcj []
  (if (contains? (ns-aliases *ns*) 'a)
    (ns-unalias *ns* 'a))

  (case *lib*
    :clj (do (alias 'a 'clojure.core.async)
             (intern 'clojure.core.async 'monitor)
             (intern 'clojure.core.async 'link))
    :dcj (alias 'a 'discourje.core.async)
    :dcj-nil (alias 'a 'discourje.core.async)
    nil))