package discourje.core.async.impl.lts;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Function;

public class LTSs {

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
