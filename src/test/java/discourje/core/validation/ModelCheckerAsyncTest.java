package discourje.core.validation;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Var;
import discourje.core.lts.LTS;
import discourje.core.validation.formulas.Causality;
import discourje.core.validation.formulas.CloseChannelsOnlyOnce;
import discourje.core.validation.formulas.ClosedChannelMustBeUsedInPath;
import discourje.core.validation.formulas.ClosedChannelMustBeUsedInProtocol;
import discourje.core.validation.formulas.DoNotSendAfterClose;
import discourje.core.validation.formulas.DoNotSendToSelf;
import discourje.core.validation.formulas.UsedChannelsMustBeClosed;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelCheckerAsyncTest<Spec> {

    public static final String NS_VALIDATION = "discourje.core.validation.validation-tests";

    @BeforeAll
    public static void setUp() {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("discourje.core.validation.validation-tests"));
        require.invoke(Clojure.read(NS_VALIDATION));
    }

    @Test
    public void testCausalityTrivialCorrect() {
        List<String> result = getModelCheckerResult("causality-trivial-correct-async");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCausalityTrivialIncorrect() {
        List<String> result = getModelCheckerResult("causality-trivial-incorrect-async");
        assertEquals(1, result.size());
        assertTrue(result.contains(new Causality().createDescription("c", "a")));
    }

    @Test
    public void testCausalityNonTrivialCorrect() {
        List<String> result = getModelCheckerResult("causality-non-trivial-correct-async");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCausalityNonTrivialIncorrect() {
        List<String> result = getModelCheckerResult("causality-non-trivial-incorrect-async");
        assertEquals(1, result.size());
        assertTrue(result.contains(new Causality().createDescription("a", "b")));
    }

    @Test
    public void testCloseChannelsOnlyOnceTrivialCorrect() {
        List<String> result = getModelCheckerResult("close-channels-only-once-trivial-correct-async");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCloseChannelsOnlyOnceTrivialIncorrect() {
        List<String> result = getModelCheckerResult("close-channels-only-once-trivial-incorrect-async");
        assertEquals(1, result.size());
        assertTrue(result.contains(new CloseChannelsOnlyOnce().createDescription("a", "b")));
    }

    @Test
    public void testCloseChannelsOnlyOnceNonTrivialCorrect() {
        List<String> result = getModelCheckerResult("close-channels-only-once-non-trivial-correct-async");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCloseChannelsOnlyOnceNonTrivialIncorrect() {
        List<String> result = getModelCheckerResult("close-channels-only-once-non-trivial-incorrect-async");
        assertEquals(2, result.size());
        assertTrue(result.contains(new CloseChannelsOnlyOnce().createDescription("a", "b")));
        assertTrue(result.contains(new DoNotSendAfterClose().createDescription("a", "b")));
    }

    @Test
    public void testClosedChannelMustBeUsedInProtocolTrivialCorrect() {
        List<String> result = getModelCheckerResult("closed-channel-must-be-used-in-protocol-trivial-correct-async");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testClosedChannelMustBeUsedInProtocolTrivialIncorrect() {
        List<String> result = getModelCheckerResult("closed-channel-must-be-used-in-protocol-trivial-incorrect-async");
        assertEquals(2, result.size());
        assertTrue(result.contains(new ClosedChannelMustBeUsedInProtocol().createDescription("b", "a")));
        assertTrue(result.contains(new ClosedChannelMustBeUsedInPath().createDescription("b", "a")));
    }

    @Test
    public void testClosedChannelMustBeUsedInProtocolNonTrivialCorrect() {
        List<String> result = getModelCheckerResult("closed-channel-must-be-used-in-protocol-non-trivial-correct-async");
        // No ClosedChannelMustBeUsedInProtocol errors expected, but we do expect two for ClosedChannelMustBeUsedInPath
        assertEquals(2, result.size());
        assertTrue(result.contains(new ClosedChannelMustBeUsedInPath().createDescription("a", "b")));
        assertTrue(result.contains(new ClosedChannelMustBeUsedInPath().createDescription("b", "a")));
    }

    @Test
    public void testClosedChannelMustBeUsedInProtocolNonTrivialIncorrect() {
        List<String> result = getModelCheckerResult("closed-channel-must-be-used-in-protocol-non-trivial-incorrect-async");
        assertEquals(3, result.size());
        assertTrue(result.contains(new ClosedChannelMustBeUsedInProtocol().createDescription("b", "c")));
        assertTrue(result.contains(new ClosedChannelMustBeUsedInPath().createDescription("a", "b")));
        assertTrue(result.contains(new ClosedChannelMustBeUsedInPath().createDescription("b", "c")));

    }

    @Test
    public void testClosedChannelMustBeUsedInPathTrivialCorrect() {
        List<String> result = getModelCheckerResult("closed-channel-must-be-used-in-path-trivial-correct-async");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testClosedChannelMustBeUsedInPathTrivialIncorrect() {
        List<String> result = getModelCheckerResult("closed-channel-must-be-used-in-path-trivial-incorrect-async");
        assertEquals(2, result.size());
        assertTrue(result.contains(new ClosedChannelMustBeUsedInProtocol().createDescription("a", "c")));
        assertTrue(result.contains(new ClosedChannelMustBeUsedInPath().createDescription("a", "c")));
    }

    @Test
    public void testClosedChannelMustBeUsedInPathNonTrivialCorrect() {
        List<String> result = getModelCheckerResult("closed-channel-must-be-used-in-path-non-trivial-correct-async");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testClosedChannelMustBeUsedInPathNonTrivialIncorrect() {
        List<String> result = getModelCheckerResult("closed-channel-must-be-used-in-path-non-trivial-incorrect-async");
        assertEquals(1, result.size());
        assertTrue(result.contains(new ClosedChannelMustBeUsedInPath().createDescription("a", "c")));
    }

    @Test
    public void testUsedChannelsMustBeClosedTrivialCorrect() {
        List<String> result = getModelCheckerResult("used-channels-must-be-closed-trivial-correct-async");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUsedChannelsMustBeClosedTrivialIncorrect() {
        List<String> result = getModelCheckerResult("used-channels-must-be-closed-trivial-incorrect-async");
        assertTrue(result.contains(new UsedChannelsMustBeClosed().createDescription("b", "a")));
    }

    @Test
    public void testUsedChannelsMustBeClosedNonTrivialCorrect() {
        List<String> result = getModelCheckerResult("used-channels-must-be-closed-non-trivial-correct-async");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUsedChannelsMustBeClosedNonTrivialIncorrect() {
        List<String> result = getModelCheckerResult("used-channels-must-be-closed-non-trivial-incorrect-async");
        assertTrue(result.contains(new UsedChannelsMustBeClosed().createDescription("d", "a")));
    }

    @Test
    public void testDoNotSendAfterCloseTrivialCorrect() {
        List<String> result = getModelCheckerResult("do-not-send-after-close-trivial-correct-async");
        assertFalse(result.contains(new DoNotSendAfterClose().createDescription("a", "b")));
    }

    @Test
    public void testDoNotSendAfterCloseTrivialIncorrect() {
        List<String> result = getModelCheckerResult("do-not-send-after-close-trivial-incorrect-async");
        assertTrue(result.contains(new DoNotSendAfterClose().createDescription("a", "b")));
    }

    @Test
    public void testDoNotSendAfterCloseNonTrivialCorrect() {
        List<String> result = getModelCheckerResult("do-not-send-after-close-non-trivial-correct-async");
        assertFalse(result.contains(new DoNotSendAfterClose().createDescription("a", "b")));
    }

    @Test
    public void testDoNotSendAfterCloseNonTrivialIncorrect() {
        List<String> result = getModelCheckerResult("do-not-send-after-close-non-trivial-incorrect-async");
        assertTrue(result.contains(new DoNotSendAfterClose().createDescription("a", "b")));
    }

    @Test
    public void testDoNotSendToSelfTrivialCorrect() {
        List<String> result = getModelCheckerResult("do-not-send-to-self-trivial-correct-async");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDoNotSendToSelfTrivialIncorrect() {
        List<String> result = getModelCheckerResult("do-not-send-to-self-trivial-incorrect-async");
        assertTrue(result.contains(new DoNotSendToSelf().createDescription("b", "b")));
    }

    @Test
    public void testDoNotSendToSelfNonTrivialCorrect() {
        List<String> result = getModelCheckerResult("do-not-send-to-self-non-trivial-correct-async");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDoNotSendToSelfNonTrivialIncorrect() {
        List<String> result = getModelCheckerResult("do-not-send-to-self-non-trivial-incorrect-async");
        assertTrue(result.contains(new DoNotSendToSelf().createDescription("a", "b")));
    }

    private List<String> getModelCheckerResult(String name) {
        IFn var = Clojure.var(NS_VALIDATION, name);
        @SuppressWarnings("unchecked")
        LTS<Spec> lts = (LTS<Spec>) ((Var) var).get();

        ModelChecker modelChecker = new ModelChecker(lts);

        return modelChecker.checkModel();
    }
}