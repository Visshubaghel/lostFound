package exceptions;

public class SimilarityBelowThresholdException extends Exception {
    public SimilarityBelowThresholdException(String message) {
        super(message);
    }
}
