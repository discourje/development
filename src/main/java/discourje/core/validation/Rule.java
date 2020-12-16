package discourje.core.validation;

import discourje.core.validation.formulas.CtlFormula;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class Rule {

    public Collection<String> getValidationErrors(DiscourjeModel<?> model) {
        Set<String> result = new HashSet<>(0);
        for (Channel channel : model.getChannels()) {
            CtlFormula ctlFormula = createCtlOperator(channel.getSender(), channel.getReceiver());
            ctlFormula.label(model);
            int labelIndex = model.getLabelIndex(ctlFormula);

            if (model.getInitialStates().stream().anyMatch(s -> !s.hasLabel(labelIndex))) {
                result.add(createDescription(channel.getSender(), channel.getReceiver()));
            }
        }
        return result;
    }

    public abstract String createDescription(String r1, String r2);

    public abstract CtlFormula createCtlOperator(String r1, String r2);
}
