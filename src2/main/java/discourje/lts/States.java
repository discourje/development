package discourje.lts;

import java.util.Collection;
import java.util.LinkedHashSet;

public class States {

    public static Collection<State<?>> expandThenPerform(Collection<State<?>> sources,
            Action.Type type, Object message, String sender, String receiver) {

        var targets = new LinkedHashSet<State<?>>();
        for (State<?> source : sources) {
            source.expand();
            var transitions = source.getTransitionsOrNull();
            targets.addAll(transitions.perform(type, message, sender, receiver));
        }

        return targets;
    }
}
