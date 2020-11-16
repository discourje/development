package discourje.core.validation.formulas;

import discourje.core.validation.CtlFormula;
import discourje.core.validation.operators.CtlOperator;
import static discourje.core.validation.operators.CtlOperators.EF;
import static discourje.core.validation.operators.CtlOperators.EX;
import static discourje.core.validation.operators.CtlOperators.and;
import static discourje.core.validation.operators.CtlOperators.first;
import static discourje.core.validation.operators.CtlOperators.implies;
import static discourje.core.validation.operators.CtlOperators.msg;
import static discourje.core.validation.operators.CtlOperators.or;
import static discourje.core.validation.operators.CtlOperators.rcv;

public class Causality extends CtlFormula {

    @Override
    public String createDescription(String r1, String r2) {
        return String.format("A message is sent from %s to %s for which no cause could be found.", r1, r2);
    }

    @Override
    public CtlOperator createCtlOperator(String r1, String r2) {
        return implies(EF(msg(r1, r2)), EF(and(EX(msg(r1, r2)), or(first(), rcv(r1)))));
    }
}
