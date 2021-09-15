package discourje.core.ctl.rules;

import discourje.core.ctl.Formula;
import discourje.core.ctl.Rule;
import static discourje.core.ctl.Formulas.EF;
import static discourje.core.ctl.Formulas.sendOrHandshake;
import static discourje.core.ctl.Formulas.close;
import static discourje.core.ctl.Formulas.implies;

public class ClosedChannelMustBeUsedInProtocol extends Rule {

    @Override
    public String createErrorDescription(String r1, String r2) {
        return String.format("A channel from %s to %s is closed, but this channel is never used in the protocol.", r1, r2);
    }

    @Override
    public Formula createCtlFormula(String r1, String r2) {
        return implies(EF(close(r1, r2)), EF(sendOrHandshake(r1, r2)));
    }
}
