package discourje.core.validation.formulas;

import discourje.core.validation.CtlFormula;
import discourje.core.validation.operators.CtlOperator;
import static discourje.core.validation.operators.CtlOperators.AG;
import static discourje.core.validation.operators.CtlOperators.close;
import static discourje.core.validation.operators.CtlOperators.implies;
import static discourje.core.validation.operators.CtlOperators.msg;
import static discourje.core.validation.operators.CtlOperators.not;

public class DoNotSendAfterClose extends CtlFormula {

    @Override
    public String createDescription(String r1, String r2) {
        return String.format("A message is sent from %s to %s, after the channel between the two is closed.", r1, r2);
    }

    @Override
    public CtlOperator createCtlOperator(String r1, String r2) {
        return AG(implies(close(r1, r2), AG(not(msg(r1, r2)))));
    }
}
