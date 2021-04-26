package discourje.core.ctl.formulas.atomic;

import discourje.core.lts.Action;
import discourje.core.ctl.Model;
import discourje.core.ctl.State;
import discourje.core.ctl.formulas.Atomic;

import java.util.Objects;

public class Act extends Atomic {
    private final String role;
    private final int hash;

    public Act(String role) {
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
        return String.format("act(%s)", role);
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
