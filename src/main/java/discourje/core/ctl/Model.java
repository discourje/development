package discourje.core.ctl;

import discourje.core.lts.Action;
import discourje.core.lts.LTS;
import discourje.core.lts.Transitions;

import java.util.*;

/**
 * The abstract model the is used to check the {@link LTS} using CTL.
 */
public class Model<Spec> {

    class DmState {
        discourje.core.lts.State<Spec> s;
        Action a;

        DmState(discourje.core.lts.State<Spec> s, Action a) {
            this.s = s;
            this.a = a;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DmState that = (DmState) o;
            return Objects.equals(s, that.s) &&
                    Objects.equals(a, that.a);
        }

        @Override
        public int hashCode() {
            return Objects.hash(s, a);
        }
    }

    private final Collection<State<Spec>> initialStates;

    private final Collection<State<Spec>> states = new LinkedHashSet<>();

    private final Map<DmState, State<Spec>> dmStateMap = new HashMap<>();

    private final Collection<Channel> channels = new HashSet<>();

    private int currentActionIndex = 0;

    private final Map<Formula, Labels> stateLabelsByFormula = new HashMap<>();

    public Model(LTS<Spec> lts) {
        lts.expandRecursively();
        lts.getInitialStates().stream()
                .sorted(Comparator.comparing(discourje.core.lts.State::getIdentifier))
                .forEach(is -> addState(is, null));
        initialStates = new ArrayList<>(states);

        for (discourje.core.lts.State<Spec> state : lts.getStates()) {
            Transitions<Spec> transitions = state.getTransitionsOrNull();
            for (Action action : transitions.getActions()) {
                for (discourje.core.lts.State<Spec> targetState : transitions.getTargetsOrNull(action)) {
                    addState(targetState, action);
                }
                if (action.getSender() != null || action.getReceiver() != null) {
                    channels.add(new Channel(action.getSender(), action.getReceiver()));
                }
            }
        }

        for (State<Spec> dmState : states) {
            Transitions<Spec> transitions = dmState.getState().getTransitionsOrNull();
            for (Action action : transitions.getActions()) {
                for (discourje.core.lts.State<Spec> state : transitions.getTargetsOrNull(action)) {
                    State<Spec> nextState = findState(state, action);
                    dmState.addNextState(nextState);
                }
            }
        }
    }

    public Model(State<Spec>[] states) {
        this.initialStates = Arrays.asList(states);
        this.states.addAll(Arrays.asList(states));
    }

    private void addState(discourje.core.lts.State<Spec> state, Action action) {
        if (!dmStateMap.containsKey(new DmState(state, action))) {
            State<Spec> newState = new State<>(state, action, currentActionIndex++);
            states.add(newState);
            dmStateMap.put(new DmState(state, action), newState);
        }
    }

    private State<Spec> findState(discourje.core.lts.State<Spec> state, Action action) {
        return dmStateMap.get(new DmState(state, action));
    }

    public Collection<State<Spec>> getInitialStates() {
        return Collections.unmodifiableCollection(initialStates);
    }

    public Collection<State<Spec>> getStates() {
        return Collections.unmodifiableCollection(states);
    }

    public Collection<Channel> getChannels() {
        return Collections.unmodifiableCollection(channels);
    }

    public boolean hasLabel(State<?> state, Formula formula) {
        return stateLabelsByFormula.get(formula).hasLabel(state);
    }

    public Labels getLabels(Formula formula) {
        return stateLabelsByFormula.get(formula);
    }

    public Labels calculateLabels(Formula formula) {
        if (stateLabelsByFormula.get(formula) == null) {
            stateLabelsByFormula.put(formula, formula.label(this));
        }
        return stateLabelsByFormula.get(formula);
    }
}
