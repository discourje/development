package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.State;
import discourje.core.validation.Model;

import java.util.*;

import static discourje.core.validation.formulas.CtlFormulas.EF;
import static discourje.core.validation.formulas.CtlFormulas.not;

public class AG implements CtlFormula {
    private final CtlFormula arg;
    private final int hash;

    public AG(CtlFormula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            CtlFormula ag = not(EF(not(arg)));
            ag.label(model);
            int agLabelIndex = model.getLabelIndex(ag);
            for (State<?> state : model.getStates()) {
                if (state.hasLabel(agLabelIndex)) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public List<Action> getCounterexample(Model<?> model) {
        var i = model.getLabelIndex(arg);

        var sources = model.getInitialStates();
        State<?> target = null;
        var parents = new HashMap<State<?>, State<?>>();

        /*
         * Search for a state that violates arg, reachable from initial states
         */

        var done = new HashSet<State<?>>();
        var todo = new LinkedList<State<?>>();

        for (State<?> source : sources) {
            done.add(source);
            todo.offer(source);
        }

        while (!todo.isEmpty()) {
            var s = todo.poll();
            if (!s.hasLabel(i)) {
                target = s;
                break;
            } else {
                for (State<?> next : s.getNextStates()) {
                    if (!done.contains(next)) {
                        parents.put(next, s);
                        done.add(next);
                        todo.offer(next);
                    }
                }
            }
        }

        if (target == null) {
            throw new IllegalStateException();
        }

        /*
         * Construct trace
         */

        var trace = new ArrayList<Action>();

        var s = target;
        while (!sources.contains(s)) {
            trace.add(s.getAction());
            s = parents.get(s);
        }

        Collections.reverse(trace);
        return trace;
    }

    @Override
    public String toString() {
        return "AG(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AG that = (AG) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
