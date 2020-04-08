package discourje.core.async.impl.lts;

import java.util.*;

public class Transitions<Spec> {

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private Map<String, Map<String, Set<Action>>> sends = new LinkedHashMap<>();

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private Map<String, Map<String, Set<Action>>> receives = new LinkedHashMap<>();

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private Map<String, Map<String, Set<Action>>> closes = new LinkedHashMap<>();

    private Map<Action, Set<LTS.State<Spec>>> targets = new LinkedHashMap<>();

    void addTarget(Action a, LTS.State<Spec> target) {
        {
            var set = targets.get(a);
            //noinspection Java8MapApi
            if (set == null) {
                set = new LinkedHashSet<>();
                targets.put(a, set);
            }
            set.add(target);
        }
        {
            var sender = a.getSender();
            var receiver = a.getReceiver();

            var mapMapSet = a.getType().select(sends, receives, closes);
            var mapSet = mapMapSet.get(sender);
            //noinspection Java8MapApi
            if (mapSet == null) {
                mapSet = new LinkedHashMap<>();
                mapMapSet.put(sender, mapSet);
            }

            var set = mapSet.get(receiver);
            //noinspection Java8MapApi
            if (set == null) {
                set = new LinkedHashSet<>();
                mapSet.put(receiver, set);
            }

            set.add(a);
        }
    }

    public Collection<Action> getActions() {
        return targets.keySet();
    }

    public Collection<LTS.State<Spec>> getTargetsOrNull(Action a) {
        if (targets.containsKey(a)) {
            return new LinkedHashSet<>(targets.get(a));
        } else {
            return null;
        }
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public Collection<LTS.State<Spec>> perform(Action.Type type, Object message, String sender, String receiver) {
        var mapMapSet = type.select(sends, receives, closes);

        var mapSet = mapMapSet.get(sender);
        if (mapSet == null) {
            return Collections.emptySet();
        }

        var set = mapSet.get(receiver);
        if (set == null) {
            return Collections.emptySet();
        }

        var result = new LinkedHashSet<LTS.State<Spec>>();
        for (Action a : set) {
            if (a.getPredicate().test(message)) {
                result.addAll(targets.get(a));
            }
        }

        return result;
    }

    public int size() {
        int i = 0;
        for (Collection<LTS.State<Spec>> c : targets.values()) {
            i += c.size();
        }
        return i;
    }
}
