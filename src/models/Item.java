package models;

import java.io.Serializable;

public class Item implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String qrToken; // Owner's QR for registered items, Finder's QR for found items
    private String textDescription;
    private double[] embeddingVector;
    private String photoPath;
    private boolean isFoundItem;

    public Item(String id, String qrToken, String textDescription, double[] embeddingVector, String photoPath, boolean isFoundItem) {
        this.id = id;
        this.qrToken = qrToken;
        this.textDescription = textDescription;
        this.embeddingVector = embeddingVector;
        this.photoPath = photoPath;
        this.isFoundItem = isFoundItem;
    }

    public String getId() { return id; }
    public String getQrToken() { return qrToken; }
    public String getTextDescription() { return textDescription; }
    public double[] getEmbeddingVector() { return embeddingVector; }
    public String getPhotoPath() { return photoPath; }
    public boolean isFoundItem() { return isFoundItem; }
    
    public void setEmbeddingVector(double[] embeddingVector) {
        this.embeddingVector = embeddingVector;
    }
}
