package discourje.core.validation;

import discourje.core.lts.Action;
import discourje.core.lts.LTS;
import discourje.core.lts.State;
import discourje.core.lts.Transitions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.apache.commons.math3.util.Pair;

/**
 * The abstract model the is used to check the {@link LTS} using CTL.
 */
public class DiscourjeModel<Spec> {

    private final Collection<DMState<Spec>> initialStates;

    private final Collection<DMState<Spec>> states = new ArrayList<>();

    private final Map<Pair<State<Spec>, Action>, DMState<Spec>> dmStateMap = new HashMap<>();

    private final Collection<Channel> channels = new HashSet<>();

    public DiscourjeModel(LTS<Spec> lts) {
        lts.expandRecursively();
        lts.getInitialStates().stream()
                .sorted(Comparator.comparing(State::getIdentifier))
                .forEach(is -> addState(is, null));
        initialStates = new ArrayList<>(states);

        for (State<Spec> state : lts.getStates()) {
            Transitions<Spec> transitions = state.getTransitionsOrNull();
            for (Action action : transitions.getActions()) {
                for (State<Spec> targetState : transitions.getTargetsOrNull(action)) {
                    addState(targetState, action);
                }
                if (action.getSender() != null || action.getReceiver() != null) {
                    channels.add(new Channel(action.getSender(), action.getReceiver()));
                }
            }
        }

        for (DMState<Spec> dmState : states) {
            Transitions<Spec> transitions = dmState.getState().getTransitionsOrNull();
            for (Action action : transitions.getActions()) {
                for (State<Spec> state : transitions.getTargetsOrNull(action)) {
                    DMState<Spec> nextState = findState(state, action);
                    dmState.addNextState(nextState);
                    nextState.addPreviousState(dmState);
                }
            }
        }
    }

    private void addState(State<Spec> state, Action action) {
        DMState<Spec> newState = new DMState<>(state, action);
        states.add(newState);
        dmStateMap.put(new Pair<>(state, action), newState);
    }

    private DMState<Spec> findState(State<Spec> state, Action action) {
        return dmStateMap.get(new Pair<>(state, action));
    }

    public Collection<DMState<Spec>> getInitialStates() {
        return Collections.unmodifiableCollection(initialStates);
    }

    public Collection<DMState<Spec>> getStates() {
        return Collections.unmodifiableCollection(states);
    }

    public Collection<Channel> getChannels() {
        return Collections.unmodifiableCollection(channels);
    }
}
