package discourje.core.async.impl.lts;

import java.util.Map;

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
