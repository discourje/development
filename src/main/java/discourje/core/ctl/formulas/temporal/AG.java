package discourje.core.ctl.formulas.temporal;

import discourje.core.ctl.Labels;
import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.Formula;
import discourje.core.ctl.formulas.Temporal;

import java.util.*;
import java.util.function.BiFunction;

import static discourje.core.ctl.Formulas.EF;
import static discourje.core.ctl.Formulas.not;

public class AG extends Temporal {
    private final Formula arg;
    private final int hash;

    public AG(Formula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        State<?> target = null;
        var parents = new HashMap<State<?>, State<?>>();

        /*
         * Search for a shortest path from source to target, such that: (a) every state on the path, *except* target,
         * satisfies arg; (b) target violates arg.
         */

        var done = new HashSet<State<?>>();
        var todo = new LinkedList<State<?>>();

        done.add(source);
        todo.offer(source);

        while (!todo.isEmpty()) {
            var s = todo.poll();
            if (!model.hasLabel(s, arg)) {
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

        var segment = f.apply(source, target);
        var segments = new ArrayList<List<Action>>();
        segments.add(segment);
        segments.addAll(arg.extractWitness(model, target));
        return segments;
    }

    @Override
    public Labels label(Model<?> model) {
        Formula ag = not(EF(not(arg)));
        return model.calculateLabels(ag);
    }

    @Override
    public String toMCRL2() {
        if (arg.isAction()) {
            return "[true*]<" + arg.toMCRL2() + ">true";
        } else {
            return "[true*](" + arg.toMCRL2() + ")";
        }
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
