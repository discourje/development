package discourje.core.ctl;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Var;
import discourje.core.lts.LTS;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ExampleApplicationsTest<Spec> {

    private static IFn require;

    @BeforeAll
    public static void setUp() {
    }

    @Test
    public void testProtocolChess() {
        List<String> result = getModelCheckerResult("chess-protocol");
        System.out.println(result);
    }

    @Test
    public void testProtocolGoFish() {
        List<String> result = getModelCheckerResult("go-fish-protocol");
        System.out.println(result);
    }

    @Test
    public void testProtocolRockPaperScissors() {
        List<String> result = getModelCheckerResult("rock-paper-scissors-protocol");
        System.out.println(result);
    }

    @Test
    public void testProtocolTicTacToe() {
        List<String> result = getModelCheckerResult("tic-tac-toe-protocol");
        System.out.println(result);
    }

    protected List<String> getModelCheckerResult(String name) {
        require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("discourje.core.async"));

        long t0 = System.currentTimeMillis();
        require.invoke(Clojure.read("discourje.core.ctl.example-applications"));
        IFn var = Clojure.var("discourje.core.ctl.example-applications", name);
        @SuppressWarnings("unchecked")
        LTS<Spec> lts = (LTS<Spec>) ((Var) var).get();
        lts.expandRecursively();
        long t1 = System.currentTimeMillis();
        ModelChecker modelChecker = new ModelChecker(lts);
        long t2 = System.currentTimeMillis();
        List<String> result = modelChecker.checkModel();
        long t3 = System.currentTimeMillis();
        System.out.println("#states;LTS; Model; Labelling");
        System.out.println("" + lts.getStates().size() + ";" + (t1 - t0) / 1000.0 + ";" + (t2 - t1) / 1000.0 + ";" + (t3 - t2) / 1000.0);
        return result;
    }
}