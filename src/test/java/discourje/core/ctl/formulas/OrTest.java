package discourje.core.ctl.formulas;

import discourje.core.lts.Action;
import discourje.core.ctl.Formula;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import org.junit.jupiter.api.Test;
import static discourje.core.ctl.Formulas.receive;
import static discourje.core.ctl.Formulas.send;
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

        Formula or = new Or(send("a", "a"), send("a", null), receive(null, "a"));
        model.calculateLabels(or);

        // verify
        assertTrue(model.hasLabel(s1, or));
        assertTrue(model.hasLabel(s2, or));
        assertFalse(model.hasLabel(s3, or));
        assertFalse(model.hasLabel(s4, or));
    }
}