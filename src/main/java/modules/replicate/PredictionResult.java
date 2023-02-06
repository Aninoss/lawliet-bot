package modules.replicate;

import java.util.List;

public class PredictionResult {

    public enum Status { STARTING, PROCESSING, SUCCEEDED, FAILED, CANCELED }


    private final double progress;
    private final Status status;
    private final List<String> outputs;
    private final String error;

    public PredictionResult(double progress, Status status, List<String> outputs, String error) {
        this.progress = progress;
        this.status = status;
        this.outputs = outputs;
        this.error = error;
    }

    public double getProgress() {
        return progress;
    }

    public Status getStatus() {
        return status;
    }

    public List<String> getOutputs() {
        return outputs;
    }

    public String getError() {
        return error;
    }

}
