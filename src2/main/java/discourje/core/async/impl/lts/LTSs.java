package discourje.core.async.impl.lts;

import discourje.core.async.impl.lts.LTS.State;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public class LTSs {

    public static boolean bisimilar(LTS<?> lts1, LTS<?> lts2) {

        var states = new LinkedHashSet<State<?>>();
        states.addAll(lts1.getStates());
        states.addAll(lts2.getStates());

        var actions = new HashSet<Action>();
        for (State<?> s : states) {
            actions.addAll(s.getTransitions().keySet());
        }

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
                            if (splitter.contains(s.getTransitions().get(a))) {
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

        for (Set<State<?>> block : partition) {
            if (block.contains(lts1.getInitialState())) {
                return block.contains(lts2.getInitialState());
            }
        }
        return false;
    }

    public static String toAldebaran(LTS<?> lts) {

        var identifiers = new LinkedHashMap<State<?>, String>();
        var i = 1;
        for (State<?> s : lts.getStates()) {
            if (Objects.equals(lts.getInitialState(), s)) {
                identifiers.put(s, "0");
            } else {
                identifiers.put(s, Integer.toString(i++));
            }
        }

        Function<State<?>, String> stateToAldebaran = source -> {
            var b = new StringBuilder();
            var transitions = source.getTransitions();
            for (Action l : transitions.keySet()) {
                var target = transitions.get(l);
                b.append("(");
                b.append(identifiers.getOrDefault(source, source.getSpec().toString()));
                b.append(",");
                b.append("\"").append(l).append("\"");
                b.append(",");
                b.append(identifiers.getOrDefault(target, target.getSpec().toString()));
                b.append(")");
                b.append(System.lineSeparator());
            }
            b.deleteCharAt(b.length() - 1);
            return b.toString();
        };

        var b = new StringBuilder();
        var n = 0;
        for (State<?> s : lts.getStates()) {
            n += s.getTransitions().size();
        }
        b.append("des (0,").append(n).append(",").append(lts.getStates().size()).append(")");
        for (State<?> s : lts.getStates()) {
            if (!s.getTransitions().isEmpty()) {
                b.append(System.lineSeparator());
                b.append(stateToAldebaran.apply(s));
            }
        }

        return b.toString();
    }
}
