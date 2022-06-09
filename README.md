# Discourje

The Discourje project aims to help programmers cope with channels and concurrency bugs in Clojure programs (that use `core.async`), based on dynamic analysis. The idea is that programmers write not only implementations of communication protocols in their Clojure programs, but also specifications. Discourje then offers a run-time verification library to ensure that channel actions in implementations are safe relative to specifications.

## Resources

Suggested resources to get an overview of Discourje: talk 1 and/or paper 1. 

### Talks

1. Sung-Shik Jongmans. **dcj-lint: Analysis of Specifications of Multiparty Sessions.** ACM Joint European Software Engineering Conference and Symposium on the Foundations of Software Engineering (ESEC/FSE), 2021. [[link](https://www.youtube.com/watch?v=f1MgTrxLKeI)]

2. Sung-Shik Jongmans. **Discourje: Runtime Verification of Communication Protocols in Clojure.** International Conference on Tools and Algorithms for the Construction and Analysis of Systems (TACAS), 2020. [[link](https://www.morressier.com/o/event/6048becc82fa0a0019cb3048/article/604907f51a80aac83ca25d9e)]

3. Ruben Hamers. **Discourje: Automatically validated message exchange patterns in Clojure.** Dutch Clojure Day (DCD), 2019. [[link](https://www.youtube.com/watch?v=Vf6lfrX5caw)]

### Papers

1. Ruben Hamers, Erik Horlings, and Sung-Shik Jongmans. **The Discourje Project: Run-Time Verification of Communication Protocols in Clojure**. International Journal on Software Tools for Technology Transfer (in press).

2. Erik Horlings and Sung-Shik Jongmans. **dcj-lint: Analysis of Specifications of Multiparty Sessions.** Proceedings of ESEC/FSE'21. [[link](https://dx.doi.org/10.1145/3468264.3473127), [pdf](https://sungshik.github.io/papers/esecfse2021.pdf)]

3. Ruben Hamers and Sung-Shik Jongmans. **Discourje: Runtime Verification of Communication Protocols in Clojure.** Proceedings of TACAS'20. [[link](https://dx.doi.org/10.1007/978-3-030-45190-5_15), [pdf](https://sungshik.github.io/papers/tacas2020.pdf)]

## Contributors

- Ruben Hamers
- Erik Horlings
- Sung-Shik Jongmans
