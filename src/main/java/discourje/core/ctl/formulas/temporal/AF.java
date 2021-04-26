package discourje.core.ctl.formulas.temporal;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.Formula;
import discourje.core.ctl.formulas.Temporal;
import discourje.core.ctl.formulas.atomic.True;

import java.util.*;
import java.util.function.BiFunction;

public class AF extends Temporal {
    private final Formula arg;
    private final int hash;

    public AF(Formula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        var i = model.getLabelIndex(arg);

        State<?> target1 = null;
        State<?> target2 = null;
        var parents = new HashMap<State<?>, State<?>>();

        /*
         * Search for shortest path from source to target1, and from target1 to target2, such that: (a) every state on
         * the path violates arg; (b) target2 has a transition to target1.
         */

        var done = new HashSet<State<?>>();
        var todo = new Stack<State<?>>();

        todo.push(source);

        search:
        while (!todo.isEmpty()) {
            var s = todo.peek();

            for (State<?> next : s.getNextStates()) {
                if (todo.contains(next)) {
                    target1 = next;
                    target2 = s;
                    break search;
                }
                if (!next.hasLabel(i) && !done.contains(next)) {
                    parents.put(next, s);
                    todo.push(next);
                    continue search;
                }
            }

            done.add(s);
            todo.pop();
        }

        /*
         * Construct segments
         */

        BiFunction<State<?>, State<?>, List<Action>> f = (s, t) -> {
            var segment = new ArrayList<Action>();
            while (!s.equals(t)) {
                segment.add(t.getAction());
                t = parents.get(t);
            }
            Collections.reverse(segment);
            return segment;
        };

        var segment1 = f.apply(source, target1);
        var segment2 = f.apply(target1, target2);
        segment2.add(target1.getAction());
        return Arrays.asList(segment1, segment2);
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            Formula au = new AU(True.TRUE, arg);
            au.label(model);
            int auIndex = model.getLabelIndex(au);
            for (State<?> state : model.getStates()) {
                if (state.hasLabel(auIndex)) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public String toMCRL2() {
        var s = arg.toMCRL2();
        var x = "X";
        while (s.contains(x + ".")) {
            x += "X";
        }

        if (arg.isAction()) {
            return "mu " + x + ".(([true]" + x + " && <true>true) || <" + s + ">true)";
        } else {
            return "mu " + x + ".(([true]" + x + " && <true>true) || (" + s + "))";
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
