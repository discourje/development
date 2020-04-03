package discourje.core.async.impl.lts;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class LTS<Spec> {

    private Map<Spec, State> states = new LinkedHashMap<>();

    private State initialState;

    private Function<Spec, Map<Action, Spec>> expander;

    public LTS(Spec initialStateSpec, Function<Spec, Map<Action, Spec>> expander) {
        this.initialState = newOrGetState(initialStateSpec);
        this.expander = expander;
    }

    @Override
    public String toString() {
        var shorthands = new LinkedHashMap<State, String>();
        var i = 1;
        for (State s : states.values()) {
            if (Objects.equals(initialState, s)) {
                shorthands.put(s, "0");
            } else {
                shorthands.put(s, Integer.toString(i++));
            }
        }
        return toString(shorthands);
    }

    public String toString(Map<State, String> shorthands) {
        var b = new StringBuilder();
        var n = 0;
        for (State s : states.values()) {
            n += s.transitions.size();
        }
        b.append("des (0,").append(n).append(",").append(states.size()).append(")");
        for (State s : states.values()) {
            if (!s.transitions.isEmpty()) {
                b.append(System.lineSeparator());
                b.append(s.toString(shorthands));
            }
        }
        return b.toString();
    }

    public State getInitialState() {
        return initialState;
    }

    private State newOrGetState(Spec spec) {
        var s = states.get(spec);
        if (s == null) {
            s = new State(spec);
            states.put(spec, s);
        }
        return s;
    }

    public class State {

        private Spec spec;
        private boolean isExpanded = false;
        private Map<Action, State> transitions = new LinkedHashMap<>();

        private State(Spec spec) {
            this.spec = spec;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LTS<?>.State state = (LTS<?>.State) o;
            return Objects.equals(spec, state.spec);
        }

        @Override
        public int hashCode() {
            return Objects.hash(spec);
        }

        @Override
        public String toString() {
            return toString(Collections.emptyMap());
        }

        public String toString(Map<State, String> shorthands) {
            var b = new StringBuilder();
            var source = this;
            for (Action l : transitions.keySet()) {
                var target = transitions.get(l);
                b.append("(");
                b.append(shorthands.getOrDefault(source, source.getSpec().toString()));
                b.append(",");
                b.append("\"").append(l).append("\"");
                b.append(",");
                b.append(shorthands.getOrDefault(target, target.getSpec().toString()));
                b.append(")");
                b.append(System.lineSeparator());
            }
            b.deleteCharAt(b.length() - 1);
            return b.toString();
        }

        public void expand() {
            expandRecursively(1);
        }

        public void expandRecursively() {
            expandRecursively(Integer.MAX_VALUE);
        }

        public void expandRecursively(int bound) {
            if (bound > 0) {
                if (!isExpanded) {
                    isExpanded = true;
                    var source = this;
                    var targetIds = expander.apply(spec);
                    for (Action a : targetIds.keySet()) {
                        var targetId = targetIds.get(a);
                        var target = newOrGetState(targetId);
                        source.transitions.put(a, target);
                    }
                }
                for (State target : transitions.values()) {
                    if (!target.isExpanded) {
                        target.expandRecursively(bound - 1);
                    }
                }
            }
        }

        public Spec getSpec() {
            return spec;
        }

        public Map<Action, State> getTransitions() {
            if (!isExpanded) {
                throw new IllegalArgumentException();
            }

            return new LinkedHashMap<>(transitions);
        }
    }
}
