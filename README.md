# Discourje

The Discourje project aims to help programmers cope with channels and
concurrency bugs in Clojure programs, based on dynamic analysis. The idea is
that programmers write not only implementations of communication protocols in
their Clojure programs, but also specifications. Discourje then offers a
run-time verification library to ensure that channel actions in implementations
are safe relative to specifications.
