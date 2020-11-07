package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static discourje.core.validation.operators.CtlFormulas.rcv;
import static discourje.core.validation.operators.CtlFormulas.self;
import static discourje.core.validation.operators.CtlFormulas.snd;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrTest<S> extends AbstractOperatorTest<S> {

    @Test
    public void testOr() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "a");
        DMState<S> s2 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s3 = createState(Action.Type.SEND, "b", "a");
        DMState<S> s4 = createState(Action.Type.SEND, "b", "b");
        DiscourjeModel<S> model = createModel(s1, s2, s3, s4);

        CtlOperator or = new Or(self("a"), snd("a"), rcv("a"));
        or.label(model);

        // verify
        assertTrue(s1.hasLabel(or));
        assertTrue(s2.hasLabel(or));
        assertTrue(s3.hasLabel(or));
        assertFalse(s4.hasLabel(or));
    }
}