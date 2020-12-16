package discourje.core.validation.formulas;

import discourje.core.validation.CtlFormula;
import discourje.core.validation.operators.CtlOperator;
import static discourje.core.validation.operators.CtlOperators.AG;
import static discourje.core.validation.operators.CtlOperators.EF;
import static discourje.core.validation.operators.CtlOperators.EX;
import static discourje.core.validation.operators.CtlOperators.close;
import static discourje.core.validation.operators.CtlOperators.implies;
import static discourje.core.validation.operators.CtlOperators.not;

public class CloseChannelsOnlyOnce extends CtlFormula {

    @Override
    public String createDescription(String r1, String r2) {
        return String.format("A channel from %s to %s is closed, but this channel has already been closed before.", r1, r2);
    }

    @Override
    public CtlOperator createCtlOperator(String r1, String r2) {
        return AG(implies(close(r1,r2), not(EX(EF(close(r1, r2))))));
    }
}
