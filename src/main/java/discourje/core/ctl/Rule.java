package discourje.core.ctl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class Rule {

    public Collection<String> getValidationErrors(Model<?> model) {
        Set<String> result = new HashSet<>(0);
        for (Channel channel : model.getChannels()) {
            Formula formula = createCtlFormula(channel.getSender(), channel.getReceiver());
            model.calculateLabels(formula);

            if (model.getInitialStates().stream().anyMatch(s -> !model.hasLabel(s, formula))) {
                result.add(createErrorDescription(channel.getSender(), channel.getReceiver()));
            }
        }
        return result;
    }

    public abstract String createErrorDescription(String r1, String r2);

    public abstract Formula createCtlFormula(String r1, String r2);
}
