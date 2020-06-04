package discourje.examples.npb3.impl.CGThreads;

public class CGMessage {
    public final int OrderNum;
    public final double alpha;
    public final double beta;

    public CGMessage(int OrderNum, double alpha, double beta) {
        this.OrderNum = OrderNum;
        this.alpha = alpha;
        this.beta = beta;
    }
}
