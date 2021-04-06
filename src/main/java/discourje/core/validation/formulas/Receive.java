package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

import java.util.Objects;

class Receive implements CtlFormula {
    private final String sender;
    private final String receiver;
    private final int hash;

    Receive(String sender, String receiver) {
        this.sender = sender;
        this.receiver = receiver;
        hash = Objects.hash(this.sender, this.receiver);
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            for (DMState<?> state : model.getStates()) {
                Action action = state.getAction();
                if (action != null &&
                        (action.getType() == Action.Type.SYNC || action.getType() == Action.Type.RECEIVE) &&
                        (sender == null || sender.equals(action.getSender())) &&
                        (receiver == null || receiver.equals(action.getReceiver()))) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    public String toString() {
        return String.format("recv_%s_%s", sender, receiver);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Receive that = (Receive) o;
        return Objects.equals(sender, that.sender) &&
                Objects.equals(receiver, that.receiver);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
