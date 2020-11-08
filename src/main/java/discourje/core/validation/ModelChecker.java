package discourje.core.validation;

import discourje.core.lts.LTS;
import discourje.core.validation.operators.CtlFormula;
import discourje.core.validation.operators.CtlFormulas;
import java.util.ArrayList;
import java.util.List;

public class ModelChecker {
    private final DiscourjeModel<?> dmModel;

    public ModelChecker(LTS<?> lts) {
        this.dmModel = new DiscourjeModel<>(lts);
    }

    public List<String> checkModel() {
        List<String> result = new ArrayList<>();
        CtlFormula doNotSendAfterClose_a_b = CtlFormulas.doNotSendAfterClose("a", "b");
        CtlFormula closeChannelsOnlyOnce = CtlFormulas.closeChannelsOnlyOnce("a", "b");
        doNotSendAfterClose_a_b.label(dmModel);
        closeChannelsOnlyOnce.label(dmModel);
        if (!dmModel.getInitialStates().stream().allMatch(s -> s.hasLabel(doNotSendAfterClose_a_b))) {
            result.add(doNotSendAfterClose_a_b.getDescription());
        }
        if (!dmModel.getInitialStates().stream().allMatch(s -> s.hasLabel(closeChannelsOnlyOnce))) {
            result.add(closeChannelsOnlyOnce.getDescription());
        }
        return result;
    }
}
