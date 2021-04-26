package discourje.core.ctl;

import discourje.core.lts.Action;
import discourje.core.lts.LTS;
import discourje.core.lts.Transitions;

import java.util.*;

import org.apache.commons.math3.util.Pair;

/**
 * The abstract model the is used to check the {@link LTS} using CTL.
 */
public class Model<Spec> {

    private final Collection<State<Spec>> initialStates;

    private final Collection<State<Spec>> states = new LinkedHashSet<>();

    private final Map<Pair<discourje.core.lts.State, Action>, State<Spec>> dmStateMap = new HashMap<>();

    private final Collection<Channel> channels = new HashSet<>();

    private int currentLabelIndex = 0;

    private final Map<Formula, Integer> labelIndices = new HashMap<>();

    public Model(LTS<Spec> lts) {
        lts.expandRecursively();
        lts.getInitialStates().stream()
                .sorted(Comparator.comparing(discourje.core.lts.State::getIdentifier))
                .forEach(is -> addState(is, null));
        initialStates = new ArrayList<>(states);

        for (discourje.core.lts.State state : lts.getStates()) {
            Transitions<Spec> transitions = state.getTransitionsOrNull();
            for (Action action : transitions.getActions()) {
                for (discourje.core.lts.State targetState : transitions.getTargetsOrNull(action)) {
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
                for (discourje.core.lts.State state : transitions.getTargetsOrNull(action)) {
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

    private void addState(discourje.core.lts.State state, Action action) {
        if (!dmStateMap.containsKey(new Pair<>(state, action))) {
            State<Spec> newState = new State<>(state, action);
            states.add(newState);
            dmStateMap.put(new Pair<>(state, action), newState);
        }
    }

    private State<Spec> findState(discourje.core.lts.State state, Action action) {
        return dmStateMap.get(new Pair<>(state, action));
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

    public int setLabelledBy(Formula formula) {
        labelIndices.put(formula, currentLabelIndex);
        return currentLabelIndex++;
    }

    public boolean isLabelledBy(Formula formula) {
        return labelIndices.containsKey(formula);
    }

    public int getLabelIndex(Formula formula) {
        return labelIndices.get(formula);
    }
}
