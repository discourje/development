package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static discourje.core.validation.operators.CtlOperators.rcv;
import static discourje.core.validation.operators.CtlOperators.self;
import static discourje.core.validation.operators.CtlOperators.snd;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AndTest<S> extends AbstractOperatorTest<S> {

    @Test
    public void testAnd() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "a");
        DMState<S> s2 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s3 = createState(Action.Type.SEND, "b", "a");
        DMState<S> s4 = createState(Action.Type.SEND, "b", "b");
        DiscourjeModel<S> model = createModel(s1, s2, s3, s4);

        And and = new And(self("a"), snd("a"), rcv("a"));
        and.label(model);

        // verify
        assertTrue(s1.hasLabel(and));
        assertFalse(s2.hasLabel(and));
        assertFalse(s3.hasLabel(and));
        assertFalse(s4.hasLabel(and));
    }

}