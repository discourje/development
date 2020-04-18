package discourje.spec.lts;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class LTS<Spec> {

    private Map<Spec, State<Spec>> states = new ConcurrentHashMap<>();

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

    public String toString(Collection<State<?>> currentStates) {
        if (currentStates.isEmpty()) {
            throw new IllegalArgumentException();
        }

        var identifiers = new LinkedHashMap<State<?>, String>();
        String s = LTSs.toAldebaran(this, identifiers);

        StringBuilder b = new StringBuilder();
        b.append(" *** Current state(s) ***");
        b.append(System.lineSeparator());
        b.append(System.lineSeparator());

        for (State<?> state : currentStates) {
            if (identifiers.containsKey(state)) {
                b.append(identifiers.get(state)).append(", ");
            } else {
                throw new IllegalArgumentException();
            }
        }
        b.delete(b.length() - 2, b.length());
        b.append(System.lineSeparator());
        b.append(System.lineSeparator());

        b.append(" *** Transition(s) *** ");
        b.append(System.lineSeparator());
        b.append(s.substring(s.indexOf(System.lineSeparator())));
        return b.toString();
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

    private State<Spec> newOrGetState(Spec spec) {
        return states.computeIfAbsent(spec, k -> new State<>() {

            private AtomicReference<Transitions<Spec>> transitions = new AtomicReference<>(null);

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                State<?> state = (State<?>) o;
                return Objects.equals(k, state.getSpec());
            }

            @Override
            public int hashCode() {
                return Objects.hash(k);
            }

            @Override
            public String toString() {
                return k.toString();
            }

            @Override
            public void expandRecursively(int bound) {
                if (bound > 0 && transitions.get() == null) {
                    var expansion = new Transitions<Spec>();
                    var targetSpecs = expander.apply(k);
                    for (Map.Entry<Action, Collection<Spec>> e : targetSpecs.entrySet()) {
                        var a = e.getKey();
                        for (Spec targetSpec : e.getValue()) {
                            var target = newOrGetState(targetSpec);
                            expansion.addTarget(a, target);
                        }
                    }

                    transitions.compareAndSet(null, expansion);

                    if (bound > 1) {
                        for (State<Spec> target : transitions.get().getTargets()) {
                            target.expandRecursively(bound - 1);
                        }
                    }
                }
            }

            @Override
            public Spec getSpec() {
                return k;
            }

            @Override
            public Transitions<Spec> getTransitionsOrNull() {
                return transitions.get();
            }
        });
    }
}
