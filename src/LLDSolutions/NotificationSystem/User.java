package LLDSolutions.NotificationSystem;

public class User {
    private final int userid;
    private String name;
    private String email; // For Email Notification
    private String phone; // For SMS Notification
    private String deviceToken; // For Push Notification.

    public User(int userid, String name, String email, String phone, String deviceToken) {
        this.userid = userid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.deviceToken = deviceToken;
    }

    public int getUserid()    { return userid; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getDeviceToken() { return deviceToken; }
}
