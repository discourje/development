package discourje.core.ctl.formulas;

import discourje.core.ctl.Model;
import discourje.core.ctl.State;
import discourje.core.lts.Action;
import org.junit.jupiter.api.Test;
import static discourje.core.ctl.Formulas.send;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AndTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testAnd() {
        State<S> s1 = createState(Action.Type.SEND, "a", "a");
        State<S> s2 = createState(Action.Type.SEND, "a", "b");
        State<S> s3 = createState(Action.Type.SEND, "b", "a");
        State<S> s4 = createState(Action.Type.SEND, "b", "b");
        Model<S> model = createModel(s1, s2, s3, s4);

        And and = new And(send("a", "a"), send("a", null), send(null, "a"));
        model.calculateLabels(and);

        // verify
        assertTrue(model.hasLabel(s1, and));
        assertFalse(model.hasLabel(s2, and));
        assertFalse(model.hasLabel(s3, and));
        assertFalse(model.hasLabel(s4, and));
    }

}