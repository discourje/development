(ns ProjectSetup.ProjectSetup)

(defn sum [ x y z]
  (+ x y z))

(defmacro unless [condition & body]
  `(if (not ~condition)
     (do ~@body)))

(def flatList `(1 2 3 ~@(list 4 5 6)))
(def value2 flatList)

(def value 10)