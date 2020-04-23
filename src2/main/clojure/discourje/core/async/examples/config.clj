(ns discourje.core.async.examples.config)

(def ^:dynamic *lib* nil)

(defn lib? [x]
  (contains? #{:clj :dcj :dcj-nil} x))

(def ^:dynamic *input* nil)

(def ^:dynamic *output* nil)

(def ^:dynamic *time* nil)