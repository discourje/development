package discourje.core.ctl.formulas;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.formulas.atomic.Send;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testNot() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2 = createState(Action.Type.SEND, "b", "a");
        Model<S> model = createModel(s1, s2);

        Not not = new Not(new Send("a", null));
        model.calculateLabels(not);

        assertFalse(model.hasLabel(s1, not));
        assertTrue(model.hasLabel(s2, not));
    }
}