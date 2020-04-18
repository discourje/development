package discourje.spec.lts;

public interface State<Spec> {

    default void expand() {
        expandRecursively(1);
    }

    default void expandRecursively() {
        expandRecursively(Integer.MAX_VALUE);
    }

    void expandRecursively(int bound);

    int getIdentifier();

    Transitions<Spec> getTransitionsOrNull();
}
