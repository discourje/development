package discourje.core.validation.formulas;

import discourje.core.validation.CtlFormula;
import discourje.core.validation.operators.CtlOperator;
import static discourje.core.validation.operators.CtlOperators.EF;
import static discourje.core.validation.operators.CtlOperators.close;
import static discourje.core.validation.operators.CtlOperators.implies;
import static discourje.core.validation.operators.CtlOperators.msg;

public class ClosedChannelMustBeUsedInProtocol extends CtlFormula {

    @Override
    public String createDescription(String r1, String r2) {
        return String.format("A channel from %s to %s is closed, but this channel is never used in the protocol.", r1, r2);
    }

    @Override
    public CtlOperator createCtlOperator(String r1, String r2) {
        return implies(EF(close(r1, r2)), EF(msg(r1, r2)));
    }
}
