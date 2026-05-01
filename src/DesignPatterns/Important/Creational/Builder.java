package DesignPatterns.Important.Creational;

class User {
    // All fields final — immutable after construction
    private final String name;
    private final String email;
    private final int age;
    private final String phone;
    private final String address;
    private final boolean isVerified;

    private User(Builder builder){
        this.name       = builder.name;
        this.email      = builder.email;
        this.age        = builder.age;
        this.phone      = builder.phone;
        this.address    = builder.address;
        this.isVerified = builder.isVerified;
    }

    // Getters only — no setters
    public String getName()      { return name; }
    public String getEmail()     { return email; }
    public int getAge()          { return age; }
    public String getPhone()     { return phone; }
    public String getAddress()   { return address; }
    public boolean isVerified()  { return isVerified; }

    @Override
    public String toString() {
        return "User{name=" + name + ", email=" + email +
                ", age=" + age + ", phone=" + phone + "}";
    }

    public static class Builder{
        // Required fields
        private final String name;
        private final String email;

        // Optional fields — defaults set here
        private int age         = 0;
        private String phone    = null;
        private String address  = null;
        private boolean isVerified = false;

        public Builder(String name, String email) {
            if(name == null || email == null) {
                throw new IllegalArgumentException("Name and email are required");
            }
            this.name = name;
            this.email = email;
        }

        // Each setter returns 'this' — enables method chaining
        public Builder age(int age) {
            if (age < 0) throw new IllegalArgumentException("Age cannot be negative");
            this.age = age;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder verified(boolean isVerified) {
            this.isVerified = isVerified;
            return this;
        }

        // Terminal method — constructs and returns the final object
        public User build() {
            return new User(this);
        }
    }

}

public class Builder {
    public static void main(String[] args){
        User user1 = new User.Builder("John", "john@example.com").build();

        User user2 = new User.Builder("Jane", "jane@example.com")
                .age(29)
                .phone("+91-9876543210")
                .address("Vapi, Gujarat")
                .verified(true)
                .build();

        System.out.println(user2.getName());
    }
}
