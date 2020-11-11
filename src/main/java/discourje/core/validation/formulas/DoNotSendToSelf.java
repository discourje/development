package discourje.core.validation.formulas;

import discourje.core.validation.CtlFormula;
import discourje.core.validation.operators.CtlOperator;
import static discourje.core.validation.operators.CtlOperators.AG;
import static discourje.core.validation.operators.CtlOperators.not;
import static discourje.core.validation.operators.CtlOperators.self;

public class DoNotSendToSelf extends CtlFormula {

    @Override
    public String createDescription(String r1, String r2) {
        return String.format("A message is sent from %s to %s.", r1, r1);
    }

    @Override
    public CtlOperator createCtlOperator(String r1, String r2) {
        return AG(not(self(r1)));
    }
}
