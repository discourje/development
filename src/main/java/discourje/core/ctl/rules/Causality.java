package discourje.core.ctl.rules;

import discourje.core.ctl.Rule;
import discourje.core.ctl.Formula;
import static discourje.core.ctl.Formulas.EF;
import static discourje.core.ctl.Formulas.EX;
import static discourje.core.ctl.Formulas.and;
import static discourje.core.ctl.Formulas.init;
import static discourje.core.ctl.Formulas.implies;
import static discourje.core.ctl.Formulas.send;
import static discourje.core.ctl.Formulas.or;
import static discourje.core.ctl.Formulas.receive;

public class Causality extends Rule {

    @Override
    public String createErrorDescription(String r1, String r2) {
        return String.format("A message is sent from %s to %s for which no cause could be found.", r1, r2);
    }

    @Override
    public Formula createCtlFormula(String r1, String r2) {
        return implies(EF(send(r1, r2)), EF(and(EX(send(r1, r2)), or(init(), receive(null, r1)))));
    }
}
