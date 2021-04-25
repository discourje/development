package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.Model;
import discourje.core.validation.State;

import java.util.Objects;

class Act implements CtlFormula {
    private final String role;
    private final int hash;

    Act(String role) {
        this.role = role;
        hash = Objects.hash(this.role);
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            for (State<?> state : model.getStates()) {
                Action action = state.getAction();
                if (action != null) {
                    switch (action.getType()) {
                        case SYNC:
                            if (role.equals((action.getSender())) || role.equals(action.getReceiver())) {
                                state.addLabel(labelIndex);
                            }
                            break;
                        case CLOSE:
                        case SEND:
                            if (role.equals((action.getSender()))) {
                                state.addLabel(labelIndex);
                            }
                            break;
                        case RECEIVE:
                            if (role.equals(action.getReceiver())) {
                                state.addLabel(labelIndex);
                            }
                            break;
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return String.format("act_%s", role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Act that = (Act) o;
        return Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
