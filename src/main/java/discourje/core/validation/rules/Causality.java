package discourje.core.validation.rules;

import discourje.core.validation.Rule;
import discourje.core.validation.formulas.CtlFormula;
import static discourje.core.validation.formulas.CtlFormulas.EF;
import static discourje.core.validation.formulas.CtlFormulas.EX;
import static discourje.core.validation.formulas.CtlFormulas.and;
import static discourje.core.validation.formulas.CtlFormulas.init;
import static discourje.core.validation.formulas.CtlFormulas.implies;
import static discourje.core.validation.formulas.CtlFormulas.send;
import static discourje.core.validation.formulas.CtlFormulas.or;
import static discourje.core.validation.formulas.CtlFormulas.receive;

public class Causality extends Rule {

    @Override
    public String createErrorDescription(String r1, String r2) {
        return String.format("A message is sent from %s to %s for which no cause could be found.", r1, r2);
    }

    @Override
    public CtlFormula createCtlFormula(String r1, String r2) {
        return implies(EF(send(r1, r2)), EF(and(EX(send(r1, r2)), or(init(), receive(null, r1)))));
    }
}
