package discourje.core.ctl.formulas;

import discourje.core.ctl.formulas.atomic.True;
import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrueTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testTrue() {
        State<S> s1 = createState(Action.Type.SYNC, "a", "b");
        Model<S> model = createModel(s1);

        True _true = True.INSTANCE;
        _true.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(_true)));
    }
}