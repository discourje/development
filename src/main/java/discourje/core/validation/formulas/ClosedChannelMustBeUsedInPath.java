package discourje.core.validation.formulas;

import discourje.core.validation.CtlFormula;
import discourje.core.validation.operators.CtlOperator;
import static discourje.core.validation.operators.CtlOperators.AG;
import static discourje.core.validation.operators.CtlOperators.EP;
import static discourje.core.validation.operators.CtlOperators.close;
import static discourje.core.validation.operators.CtlOperators.implies;
import static discourje.core.validation.operators.CtlOperators.msg;

public class ClosedChannelMustBeUsedInPath extends CtlFormula {

    @Override
    public String createDescription(String r1, String r2) {
        return String.format("A channel from %s to %s is closed, but this channel is never used in the path.", r1, r2);
    }

    @Override
    public CtlOperator createCtlOperator(String r1, String r2) {
        return AG(implies(close(r1, r2), EP(msg(r1, r2))));
    }
}
