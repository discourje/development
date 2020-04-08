package discourje.core.async.impl.lts;

import java.util.*;
import java.util.function.Function;

public class LTS<Spec> {

    private Map<Spec, State<Spec>> states = new LinkedHashMap<>();

    private Collection<State<Spec>> initialStates;

    private Function<Spec, Map<Action, Collection<Spec>>> expander;

    public LTS(Collection<Spec> initialStateSpecs, Function<Spec, Map<Action, Collection<Spec>>> expander) {
        this.initialStates = new LinkedHashSet<>();
        for (Spec initialStateSpec : initialStateSpecs) {
            this.initialStates.add(newOrGetState(initialStateSpec));
        }

        this.expander = expander;
    }

    @Override
    public String toString() {
        return LTSs.toAldebaran(this);
    }

    public void expandRecursively() {
        expandRecursively(Integer.MAX_VALUE);
    }

    public void expandRecursively(int bound) {
        for (State<Spec> s : states.values()) {
            s.expandRecursively(bound);
        }
    }

    public Collection<Action> getActions() {
        var actions = new LinkedHashSet<Action>();
        for (State<Spec> s : states.values()) {
            actions.addAll(s.getTransitionsOrNull().getActions());
        }
        return actions;
    }

    public Collection<State<Spec>> getInitialStates() {
        return initialStates;
    }

    public Collection<State<Spec>> getStates() {
        return states.values();
    }

    private synchronized State<Spec> newOrGetState(Spec spec) {
        var s = states.get(spec);
        //noinspection Java8MapApi
        if (s == null) {

            s = new State<>() {
                private Transitions<Spec> transitions;

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    State<?> state = (State<?>) o;
                    return Objects.equals(spec, state.getSpec());
                }

                @Override
                public int hashCode() {
                    return Objects.hash(spec);
                }

                @Override
                public String toString() {
                    return spec.toString();
                }

                @Override
                public synchronized void expandRecursively(int bound) {
                    if (bound > 0) {
                        var targetsToExpand = new LinkedHashSet<State<Spec>>();

                        if (transitions == null) {
                            transitions = new Transitions<>();
                            var source = this;
                            var targetSpecs = expander.apply(spec);
                            for (Map.Entry<Action, Collection<Spec>> e : targetSpecs.entrySet()) {
                                var a = e.getKey();
                                for (Spec targetSpec : e.getValue()) {
                                    var target = newOrGetState(targetSpec);
                                    source.transitions.addTarget(a, target);
                                    if (target.getTransitionsOrNull() == null) {
                                        targetsToExpand.add(target);
                                    }
                                }
                            }
                        }

                        for (State<Spec> target : targetsToExpand) {
                            target.expandRecursively(bound - 1);
                        }
                    }
                }

                @Override
                public Spec getSpec() {
                    return spec;
                }

                @Override
                public Transitions<Spec> getTransitionsOrNull() {
                    return transitions;
                }
            };

            states.put(spec, s);
        }

        return s;
    }

    public interface State<Spec> {

        default void expand() {
            expandRecursively(1);
        }

        default void expandRecursively() {
            expandRecursively(Integer.MAX_VALUE);
        }

        void expandRecursively(int bound);

        Spec getSpec();

        Transitions<Spec> getTransitionsOrNull();
    }
}
