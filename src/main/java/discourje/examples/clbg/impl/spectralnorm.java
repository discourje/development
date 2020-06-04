//package discourje.examples.clbg.impl;
//
///*
//The Computer Language Benchmarks Game
//https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
//
//Based on C# entry by Isaac Gouy
//contributed by Jarkko Miettinen
//Parallel by The Anh Tran
// */
//
//import discourje.examples.tacas2020.Benchmarks;
//import discourje.core.Chan;
//import discourje.core.ClojureCoreAsync;
//import discourje.core.DiscourjeCoreAsync;
//
//import java.text.DecimalFormat;
//import java.text.NumberFormat;
////import java.util.concurrent.CyclicBarrier;
//
//public class spectralnorm {
//    private static final NumberFormat formatter = new DecimalFormat("#.000000000");
//
//    public static void main(String[] args) {
//        int n = 1000;
//        if (args.length > 0) n = Integer.parseInt(args[0]);
//
//        System.out.println(formatter.format(spectralnormGame(n)));
//    }
//
//    private static final double spectralnormGame(int n) {
//        // create unit vector
//        double[] u = new double[n];
//        double[] v = new double[n];
//        double[] tmp = new double[n];
//
//        for (int i = 0; i < n; i++)
//            u[i] = 1.0;
//
//        // get available processor, then set up syn object
//        //int nthread = Runtime.getRuntime ().availableProcessors ();
//        //Approximate.barrier = new CyclicBarrier (nthread);
//        int nthread = Benchmarks.K;
//        Approximate.chans = new Chan[nthread];
//
//        DiscourjeCoreAsync.Monitor m;
//        switch (Benchmarks.LIB) {
//            case CLOJURE:
//                m = null;
//                break;
//            case DISCOURJE:
//                m = DiscourjeCoreAsync.monitor("discourje.examples.tacas2020.clbg.spectralnorm.spec", "s", nthread);
//                break;
//            default:
//                throw new RuntimeException();
//        }
//
//        for (int i = 0; i < nthread; i++) {
//            switch (Benchmarks.LIB) {
//                case CLOJURE:
//                    Approximate.chans[i] = ClojureCoreAsync.chan(1);
//                    break;
//                case DISCOURJE:
//                    Approximate.chans[i] = DiscourjeCoreAsync.chan(1, "worker[" + i + "]", "worker[" + ((i + 1) % nthread) + "]", m);
//                    break;
//                default:
//                    throw new RuntimeException();
//            }
//        }
//
//        int chunk = n / nthread;
//        Approximate[] ap = new Approximate[nthread];
//
//        for (int i = 0; i < nthread; i++) {
//            int r1 = i * chunk;
//            int r2 = (i < (nthread - 1)) ? r1 + chunk : n;
//
//            ap[i] = new Approximate(i, u, v, tmp, r1, r2);
//        }
//
//
//        double vBv = 0, vv = 0;
//        for (int i = 0; i < nthread; i++) {
//            try {
//                ap[i].join();
//
//                vBv += ap[i].m_vBv;
//                vv += ap[i].m_vv;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        return Math.sqrt(vBv / vv);
//    }
//
//    private static class Approximate extends Thread {
//        //private static CyclicBarrier barrier;
//        private static Chan[] chans;
//        private final int i;
//        private double[] _u;
//        private double[] _v;
//        private double[] _tmp;
//        private int range_begin, range_end;
//        private double m_vBv = 0, m_vv = 0;
//
//        public Approximate(int i, double[] u, double[] v, double[] tmp, int rbegin, int rend) {
//            super();
//
//            this.i = i;
//
//            _u = u;
//            _v = v;
//            _tmp = tmp;
//
//            range_begin = rbegin;
//            range_end = rend;
//
//            start();
//        }
//
//        /* return element i,j of infinite matrix A */
//        private final static double eval_A(int i, int j) {
//            int div = (((i + j) * (i + j + 1) >>> 1) + i + 1);
//            return 1.0 / div;
//        }
//
//        public void run() {
//            // 20 steps of the power method
//            for (int i = 0; i < 10; i++) {
//                MultiplyAtAv(_u, _tmp, _v);
//                MultiplyAtAv(_v, _tmp, _u);
//            }
//
//            for (int i = range_begin; i < range_end; i++) {
//                m_vBv += _u[i] * _v[i];
//                m_vv += _v[i] * _v[i];
//            }
//        }
//
//        /* multiply vector v by matrix A, each thread evaluate its range only */
//        private final void MultiplyAv(final double[] v, double[] Av) {
//            for (int i = range_begin; i < range_end; i++) {
//                double sum = 0;
//                for (int j = 0; j < v.length; j++)
//                    sum += eval_A(i, j) * v[j];
//
//                Av[i] = sum;
//            }
//        }
//
//        /* multiply vector v by matrix A transposed */
//        private final void MultiplyAtv(final double[] v, double[] Atv) {
//            for (int i = range_begin; i < range_end; i++) {
//                double sum = 0;
//                for (int j = 0; j < v.length; j++)
//                    sum += eval_A(j, i) * v[j];
//
//                Atv[i] = sum;
//            }
//        }
//
//        /* multiply vector v by matrix A and then by matrix A transposed */
//        private final void MultiplyAtAv(final double[] v, double[] tmp, double[] AtAv) {
//            try {
//                MultiplyAv(v, tmp);
//                // all thread must syn at completion
//                //barrier.await ();
//                if (i == 0) {
//                    chans[0].send(0);
//                    chans[chans.length - 1].recv();
//                    chans[0].send(0);
//                    chans[chans.length - 1].recv();
//                } else {
//                    chans[i - 1].recv();
//                    chans[i % chans.length].send(0);
//                    chans[i - 1].recv();
//                    chans[i % chans.length].send(0);
//                }
//
//                MultiplyAtv(tmp, AtAv);
//                // all thread must syn at completion
//                //barrier.await ();
//                if (i == 0) {
//                    chans[0].send(0);
//                    chans[chans.length - 1].recv();
//                    chans[0].send(0);
//                    chans[chans.length - 1].recv();
//                } else {
//                    chans[i - 1].recv();
//                    chans[i % chans.length].send(0);
//                    chans[i - 1].recv();
//                    chans[i % chans.length].send(0);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//}