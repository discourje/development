package discourje.core.validation;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Var;
import discourje.core.lts.LTS;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;

public class AbstractModelCheckerTest<Spec> {
    public static final String NS_VALIDATION = "discourje.core.validation.validation-tests";

    @BeforeAll
    public static void setUp() {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("discourje.core.validation.example-applications"));
        require.invoke(Clojure.read(NS_VALIDATION));
    }

    protected List<String> getModelCheckerResult(String name) {
        return getModelChecker(name).checkModel();
    }

    private ModelChecker getModelChecker(String name) {
        return new ModelChecker(getLTS(name));
    }

    LTS<Spec> getLTS(String name) {
        IFn var = Clojure.var(NS_VALIDATION, name);
        @SuppressWarnings("unchecked")
        LTS<Spec> lts = (LTS<Spec>) ((Var) var).get();
        return lts;
    }
}
