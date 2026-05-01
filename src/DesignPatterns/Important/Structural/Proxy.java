package DesignPatterns.Important.Structural;

import java.util.HashMap;
import java.util.Map;

class User {
    private int id;
    private String username;
    private String email;

    public User(int id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

interface UserService {
    User getUser(int id);
    void saveUser(User user);
    void deleteUser(int id);
}

class DatabaseUserService implements UserService {
    @Override
    public User getUser(int id) {
        System.out.println("DB HIT: Fetching user " + id + " from database...");
        // simulate DB call
        return new User(id, "User_" + id, "user" + id + "@example.com");
    }

    @Override
    public void saveUser(User user) {
        System.out.println("DB: Saving user " + user.getId());
    }

    @Override
    public void deleteUser(int id) {
        System.out.println("DB: Deleting user " + id);
    }
}

class CachingUserServiceProxy implements UserService {
    private final UserService realService;               // wraps real service
    private final Map<Integer, User> cache = new HashMap<>();

    public CachingUserServiceProxy(UserService realService) {
        this.realService = realService;
    }

    @Override
    public User getUser(int id) {
        // Cache hit — return without touching DB
        if (cache.containsKey(id)) {
            System.out.println("CACHE HIT: Returning user " + id + " from cache");
            return cache.get(id);
        }

        // Cache miss — delegate to real service, then cache result
        System.out.println("CACHE MISS: Fetching user " + id);
        User user = realService.getUser(id);
        cache.put(id, user);
        return user;
    }

    @Override
    public void saveUser(User user) {
        realService.saveUser(user);
        cache.put(user.getId(), user); // update cache on write
    }

    @Override
    public void deleteUser(int id) {
        realService.deleteUser(id);
        cache.remove(id); // invalidate cache on delete
    }
}

public class Proxy {
    public static void main(String[] args) {
        UserService service = new CachingUserServiceProxy(new DatabaseUserService());

        service.getUser(101); // CACHE MISS → DB HIT
        service.getUser(101); // CACHE HIT
        service.getUser(101); // CACHE HIT
        service.getUser(202); // CACHE MISS → DB HIT
        service.deleteUser(101); // cache invalidated
        service.getUser(101); // CACHE MISS → DB HIT again

        // Output:
        // CACHE MISS: Fetching user 101
        // DB HIT: Fetching user 101 from database...
        // CACHE HIT: Returning user 101 from cache
        // CACHE HIT: Returning user 101 from cache
        // CACHE MISS: Fetching user 202
        // DB HIT: Fetching user 202 from database...
        // DB: Deleting user 101
        // CACHE MISS: Fetching user 101
        // DB HIT: Fetching user 101 from database...

    }
}
