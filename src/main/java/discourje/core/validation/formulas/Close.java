package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.State;
import discourje.core.validation.Model;

import java.util.Objects;

class Close implements CtlFormula {
    private final String sender;
    private final String receiver;
    private final int hash;

    Close(String sender, String receiver) {
        this.sender = sender;
        this.receiver = receiver;
        hash = Objects.hash(this.sender, this.receiver);
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            for (State<?> state : model.getStates()) {
                Action action = state.getAction();
                if (action != null &&
                        action.getType() == Action.Type.CLOSE &&
                        (sender == null || sender.equals(action.getSender())) &&
                        (receiver == null || receiver.equals(action.getReceiver()))) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public boolean isActionFormula() {
        return true;
    }

    @Override
    public String toMCRL2() {
        return "close(" +
                sender.replace('[', '(').replace(']', ')') +
                "," +
                receiver.replace('[', '(').replace(']', ')') +
                ")";
    }

    public String toString() {
        return String.format("close_%s_%s", sender, receiver);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Close that = (Close) o;
        return sender.equals(that.sender) &&
                receiver.equals(that.receiver);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
