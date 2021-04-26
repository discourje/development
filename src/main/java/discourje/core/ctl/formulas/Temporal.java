package discourje.core.ctl.formulas;

import discourje.core.ctl.Formula;

public abstract class Temporal implements Formula {

    @Override
    public final boolean isTemporal() {
        return false;
    }
}
