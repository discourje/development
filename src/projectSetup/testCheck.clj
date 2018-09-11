(ns projectSetup.testCheck)

(defn sumAll
  "test method for checking correct config and installation of test.check library,
   added arity overloads for testing(they are not needed for practical reasons!)"
  ([] (int 0) )
  ([x] (int x))
  ([x & more] (apply + x more)))

(defn messenger
  "test method for checking correct config and installation of test.check library"
  ([]     (messenger "Hello world!"))
  ([msg]  (println msg))
  ([msg & more]  (apply println msg more)))