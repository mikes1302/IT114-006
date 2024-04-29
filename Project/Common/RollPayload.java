package Project.Common;

public class RollPayload  extends Payload{
    private int result;

    public RollPayload() {
        // Default constructor
    }

    public RollPayload(int result) {
        this.result = result;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }
}
