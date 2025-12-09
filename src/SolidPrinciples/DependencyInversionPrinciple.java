package SolidPrinciples;

/*
* High Level Modules: Part of the application that contains core logic - the brains of the
* application. They make big decisions and coordinates how different features work together.
*
* Low Level Modules: The one that handles the details, like taking to the database, reading
* files, and making api calls.
*
* Now the Definition:
* => High Level modules should not depend on low level modules, Both should depend on
* abstractions(interfaces).
* => Abstractions should not depend on details. Concrete Implementations should depend on
* abstractions.
*
* Simple Version:
* Depend on interfaces not on concrete classes.
* */

// BAD EXAMPLE
/*
* UserService (high-level) depends directly on MySQLDatabase (low-level concrete class)
* Can’t easily switch to PostgresSQL, MongoDB, or even an in-memory test database
* Violates both rules of DIP
* */
class MySQLDatabase{
    public void save(String data){
        System.out.println("Saving data to MYSQL: "+ data);
    }
}

class UserService{
    private MySQLDatabase sql = new MySQLDatabase();

    public void registerUser(String username){
        String data = "User: " + username;
        sql.save(data);
    }
}

// GOOD EXAMPLE
// 1. Abstraction (belongs to the high-level module!)
interface UserRepository{
    void save(String data);
}

// 2. Low-level concrete implementations depend on the abstraction
class MySQLDatabaseGood implements UserRepository {
    @Override
    public void save(String data) {
        System.out.println("Saving to MySQL: " + data);
    }
}

class MongoDatabase implements UserRepository {
    @Override
    public void save(String data) {
        System.out.println("Saving to MongoDB: " + data);
    }
}

class UserServiceGood{
    private final UserRepository repository;

    public UserServiceGood(UserRepository repository){
        this.repository = repository;
    }

    public void registerUser(String username){
        String data = "User: " + username;
        repository.save(data);
    }

}

public class DependencyInversionPrinciple {
    public static void main(String[] args){

        UserRepository ur = new MongoDatabase();

        UserServiceGood usd = new UserServiceGood(ur);
        usd.registerUser("admin");
    }
}
