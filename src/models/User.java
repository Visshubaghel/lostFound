package models;

public class User {
    private String qrToken;
    private String name;
    private String phone;

    public User(String qrToken, String name, String phone) {
        this.qrToken = qrToken;
        this.name = name;
        this.phone = phone;
    }

    public String getQrToken() { return qrToken; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
}
