package discourje.core.validation.operators;

public class CtlOperators {
    /**
     * Label each state that is the first state in the LTS.
     */
    public static CtlOperator first() {
        return new First();
    }

    /**
     * Label each state that is reached by sending and receiving to the same role r1.
     */
    public static CtlOperator self(String r1) {
        return new Self(r1);
    }

    /**
     * Label each state where the last action is a send by r1.
     */
    public static CtlOperator snd(String r1) {
        return new Send(r1);
    }

    /**
     * Label each state where the last action is a receive by r1.
     */
    public static CtlOperator rcv(String r1) {
        return new Receive(r1);
    }

    /**
     * Label each state where the last action is a send by r1 and a receive by r2.
     */
    public static CtlOperator msg(String r1, String r2) {
        return new Message(r1, r2);
    }

    /**
     * Label each state where the last action is a close on the channel between r1 and r2..
     */
    public static CtlOperator close(String r1, String r2) {
        return new Close(r1, r2);
    }

    /**
     * Label each state that is labelled by all the arguments.
     */
    public static CtlOperator and(CtlOperator... args) {
        return new And(args);
    }

    /**
     * Label each state that is labelled by at least one of the arguments.
     */
    public static CtlOperator or(CtlOperator... args) {
        return new Or(args);
    }

    /**
     * Label each state that is not labelled by the argument.
     */
    public static CtlOperator not(CtlOperator arg) {
        return new Not(arg);
    }

    /**
     * Label each state that is either labelled by rhs,
     * or not labelled by lhs.
     */
    public static CtlOperator implies(CtlOperator lhs, CtlOperator rhs) {
        return new Implies(lhs, rhs);
    }

    /**
     * Label each state where all directly following states are labelled by arg.
     * Such a state must exist.
     */
    public static AX AX(CtlOperator arg) {
        return new AX(arg);
    }

    /**
     * Label each state where at all paths a state appears that is labelled by arg.
     */
    public static CtlOperator AF(CtlOperator arg) {
        return new AF(arg);
    }

    /**
     * Label each state where all following states on all paths are labelled by arg.
     */
    public static CtlOperator AG(CtlOperator arg) {
        return new AG(arg);
    }

    /**
     * Label each state where all next states are labelled by lhs, until a state is labelled by rhs.
     * A state labelled by rhs must occur in each path.
     */
    public static CtlOperator AU(CtlOperator lhs, CtlOperator rhs) {
        return new AU(lhs, rhs);
    }

    /**
     * Label each state where at least one directly following state is labelled by arg.
     */
    public static CtlOperator EX(CtlOperator arg) {
        return new EX(arg);
    }

    /**
     * Label each state where for at least one path a state appears that is labelled by arg.
     */
    public static CtlOperator EF(CtlOperator arg) {
        return new EF(arg);
    }

    /**
     * Label each state where all at least one path exists where all following states are labelled by arg.
     */
    public static CtlOperator EG(CtlOperator arg) {
        return new EG(arg);
    }

    /**
     * Label each state where a path exists where all next states are labelled by lhs, until a state is labelled by rhs.
     * A state labelled by rhs must occur in this path.
     */
    public static CtlOperator EU(CtlOperator lhs, CtlOperator rhs) {
        return new EU(lhs, rhs);
    }

    /**
     * Label each state where all directly preceding states are labelled by arg.
     * Such a state must exist.
     */
    public static CtlOperator AY(CtlOperator arg) {
        return new AY(arg);
    }

    /**
     * Label each state where at all paths a previous state exists that is labelled by arg.
     */
    public static CtlOperator AP(CtlOperator arg) {
        return new AP(arg);
    }

    /**
     * Label each state where all previous states on all paths are labelled by arg.
     */
    public static CtlOperator AH(CtlOperator arg) {
        return new AH(arg);
    }

    /**
     * Label each state where all previous states are labelled by lhs, until a state that is labelled by rhs.
     * A state labelled by rhs must occur in each path.
     */
    public static CtlOperator AS(CtlOperator lhs, CtlOperator rhs) {
        return new AS(lhs, rhs);
    }

    /**
     * Label each state where at least one directly preceding state is labelled by arg.
     */
    public static CtlOperator EY(CtlOperator arg) {
        return new EY(arg);
    }

    /**
     * Label each state where for at least one path a previous state exists that is labelled by arg.
     */
    public static CtlOperator EP(CtlOperator arg) {
        return new EP(arg);
    }

    /**
     * Label each state where at least one path exists where all previous states are labelled by arg.
     */
    public static CtlOperator EH(CtlOperator arg) {
        return new EH(arg);
    }

    /**
     * Label each state where a path exists where all previous states are labelled by lhs, since a state that is labelled by rhs.
     * A state labelled by rhs must occur in this path.
     */
    public static CtlOperator ES(CtlOperator lhs, CtlOperator rhs) {
        return new ES(lhs, rhs);
    }
}
