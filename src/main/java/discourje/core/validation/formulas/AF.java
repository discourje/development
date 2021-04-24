package discourje.core.validation.formulas;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

import java.util.Objects;
import java.util.concurrent.ForkJoinPool;

public class AF implements CtlFormula {
    private final CtlFormula arg;
    private final int hash;

    public AF(CtlFormula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            CtlFormula au = new AU(True.TRUE, arg);
            au.label(model);
            int auIndex = model.getLabelIndex(au);

            /* Sequential version */
            for (DMState<?> state : model.getStates()) {
                if (state.hasLabel(auIndex)) {
                    state.addLabel(labelIndex);
                }
            }

            /* Parallel version 1
            model.getStates().parallelStream().forEach((state) -> {
                if (state.hasLabel(auIndex)) {
                    state.addLabel(labelIndex);
                }
            });
            */

            /* Parallel version 2
            var states = model.getStates();
            var n = 8;
            var work = states.size() / n + 1;
            System.out.println(n + " " + work);
            Thread[] threads = new Thread[n];
            for (int i = 0; i < n; i++) {
                final var id = i;
                threads[i] = new Thread(() -> {
                    for (int x = id * work; x < (id + 1) * work; x++) {
                        if (x < states.size() - 1) {
                            var state = states.get(x);
                            if (state.hasLabel(auIndex)) {
                                state.addLabel(labelIndex);
                            }
                        }
                    }

                });
                threads[i].start();
            }
            for (int i = 0; i < n; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }*/
        }
    }

    @Override
    public String toString() {
        return "AF(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AF that = (AF) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
