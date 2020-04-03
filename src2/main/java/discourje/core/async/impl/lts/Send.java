package discourje.core.async.impl.lts;

import java.util.function.Predicate;

public interface Send extends Action {

    void getSender();

    void getReceiver();

    Predicate<?> getPredicate();
}
