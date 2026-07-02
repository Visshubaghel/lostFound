package service;

import db.DatabaseManager;
import exceptions.SimilarityBelowThresholdException;
import models.Item;
import models.Match;
import utils.CosineSimilarity;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.*;

public class ItemRegistry {
    private Map<String, Item> registeredItems = new ConcurrentHashMap<>();
    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    private DatabaseManager dbManager = DatabaseManager.getInstance();
    private MongoCollection<Document> itemsCol;
    private MongoCollection<Document> notifCol;

    public ItemRegistry() {
        itemsCol = dbManager.getDatabase().getCollection("items");
        notifCol = dbManager.getDatabase().getCollection("notifications");
        loadItemsFromDatabase();
    }

    private void loadItemsFromDatabase() {
        try (MongoCursor<Document> cursor = itemsCol.find(Filters.eq("is_found", false)).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String id = doc.getString("id");
                String qr = doc.getString("qr_token");
                String desc = doc.getString("text_description");
                String photo = doc.getString("photo_path");
                
                try {
                    double[] vector = AIProcessor.generateEmbedding(desc); 
                    Item item = new Item(id, qr, desc, vector, photo, false);
                    registeredItems.put(id, item);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void registerItem(Item item) {
        saveItemToDb(item);
        if (!item.isFoundItem()) {
            registeredItems.put(item.getId(), item);
        }
    }

    private void saveItemToDb(Item item) {
        Document doc = new Document("id", item.getId())
            .append("qr_token", item.getQrToken())
            .append("text_description", item.getTextDescription())
            .append("photo_path", item.getPhotoPath())
            .append("is_found", item.isFoundItem());
            
        itemsCol.insertOne(doc);
    }

    public List<Item> getItemsByQrToken(String qrToken) {
        List<Item> items = new ArrayList<>();
        try (MongoCursor<Document> cursor = itemsCol.find(Filters.eq("qr_token", qrToken)).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String id = doc.getString("id");
                String desc = doc.getString("text_description");
                String photo = doc.getString("photo_path");
                boolean isFound = doc.getBoolean("is_found", false);
                items.add(new Item(id, qrToken, desc, null, photo, isFound));
            }
        }
        return items;
    }

    public CompletableFuture<Match<Item>> processFoundItem(Item foundItem) {
        saveItemToDb(foundItem);
        return CompletableFuture.supplyAsync(() -> {
            PriorityQueue<Match<Item>> pq = new PriorityQueue<>(
                (m1, m2) -> Double.compare(m2.getSimilarityScore(), m1.getSimilarityScore())
            );

            for (Item regItem : registeredItems.values()) {
                double score = CosineSimilarity.calculate(regItem.getEmbeddingVector(), foundItem.getEmbeddingVector());
                pq.offer(new Match<>(regItem, score));
            }

            Match<Item> bestMatch = pq.peek();
            
            if (bestMatch != null && bestMatch.getSimilarityScore() >= 0.85) {
                return bestMatch;
            } else {
                throw new CompletionException(new SimilarityBelowThresholdException("No match found with confidence >= 85%"));
            }
        }, executorService);
    }
    
    public Item getItemById(String id) {
        if (registeredItems.containsKey(id)) return registeredItems.get(id);
        
        Document doc = itemsCol.find(Filters.eq("id", id)).first();
        if (doc != null) {
            return new Item(id, doc.getString("qr_token"), doc.getString("text_description"), null, doc.getString("photo_path"), doc.getBoolean("is_found", false));
        }
        return null;
    }
    
    public void createNotification(models.Notification notification) {
        Document doc = new Document("id", notification.getId())
            .append("owner_qr", notification.getOwnerQr())
            .append("finder_qr", notification.getFinderQr())
            .append("owner_item_id", notification.getOwnerItemId())
            .append("found_item_id", notification.getFoundItemId())
            .append("status", notification.getStatus())
            .append("meetup_time", notification.getMeetupTime())
            .append("location", notification.getLocation())
            .append("contact_phone", notification.getContactPhone());
            
        notifCol.insertOne(doc);
    }

    public List<models.Notification> getNotificationsForUser(String qrToken) {
        List<models.Notification> list = new ArrayList<>();
        
        Document query = new Document("$or", Arrays.asList(
            new Document("owner_qr", qrToken),
            new Document("finder_qr", qrToken)
        ));
        
        try (MongoCursor<Document> cursor = notifCol.find(query).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                list.add(new models.Notification(
                    doc.getString("id"), doc.getString("owner_qr"), doc.getString("finder_qr"),
                    doc.getString("owner_item_id"), doc.getString("found_item_id"), doc.getString("status"),
                    doc.getString("meetup_time"), doc.getString("location"), doc.getString("contact_phone")
                ));
            }
        }
        return list;
    }

    public void updateMeetupDetails(String notificationId, String phone, String location, String time) {
        Document update = new Document("$set", new Document("contact_phone", phone)
            .append("location", location)
            .append("meetup_time", time)
            .append("status", "VERIFIED"));
            
        notifCol.updateOne(Filters.eq("id", notificationId), update);
    }
}
