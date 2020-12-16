package discourje.core.validation.rules;

import discourje.core.validation.Rule;
import discourje.core.validation.formulas.CtlFormula;
import static discourje.core.validation.formulas.CtlFormulas.EF;
import static discourje.core.validation.formulas.CtlFormulas.EX;
import static discourje.core.validation.formulas.CtlFormulas.and;
import static discourje.core.validation.formulas.CtlFormulas.first;
import static discourje.core.validation.formulas.CtlFormulas.implies;
import static discourje.core.validation.formulas.CtlFormulas.msg;
import static discourje.core.validation.formulas.CtlFormulas.or;
import static discourje.core.validation.formulas.CtlFormulas.rcv;

public class Causality extends Rule {

    @Override
    public String createDescription(String r1, String r2) {
        return String.format("A message is sent from %s to %s for which no cause could be found.", r1, r2);
    }

    @Override
    public CtlFormula createCtlOperator(String r1, String r2) {
        return implies(EF(msg(r1, r2)), EF(and(EX(msg(r1, r2)), or(first(), rcv(r1)))));
    }
}
