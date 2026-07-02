package db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class DatabaseManager {
    private static DatabaseManager instance;
    private MongoClient mongoClient;
    private MongoDatabase database;

    private DatabaseManager() {
        String uri = System.getenv("MONGODB_URI");
        if (uri == null || uri.isBlank()) {
            throw new IllegalStateException("MONGODB_URI environment variable is not set");
        }

        String databaseName = System.getenv().getOrDefault("MONGODB_DB", "lostfound");
        mongoClient = MongoClients.create(uri);
        database = mongoClient.getDatabase(databaseName);
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}
