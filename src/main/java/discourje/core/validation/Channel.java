package discourje.core.validation;

public class Channel {
    private final String sender;
    private final String receiver;

    public Channel(String sender, String receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }
}
