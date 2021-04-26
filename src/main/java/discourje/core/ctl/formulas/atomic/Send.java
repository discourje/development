package discourje.core.ctl.formulas.atomic;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.formulas.Atomic;

import java.util.Objects;

public class Send extends Atomic {
    private final String sender;
    private final String receiver;
    private final int hash;

    public Send(String sender, String receiver) {
        this.sender = sender;
        this.receiver = receiver;
        hash = Objects.hash(this.sender, this.receiver);
    }

    @Override
    public boolean isAction() {
        return true;
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            for (State<?> state : model.getStates()) {
                Action action = state.getAction();
                if (action != null &&
                        (action.getType() == Action.Type.SYNC || action.getType() == Action.Type.SEND) &&
                        (sender == null || sender.equals(action.getSender())) &&
                        (receiver == null || receiver.equals(action.getReceiver()))) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public String toMCRL2() {
        return "send(" +
                sender.replace('[', '(').replace(']', ')') +
                "," +
                receiver.replace('[', '(').replace(']', ')') +
                ") || handshake(" +
                sender.replace('[', '(').replace(']', ')') +
                "," +
                receiver.replace('[', '(').replace(']', ')') +
                ")";
    }

    @Override
    public String toString() {
        return String.format("send(%s,%s)", sender, receiver);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Send that = (Send) o;
        return Objects.equals(sender, that.sender) &&
                Objects.equals(receiver, that.receiver);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
