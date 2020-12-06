package discourje.core.validation;

import discourje.core.validation.operators.CtlOperator;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class CtlFormula {

    public Collection<String> getValidationErrors(DiscourjeModel<?> model) {
        Set<String> result = new HashSet<>(0);
        for (Channel channel : model.getChannels()) {
            CtlOperator ctlOperator = createCtlOperator(channel.getSender(), channel.getReceiver());
            ctlOperator.label(model);
            int labelIndex = model.getLabelIndex(ctlOperator);

            if (model.getInitialStates().stream().anyMatch(s -> !s.hasLabel(labelIndex))) {
                result.add(createDescription(channel.getSender(), channel.getReceiver()));
            }
        }
        return result;
    }

    public abstract String createDescription(String r1, String r2);

    public abstract CtlOperator createCtlOperator(String r1, String r2);
}
