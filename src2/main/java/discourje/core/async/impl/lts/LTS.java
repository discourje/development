package discourje.core.async.impl.lts;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class LTS<Spec> {

    private Map<Spec, State<Spec>> states = new LinkedHashMap<>();

    private State<Spec> initialState;

    private Function<Spec, Map<Action, Spec>> expander;

    public LTS(Spec initialStateSpec, Function<Spec, Map<Action, Spec>> expander, boolean expandRecursively) {
        this.initialState = newOrGetState(initialStateSpec);
        this.expander = expander;
        
        if (expandRecursively) {
            this.initialState.expandRecursively();
        }
    }

    @Override
    public String toString() {
        return LTSs.toAldebaran(this);
    }

    public State<Spec> getInitialState() {
        return initialState;
    }

    public Collection<State<Spec>> getStates() {
        return states.values();
    }

    private State<Spec> newOrGetState(Spec spec) {
        var s = states.get(spec);
        //noinspection Java8MapApi
        if (s == null) {

            s = new State<>() {
                private Map<Action, State<Spec>> transitions = null;

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
                        if (transitions == null) {
                            transitions = new LinkedHashMap<>();
                            var source = this;
                            var targetIds = expander.apply(spec);
                            for (Action a : targetIds.keySet()) {
                                var targetId = targetIds.get(a);
                                var target = newOrGetState(targetId);
                                source.transitions.put(a, target);
                            }
                        }
                        for (State<Spec> target : transitions.values()) {
                            if (!target.isExpanded()) {
                                target.expandRecursively(bound - 1);
                            }
                        }
                    }
                }

                @Override
                public Spec getSpec() {
                    return spec;
                }

                @Override
                public Map<Action, State<Spec>> getTransitions() {
                    if (!isExpanded()) {
                        throw new IllegalArgumentException();
                    }

                    return new LinkedHashMap<>(transitions);
                }

                @Override
                public boolean isExpanded() {
                    return transitions != null;
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

        Map<Action, State<Spec>> getTransitions();

        boolean isExpanded();
    }
}
