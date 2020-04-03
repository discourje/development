package discourje.core.async.impl.lts;

import java.util.function.Predicate;

public interface Receive extends Action {

    void getSender();

    void getReceiver();

    Predicate<?> getPredicate();
}
