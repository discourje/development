(ns projectSetup.ProjectSetup)

(defn sum [ x y z]
  (+ x y z))

(defmacro unless "Evaluates the body only when the condition is false." [condition & body]
  `(if (not ~condition)
     (do ~@body)))

(def flatList `(1 2 3 ~@(list 4 5 6)))

(def value 10)