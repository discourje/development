package discourje.core.validation;

import discourje.core.lts.Action;
import discourje.core.lts.LTS;
import discourje.core.lts.State;
import discourje.core.lts.Transitions;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

class DiscourjeModelTest<Spec> {

    /**
     * Test the creation of a DiscourjeModel from an LTS representing this:
     * ----sync-----|
     * | --recv--\  |
     * |/         v |
     * 1---send-->2<-
     * ^          |
     * |---close---
     * This should result in a DiscourjeModel like this:
     * <p>
     * ----------------> 2,sync -|
     * | --------|-----> 2,recv -|
     * | | ------|-|---> 2,send -|
     * | | |     | | |           |
     * 1,null   1,close <---------
     */
    @Test
    public void createDiscourjeModel_shouldFillFieldsCorrectly() {
        @SuppressWarnings("unchecked")
        State<Spec> _1 = mock(State.class);
        when(_1.getIdentifier()).thenReturn(1);

        @SuppressWarnings("unchecked")
        State<Spec> _2 = mock(State.class);
        when(_2.getIdentifier()).thenReturn(2);

        Action sync = new Action("sync", Action.Type.SYNC, o -> true, "a", "b");
        Action recv = new Action("recv", Action.Type.RECEIVE, o -> true, "a", "b");
        Action send = new Action("send", Action.Type.SEND, o -> true, "a", "b");
        Action close = new Action("close", Action.Type.CLOSE, o -> true, "a", "b");

        @SuppressWarnings("unchecked")
        Transitions<Spec> transitions_1 = mock(Transitions.class);
        when(transitions_1.getActions()).thenReturn(Arrays.asList(sync, send, recv));
        when(transitions_1.getTargetsOrNull(sync)).thenReturn(Collections.singletonList(_2));
        when(transitions_1.getTargetsOrNull(send)).thenReturn(Collections.singletonList(_2));
        when(transitions_1.getTargetsOrNull(recv)).thenReturn(Collections.singletonList(_2));
        when(_1.getTransitionsOrNull()).thenReturn(transitions_1);

        @SuppressWarnings("unchecked")
        Transitions<Spec> transitions_2 = mock(Transitions.class);
        when(transitions_2.getActions()).thenReturn(Collections.singletonList(close));
        when(transitions_2.getTargetsOrNull(close)).thenReturn(Collections.singletonList(_1));
        when(_2.getTransitionsOrNull()).thenReturn(transitions_2);

        @SuppressWarnings("unchecked")
        LTS<Spec> lts = mock(LTS.class);
        when(lts.getInitialStates()).thenReturn(Collections.singletonList(_1));
        when(lts.getStates()).thenReturn(Arrays.asList(_1, _2));

        // execute
        DiscourjeModel<?> model = new DiscourjeModel<>(lts);

        // verify
        // expected states
        DMState<?> _1_null = new DMState<>(_1, null);
        DMState<?> _1_close = new DMState<>(_1, close);
        DMState<?> _2_sync = new DMState<>(_2, sync);
        DMState<?> _2_recv = new DMState<>(_2, recv);
        DMState<?> _2_send = new DMState<>(_2, send);

        assertEquals(1, model.getInitialStates().size());
        assertTrue(model.getInitialStates().containsAll(Collections.singletonList(_1_null)));

        assertEquals(5, model.getStates().size());
        assertTrue(model.getStates().contains(_1_null));
        assertTrue(model.getStates().contains(_1_close));
        assertTrue(model.getStates().contains(_2_sync));
        assertTrue(model.getStates().contains(_2_send));
        assertTrue(model.getStates().contains(_2_recv));

        for (DMState<?> state : model.getStates()) {
            if (state.equals(_1_null) || state.equals(_1_close)) {
                assertEquals(3, state.getTransitions().size());
                assertTrue(state.getTransitions().contains(_2_sync));
                assertTrue(state.getTransitions().contains(_2_send));
                assertTrue(state.getTransitions().contains(_2_recv));
            } else if (state.equals(_2_sync) || state.equals(_2_send) || state.equals(_2_recv)) {
                assertEquals(1, state.getTransitions().size());
                assertTrue(state.getTransitions().contains(_1_close));
            } else {
                fail("Unexpected state " + state);
            }
        }
    }
}