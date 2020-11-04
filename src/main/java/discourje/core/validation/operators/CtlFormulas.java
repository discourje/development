package discourje.core.validation.operators;

public class CtlFormulas {

    public CtlFormula doNotSendAfterClose(String r1, String r2) {
        return new CtlFormula(
                "Do not send after close",
                String.format("A message is sent from %s to %s, after the channel between the two is closed.", r1, r2),
                AG(implies(close(r1, r2), AG(not(msg(r1, r2))))));
    }

    public CtlFormula causality(String r1, String r2) {
        return new CtlFormula(
                "Causality",
                String.format("A message is sent from %s to %s for which no cause could be found.", r1, r2),
                implies(EF(msg(r1, r2)), EF(and(EX(msg(r1, r2)), or(first(), rcv(r1))))));
    }

    public CtlFormula closeUsedChannels(String r1, String r2) {
        return new CtlFormula(
                "Close used channels",
                String.format("A message is sent from %s to %s, but the channel is not closed afterwards.", r1, r2),
                AG(implies(msg(r1, r2), AF(close(r1, r2)))));
    }

    public CtlFormula doNotSendToSelf(String r1) {
        return new CtlFormula(
                "Do not send to self",
                String.format("A message is sent from %s to %s.", r1, r1),
                AG(not(self(r1))));
    }

    public CtlFormula closedChannelMustBeUsedInProtocol(String r1, String r2) {
        return new CtlFormula(
                "Closed channel must be used in protocol",
                String.format("A channel from %s to %s is closed, but this channel is never used in the protocol.", r1, r2),
                implies(EF(close(r1, r2)), EF(msg(r1, r2))));
    }

    // TODO: implement this with test cases
//    public CtlOperator closedChannelMustBePresentInPath(String r1, String r2) {
//        return implies(EF(close(r1, r2)), EF(msg(r1, r2)));
//    }

    public CtlFormula closeChannelsOnlyOnce(String r1, String r2) {
        return new CtlFormula(
                "Closed channels only once",
                String.format("A channel from %s to %s is closed, but this channel has already been closed before.", r1, r2),
                AG(implies(close(r1,r2), AG(not(close(r1,r2))))));
    }

    public CtlOperator first() {
        return new First();
    }

    public CtlOperator self(String r1) {
        return new Self(r1);
    }

    public CtlOperator snd(String r1) {
        return new Send(r1);
    }

    public CtlOperator rcv(String r1) {
        return new Receive(r1);
    }

    public CtlOperator msg(String r1, String r2) {
        return new Message(r1, r2);
    }

    public CtlOperator close(String r1, String r2) {
        return new Close(r1, r2);
    }

    public CtlOperator and(CtlOperator... args) {
        return new And(args);
    }

    public CtlOperator or(CtlOperator... args) {
        return new Or(args);
    }

    public CtlOperator not(CtlOperator arg) {
        return new Not(arg);
    }

    public CtlOperator implies(CtlOperator lhs, CtlOperator rhs) {
        return or(rhs, not(lhs));
    }

    public CtlOperator AF(CtlOperator arg) {
        return AU(new True(), arg);
    }

    public CtlOperator AG(CtlOperator arg) {
        return not(AF(not(arg)));
    }

    public CtlOperator AU(CtlOperator lhs, CtlOperator rhs) {
        return new AU(lhs, rhs);
    }

    public CtlOperator EX(CtlOperator arg) {
        return EX(arg);
    }

    public CtlOperator EF(CtlOperator arg) {
        return EU(new True(), arg);
    }

    public CtlOperator EG(CtlOperator arg) {
        return not(EF(not(arg)));
    }

    public CtlOperator EU(CtlOperator lhs, CtlOperator rhs) {
        return new EU(lhs, rhs);
    }
}
