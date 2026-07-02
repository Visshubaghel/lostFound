package models;

public class Match<T> {
    private T item;
    private double similarityScore;

    public Match(T item, double similarityScore) {
        this.item = item;
        this.similarityScore = similarityScore;
    }

    public T getItem() {
        return item;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }
}
