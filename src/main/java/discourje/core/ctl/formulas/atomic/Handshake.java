package discourje.core.ctl.formulas.atomic;

import discourje.core.ctl.Labels;
import discourje.core.ctl.Model;
import discourje.core.ctl.State;
import discourje.core.ctl.formulas.Atomic;
import discourje.core.lts.Action;
import java.util.Objects;

public class Handshake extends Atomic {
    private final String sender;
    private final String receiver;
    private final int hash;

    public Handshake(String sender, String receiver) {
        this.sender = sender;
        this.receiver = receiver;
        hash = Objects.hash(this.sender, this.receiver);
    }

    @Override
    public boolean isAction() {
        return true;
    }

    @Override
    public Labels label(Model<?> model) {
        Labels labels = new Labels();
        for (State<?> state : model.getStates()) {
            Action action = state.getAction();
            if (action != null && action.getType() == Action.Type.SYNC &&
                    (sender == null || sender.equals(action.getSender())) &&
                    (receiver == null || receiver.equals(action.getReceiver()))) {
                labels.setLabel(state);
            }
        }
        return labels;
    }

    @Override
    public String toMCRL2() {
        return "handshake(" +
                sender.replace('[', '(').replace(']', ')') +
                "," +
                receiver.replace('[', '(').replace(']', ')') +
                ")";
    }

    @Override
    public String toString() {
        return String.format("handshake(%s,%s)", sender, receiver);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Handshake that = (Handshake) o;
        return Objects.equals(sender, that.sender) &&
                Objects.equals(receiver, that.receiver);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
