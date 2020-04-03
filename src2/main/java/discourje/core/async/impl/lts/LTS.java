package discourje.core.async.impl.lts;

import java.util.*;
import java.util.function.Function;

public class LTS<Spec> {

    private Map<Spec, State<Spec>> states = new LinkedHashMap<>();

    private State<Spec> initialState;

    private Function<Spec, Map<Action, Spec>> expander;

    public LTS(Spec initialStateSpec, Function<Spec, Map<Action, Spec>> expander) {
        this.initialState = newOrGetState(initialStateSpec);
        this.expander = expander;
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

//    public static void areBisimilar(LTS<?> lts1, LTS<?> lts2) {
//
//
//        var alphas = new HashSet<Action>();
//
//        for (State s : lts1.states.values()) {
//            alphas.addAll(s.getAlphas());
//        }
//
//        var partition = new HashSet<Set<State>>();
//        partition.add(states);
//
//        while (true) {
//            var partition$prime = new HashSet<Set<State>>();
//
//            for (Set<State> block : partition) {
//                var intersection = new HashSet<State>();
//                var complement = new HashSet<State>();
//                BooleanSupplier isSplit = () ->
//                        !intersection.isEmpty() && !complement.isEmpty();
//
//                for (Set<State> splitter : partition) {
//                    for (Alpha a : alphas) {
//                        intersection.clear();
//                        complement.clear();
//
//                        for (State s : block) {
//                            if (s.hasSomeSuccessor(a, splitter)) {
//                                intersection.add(s);
//                            } else {
//                                complement.add(s);
//                            }
//                        }
//
//                        if (isSplit.getAsBoolean()) break;
//                    }
//
//                    if (isSplit.getAsBoolean()) break;
//                }
//
//                if (!intersection.isEmpty()) {
//                    partition$prime.add(intersection);
//                }
//                if (!complement.isEmpty()) {
//                    partition$prime.add(complement);
//                }
//            }
//
//            System.out.println(states.size() + " " + partition.size());
//
//            if (partition.equals(partition$prime)) {
//                break;
//            } else {
//                partition = partition$prime;
//            }
//        }
//
//
//        return partition;
//    }
}
