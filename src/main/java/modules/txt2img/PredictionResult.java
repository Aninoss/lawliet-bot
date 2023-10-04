package modules.txt2img;

import java.util.Collections;
import java.util.List;

public class PredictionResult {

    public enum Status { IN_QUEUE, IN_PROGRESS, COMPLETED, FAILED }

    public enum Error { GENERAL, NSFW }


    private final double progress;
    private final Status status;
    private List<String> outputs;
    private final Error error;

    public PredictionResult(double progress, Status status, List<String> outputs, Error error) {
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

    public void setOutputs(List<String> outputs) {
        this.outputs = outputs;
    }

    public Error getError() {
        return error;
    }

    public static PredictionResult failed(Error error) {
        return new PredictionResult(
                1,
                PredictionResult.Status.FAILED,
                Collections.emptyList(),
                error
        );
    }

}
