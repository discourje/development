package discourje.core.async.impl.lts;

import java.util.*;
import java.util.function.Function;

public class LTS<Spec> {

    private Map<Spec, State<Spec>> states = new LinkedHashMap<>();

    private Collection<State<Spec>> initialStates;

    private Function<Spec, Map<Action, Collection<Spec>>> expander;

    public LTS(Collection<Spec> initialStateSpecs, Function<Spec, Map<Action, Collection<Spec>>> expander, boolean expandRecursively) {
        this.initialStates = new LinkedHashSet<>();
        for (Spec initialStateSpec : initialStateSpecs) {
            this.initialStates.add(newOrGetState(initialStateSpec));
        }

        this.expander = expander;
        if (expandRecursively) {
            for (State<Spec> initialState : initialStates) {
                initialState.expandRecursively();
            }
        }
    }

    @Override
    public String toString() {
        return LTSs.toAldebaran(this);
    }

    public void expandRecursively() {
        for (State<Spec> s : states.values()) {
            s.expandRecursively();
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

    private State<Spec> newOrGetState(Spec spec) {
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
                public void expandRecursively(int bound) {
                    if (bound > 0) {
                        var targetsToExpand = new LinkedHashSet<State<Spec>>();

                        if (transitions == null) {
                            transitions = new Transitions<>();
                            var source = this;
                            var targetSpecs = expander.apply(spec);
                            for (Map.Entry<Action, Collection<Spec>> e : targetSpecs.entrySet()) {
                                var a = e.getKey();
                                var targets = new LinkedHashSet<State<Spec>>();
                                for (Spec targetSpec : e.getValue()) {
                                    var target = newOrGetState(targetSpec);
                                    targets.add(target);
                                    if (target.getTransitionsOrNull() == null) {
                                        targetsToExpand.add(target);
                                    }
                                }
                                source.transitions.targets.put(a, targets);
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

    public static class Transitions<Spec> {

        private Map<Action, Collection<State<Spec>>> targets = new LinkedHashMap<>();

        public Collection<Action> getActions() {
            return targets.keySet();
        }

        public Collection<State<Spec>> getTargetsOrNull(Action a) {
            if (targets.containsKey(a)) {
                return new LinkedHashSet<>(targets.get(a));
            } else {
                return null;
            }
        }

        public boolean isEmpty() {
            return size() == 0;
        }

        public int size() {
            int i = 0;
            for (Collection<State<Spec>> c : targets.values()) {
                i += c.size();
            }
            return i;
        }
    }
}
