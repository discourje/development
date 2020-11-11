package discourje.core.validation.operators;

public class CtlOperators {
    public static CtlOperator first() {
        return new First();
    }

    public static CtlOperator self(String r1) {
        return new Self(r1);
    }

    public static CtlOperator snd(String r1) {
        return new Send(r1);
    }

    public static CtlOperator rcv(String r1) {
        return new Receive(r1);
    }

    public static CtlOperator msg(String r1, String r2) {
        return new Message(r1, r2);
    }

    public static CtlOperator close(String r1, String r2) {
        return new Close(r1, r2);
    }

    public static CtlOperator and(CtlOperator... args) {
        return new And(args);
    }

    public static CtlOperator or(CtlOperator... args) {
        return new Or(args);
    }

    public static CtlOperator not(CtlOperator arg) {
        return new Not(arg);
    }

    public static CtlOperator implies(CtlOperator lhs, CtlOperator rhs) {
        return new Implies(lhs, rhs);
    }

    public static AX AX(CtlOperator arg) {
        return new AX(arg);
    }

    public static CtlOperator AF(CtlOperator arg) {
        return new AF(arg);
    }

    public static CtlOperator AG(CtlOperator arg) {
        return new AG(arg);
    }

    public static CtlOperator AU(CtlOperator lhs, CtlOperator rhs) {
        return new AU(lhs, rhs);
    }

    public static CtlOperator EX(CtlOperator arg) {
        return new EX(arg);
    }

    public static CtlOperator EF(CtlOperator arg) {
        return new EF(arg);
    }

    public static CtlOperator EU(CtlOperator lhs, CtlOperator rhs) {
        return new EU(lhs, rhs);
    }
}
