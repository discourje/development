package discourje.core.ctl.formulas;

import discourje.core.lts.Action;
import discourje.core.ctl.Formula;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.formulas.atomic.Close;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloseTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testClose() {
        // setup
        State<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2 = createState(Action.Type.CLOSE, "b", "a");

        Model<S> model = createModel(s1, s2);

        // execute
        Formula close = new Close("a", "b");
        model.calculateLabels(close);

        // verify
        assertTrue(model.hasLabel(s1, close));
        assertFalse(model.hasLabel(s2, close));
    }

}