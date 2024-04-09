package Project.Common;

public class RollPayload extends Payload {
    private int result;

    public RollPayload(PayloadType pt) {
        setPayloadType(pt);
    }

    public RollPayload() {
        // Default to hide
        this(PayloadType.ROLL);
    }

    public void setResult(int result) {
        this.result = result;
    }

    public int getResult() {
        return result;
    }
}
