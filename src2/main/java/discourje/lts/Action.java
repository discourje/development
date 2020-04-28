package discourje.lts;

import java.util.Objects;
import java.util.function.Predicate;

public class Action {

    private String name;
    private Type type;
    private Predicate<Object> predicate;
    private String sender;
    private String receiver;

    public Action(String name, Type type, Predicate<Object> predicate, String sender, String receiver) {
        this.name = name;
        this.type = type;
        this.predicate = predicate;
        this.sender = sender;
        this.receiver = receiver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Action action = (Action) o;
        return Objects.equals(name, action.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public Predicate<Object> getPredicate() {
        return predicate;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public enum Type {
        SYNC, SEND, RECEIVE, CLOSE;

        public <T> T select(T sync, T send, T receive, T close) {
            switch (this) {
                case SYNC:
                    return sync;
                case SEND:
                    return send;
                case RECEIVE:
                    return receive;
                case CLOSE:
                    return close;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }
}
