package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.State;
import discourje.core.validation.Model;
import org.junit.jupiter.api.Test;
import static discourje.core.validation.formulas.CtlFormulas.receive;
import static discourje.core.validation.formulas.CtlFormulas.send;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testOr() {
        State<S> s1 = createState(Action.Type.SEND, "a", "a");
        State<S> s2 = createState(Action.Type.SEND, "a", "b");
        State<S> s3 = createState(Action.Type.SEND, "b", "a");
        State<S> s4 = createState(Action.Type.SEND, "b", "b");
        Model<S> model = createModel(s1, s2, s3, s4);

        CtlFormula or = new Or(send("a", "a"), send("a", null), receive(null, "a"));
        or.label(model);

        // verify
        assertTrue(s1.hasLabel(model.getLabelIndex(or)));
        assertTrue(s2.hasLabel(model.getLabelIndex(or)));
        //assertTrue(s3.hasLabel(model.getLabelIndex(or)));
        assertFalse(s4.hasLabel(model.getLabelIndex(or)));
    }
}