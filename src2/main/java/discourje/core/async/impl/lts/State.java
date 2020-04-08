package discourje.core.async.impl.lts;

public interface State<Spec> {

    default void expand() {
        expandRecursively(1);
    }

    default void expandRecursively() {
        expandRecursively(Integer.MAX_VALUE);
    }

    void expandRecursively(int bound);

    Spec getSpec();

    Transitions<Spec> getTransitionsOrNull();
}
