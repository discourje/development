package discourje.core.lts;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public class LTSs {

    public static boolean bisimilar(LTS<?> lts1, LTS<?> lts2) {
        lts1.expandRecursively();
        lts2.expandRecursively();

        var states = new LinkedHashSet<State<?>>();
        states.addAll(lts1.getStates());
        states.addAll(lts2.getStates());

        var actions = new HashSet<Action>();
        actions.addAll(lts1.getActions());
        actions.addAll(lts2.getActions());

        var partition = new HashSet<Set<State<?>>>();
        partition.add(states);

        while (true) {
            var partition$prime = new HashSet<Set<State<?>>>();

            for (Set<State<?>> block : partition) {
                var intersection = new HashSet<State<?>>();
                var complement = new HashSet<State<?>>();
                BooleanSupplier isSplit = () -> !intersection.isEmpty() && !complement.isEmpty();

                for (Set<State<?>> splitter : partition) {
                    for (Action a : actions) {
                        intersection.clear();
                        complement.clear();

                        for (State<?> s : block) {
                            var targets = s.getTransitionsOrNull().getTargetsOrNull(a);
                            if (targets != null && splitter.containsAll(targets)) {
                                intersection.add(s);
                            } else {
                                complement.add(s);
                            }
                        }

                        if (isSplit.getAsBoolean()) break;
                    }

                    if (isSplit.getAsBoolean()) break;
                }

                if (!intersection.isEmpty()) {
                    partition$prime.add(intersection);
                }
                if (!complement.isEmpty()) {
                    partition$prime.add(complement);
                }
            }

            if (partition.equals(partition$prime)) {
                break;
            } else {
                partition = partition$prime;
            }
        }

        for (State<?> initialState1 : lts1.getInitialStates()) {
            for (State<?> initialState2 : lts2.getInitialStates()) {
                var b = false;
                for (Set<State<?>> block : partition) {
                    b = block.contains(initialState1) && block.contains(initialState2);
                    if (b) break;
                }
                if (!b) return false;
            }
        }

        return true;
    }

    public static String toAldebaran(LTS<?> lts) {
        if (lts.getInitialStates().size() != 1) {
            throw new IllegalArgumentException();
        }

        var initialState = lts.getInitialStates().iterator().next();
        if (initialState.getIdentifier() != 0) {
            throw new IllegalArgumentException();
        }

        Function<State<?>, String> stateToAldebaran = source -> {
            var b = new StringBuilder();
            var transitions = source.getTransitionsOrNull();
            for (Action a : transitions.getActions()) {
                for (State<?> target : transitions.getTargetsOrNull(a)) {
                    b.append("(");
                    b.append(source.getIdentifier());
                    b.append(",");
                    b.append("\"").append(a).append("\"");
                    b.append(",");
                    b.append(target.getIdentifier());
                    b.append(")");
                    b.append(System.lineSeparator());
                }
            }
            b.deleteCharAt(b.length() - 1);
            return b.toString();
        };

        var states = new ArrayList<>(lts.getStates());
        states.sort(Comparator.comparingInt(State::getIdentifier));

        var b = new StringBuilder();
        var n = 0;
        for (State<?> s : states) {
            var transitions = s.getTransitionsOrNull();
            n += transitions == null ? 0 : s.getTransitionsOrNull().size();
        }
        b.append("des (0,").append(n).append(",").append(states.size()).append(")");
        for (State<?> s : states) {
            var transitions = s.getTransitionsOrNull();
            if (transitions == null) {
                b.append(System.lineSeparator());
                b.append("*** state ").append(s.getIdentifier()).append(" not yet expanded ***");
            } else if (!transitions.isEmpty()) {
                b.append(System.lineSeparator());
                b.append(stateToAldebaran.apply(s));
            }
        }

        return b.toString();
    }
}
