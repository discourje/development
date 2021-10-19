package discourje.core.ctl;

import java.util.BitSet;
import java.util.Collection;

public class Labels {
    private final BitSet labels;

    public Labels() {
        this.labels = new BitSet();
    }

    public boolean hasLabel(State<?> state) {
        return labels.get(state.getIndex());
    }

    public boolean setLabel(State<?> state) {
        boolean alreadySet = labels.get(state.getIndex());
        labels.set(state.getIndex());
        return !alreadySet;
    }

    public boolean allHaveLabel(Collection<State<?>> states) {
        return states.stream().allMatch(this::hasLabel);
    }

    public boolean anyHaveLabel(Collection<State<?>> states) {
        return states.stream().anyMatch(this::hasLabel);
    }
}
