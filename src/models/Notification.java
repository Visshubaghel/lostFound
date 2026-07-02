package models;

public class Notification {
    private String id;
    private String ownerQr;
    private String finderQr;
    private String ownerItemId;
    private String foundItemId;
    private String status;
    private String meetupTime;
    private String location;
    private String contactPhone;

    public Notification(String id, String ownerQr, String finderQr, String ownerItemId, String foundItemId, String status, String meetupTime, String location, String contactPhone) {
        this.id = id;
        this.ownerQr = ownerQr;
        this.finderQr = finderQr;
        this.ownerItemId = ownerItemId;
        this.foundItemId = foundItemId;
        this.status = status;
        this.meetupTime = meetupTime;
        this.location = location;
        this.contactPhone = contactPhone;
    }

    public String getId() { return id; }
    public String getOwnerQr() { return ownerQr; }
    public String getFinderQr() { return finderQr; }
    public String getOwnerItemId() { return ownerItemId; }
    public String getFoundItemId() { return foundItemId; }
    public String getStatus() { return status; }
    public String getMeetupTime() { return meetupTime; }
    public String getLocation() { return location; }
    public String getContactPhone() { return contactPhone; }
}
