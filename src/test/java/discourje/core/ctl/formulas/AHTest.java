package discourje.core.ctl.formulas;

import discourje.core.ctl.Model;
import discourje.core.ctl.State;
import discourje.core.ctl.formulas.temporal.AH;
import discourje.core.lts.Action;
import org.junit.jupiter.api.Test;
import static discourje.core.ctl.Formulas.close;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AHTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testValidOnAllPathsEarlySplit() {
        State<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2b = createState(Action.Type.CLOSE, "a", "b");
        State<S> s3a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s3b = createState(Action.Type.CLOSE, "a", "b");

        s2a.addNextState(s1);
        s2b.addNextState(s1);
        s3a.addNextState(s2a);
        s3b.addNextState(s2b);

        Model<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        AH ah = new AH(close("a", "b"));
        model.calculateLabels(ah);

        assertTrue(model.hasLabel(s1, ah));
    }

    @Test
    public void testValidOnAllPathsLateSplit() {
        State<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s3a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s3b = createState(Action.Type.CLOSE, "a", "b");

        s2.addNextState(s1);
        s3a.addNextState(s2);
        s3b.addNextState(s2);

        Model<S> model = createModel(s1, s2, s3a, s3b);

        AH ah = new AH(close("a", "b"));
        model.calculateLabels(ah);

        assertTrue(model.hasLabel(s1, ah));
    }

    @Test
    public void testNotValidInNextState() {
        State<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2 = createState(Action.Type.SEND, "a", "b");
        State<S> s3 = createState(Action.Type.CLOSE, "a", "b");

        s2.addNextState(s1);
        s3.addNextState(s2);

        Model<S> model = createModel(s1, s2, s3);

        AH ah = new AH(close("a", "b"));
        model.calculateLabels(ah);

        assertFalse(model.hasLabel(s1, ah));
    }

    @Test
    public void testValidOnOnePath() {
        State<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2b = createState(Action.Type.CLOSE, "a", "b");
        State<S> s3a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s3b = createState(Action.Type.SEND, "a", "c");

        s2a.addNextState(s1);
        s2b.addNextState(s1);
        s3a.addNextState(s2a);
        s3b.addNextState(s2b);

        Model<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        AH ah = new AH(close("a", "b"));
        model.calculateLabels(ah);

        assertFalse(model.hasLabel(s1, ah));
    }

    @Test
    public void testValidOnNoPath1() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2a = createState(Action.Type.SEND, "a", "b");
        State<S> s2b = createState(Action.Type.SEND, "a", "b");
        State<S> s3a = createState(Action.Type.CLOSE, "a", "c");
        State<S> s3b = createState(Action.Type.CLOSE, "a", "c");

        s2a.addNextState(s1);
        s2b.addNextState(s1);
        s3a.addNextState(s2a);
        s3b.addNextState(s2b);

        Model<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        AH ah = new AH(close("a", "b"));
        model.calculateLabels(ah);

        assertFalse(model.hasLabel(s1, ah));
    }

    @Test
    public void testValidOnNoPath2() {
        State<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2b = createState(Action.Type.CLOSE, "a", "b");
        State<S> s3a = createState(Action.Type.SEND, "a", "c");
        State<S> s3b = createState(Action.Type.SEND, "a", "c");

        s2a.addNextState(s1);
        s2b.addNextState(s1);
        s3a.addNextState(s2a);
        s3b.addNextState(s2b);

        Model<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        AH ah = new AH(close("a", "b"));
        model.calculateLabels(ah);

        assertFalse(model.hasLabel(s1, ah));
    }
}