package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import models.Item;
import models.Match;
import service.AIProcessor;
import service.ItemRegistry;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.json.JSONArray;

public class AppServer {
    private static final int PORT = 8080;
    private static ItemRegistry registry = new ItemRegistry();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isEmpty()) {
            port = Integer.parseInt(envPort);
        }
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/register", new RegisterItemHandler());
        server.createContext("/api/found", new FoundItemHandler());
        server.createContext("/api/items", new GetItemsHandler());
        server.createContext("/api/notifications", new GetNotificationsHandler());
        server.createContext("/api/meetup/schedule", new ScheduleMeetupHandler());
        
        server.setExecutor(null);
        System.out.println("Server started on port " + port);
        server.start();
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String path = t.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            Path filePath = Paths.get("public", path.substring(1)); // strip leading slash
            if (Files.exists(filePath)) {
                byte[] response = Files.readAllBytes(filePath);
                
                String mimeType = "text/plain";
                if (path.endsWith(".html")) mimeType = "text/html";
                else if (path.endsWith(".css")) mimeType = "text/css";
                else if (path.endsWith(".js")) mimeType = "application/javascript";

                t.getResponseHeaders().set("Content-Type", mimeType);
                t.sendResponseHeaders(200, response.length);
                OutputStream os = t.getResponseBody();
                os.write(response);
                os.close();
            } else {
                String response = "404 Not Found";
                t.sendResponseHeaders(404, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    static class RegisterItemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                try {
                    InputStream is = t.getRequestBody();
                    String body = new String(is.readAllBytes());
                    JSONObject json = new JSONObject(body);

                    String qrToken = json.getString("qrToken");
                    String imageBase64 = json.getString("imageBase64");
                    
                    String id = UUID.randomUUID().toString();
                    
                    String base64Data = imageBase64;
                    if (imageBase64.contains(",")) base64Data = imageBase64.split(",")[1];
                    AIProcessor.AIResult aiResult = AIProcessor.analyzeImageAndEmbed(base64Data);
                    
                    Item item = new Item(id, qrToken, aiResult.description, aiResult.embedding, imageBase64, false);
                    registry.registerItem(item);

                    JSONObject res = new JSONObject();
                    res.put("status", "success");
                    res.put("message", "Item registered successfully");
                    res.put("description", aiResult.description);

                    sendResponse(t, 200, res.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    JSONObject res = new JSONObject();
                    res.put("status", "error");
                    res.put("message", e.getMessage());
                    sendResponse(t, 500, res.toString());
                }
            }
        }
    }

    static class FoundItemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                try {
                    InputStream is = t.getRequestBody();
                    String body = new String(is.readAllBytes());
                    JSONObject json = new JSONObject(body);

                    String finderQr = json.getString("finderQr");
                    String imageBase64 = json.getString("imageBase64");

                    String id = UUID.randomUUID().toString();
                    
                    String base64Data = imageBase64;
                    if (imageBase64.contains(",")) base64Data = imageBase64.split(",")[1];
                    AIProcessor.AIResult aiResult = AIProcessor.analyzeImageAndEmbed(base64Data);
                    
                    Item foundItem = new Item(id, finderQr, aiResult.description, aiResult.embedding, imageBase64, true);

                    registry.processFoundItem(foundItem)
                        .thenAccept(match -> {
                            try {
                                models.Notification notif = new models.Notification(
                                    UUID.randomUUID().toString(),
                                    match.getItem().getQrToken(), 
                                    finderQr, 
                                    match.getItem().getId(), 
                                    foundItem.getId(), 
                                    "PENDING", "", "", ""
                                );
                                registry.createNotification(notif);

                                JSONObject res = new JSONObject();
                                res.put("status", "match");
                                res.put("confidence", match.getSimilarityScore());
                                res.put("ownerQr", match.getItem().getQrToken());
                                res.put("description", aiResult.description);
                                sendResponse(t, 200, res.toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        })
                        .exceptionally(ex -> {
                            try {
                                JSONObject res = new JSONObject();
                                res.put("status", "no_match");
                                res.put("message", ex.getCause().getMessage());
                                res.put("description", aiResult.description);
                                sendResponse(t, 200, res.toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return null;
                        });
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        JSONObject res = new JSONObject();
                        res.put("status", "error");
                        res.put("message", e.getMessage());
                        sendResponse(t, 500, res.toString());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
    
    static class GetItemsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("GET".equals(t.getRequestMethod())) {
                try {
                    String query = t.getRequestURI().getQuery();
                    String qrToken = "";
                    if (query != null && query.contains("qrToken=")) {
                        qrToken = query.split("qrToken=")[1].split("&")[0];
                    }
                    
                    List<Item> items = registry.getItemsByQrToken(qrToken);
                    
                    JSONArray arr = new JSONArray();
                    for (Item item : items) {
                        JSONObject obj = new JSONObject();
                        obj.put("id", item.getId());
                        obj.put("description", item.getTextDescription());
                        obj.put("isFoundItem", item.isFoundItem());
                        arr.put(obj);
                    }
                    
                    JSONObject res = new JSONObject();
                    res.put("status", "success");
                    res.put("items", arr);
                    sendResponse(t, 200, res.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        JSONObject res = new JSONObject();
                        res.put("status", "error");
                        sendResponse(t, 500, res.toString());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
    
    static class GetNotificationsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("GET".equals(t.getRequestMethod())) {
                try {
                    String query = t.getRequestURI().getQuery();
                    String qrToken = "";
                    if (query != null && query.contains("qrToken=")) {
                        qrToken = query.split("qrToken=")[1].split("&")[0];
                    }
                    
                    List<models.Notification> notifs = registry.getNotificationsForUser(qrToken);
                    
                    JSONArray arr = new JSONArray();
                    for (models.Notification n : notifs) {
                        JSONObject obj = new JSONObject();
                        obj.put("id", n.getId());
                        obj.put("status", n.getStatus());
                        obj.put("meetupTime", n.getMeetupTime());
                        obj.put("location", n.getLocation());
                        obj.put("contactPhone", n.getContactPhone());
                        
                        if (qrToken.equals(n.getOwnerQr())) {
                            obj.put("role", "OWNER");
                            Item finderItem = registry.getItemById(n.getFoundItemId());
                            if (finderItem != null) obj.put("verificationImage", finderItem.getPhotoPath());
                        } else {
                            obj.put("role", "FINDER");
                            Item ownerItem = registry.getItemById(n.getOwnerItemId());
                            if (ownerItem != null) obj.put("verificationImage", ownerItem.getPhotoPath());
                        }
                        
                        arr.put(obj);
                    }
                    
                    JSONObject res = new JSONObject();
                    res.put("status", "success");
                    res.put("notifications", arr);
                    sendResponse(t, 200, res.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        JSONObject res = new JSONObject();
                        res.put("status", "error");
                        sendResponse(t, 500, res.toString());
                    } catch (IOException ex) { }
                }
            }
        }
    }
    
    static class ScheduleMeetupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                try {
                    String body = new String(t.getRequestBody().readAllBytes());
                    JSONObject req = new JSONObject(body);
                    String notificationId = req.getString("notificationId");
                    String phone = req.getString("phone");
                    String location = req.getString("location");
                    String time = req.getString("time");
                    
                    registry.updateMeetupDetails(notificationId, phone, location, time);
                    
                    JSONObject res = new JSONObject();
                    res.put("status", "success");
                    sendResponse(t, 200, res.toString());
                } catch (Exception e) {
                    try {
                        JSONObject res = new JSONObject();
                        res.put("status", "error");
                        sendResponse(t, 500, res.toString());
                    } catch (IOException ex) { }
                }
            }
        }
    }
    
    private static void sendResponse(HttpExchange t, int statusCode, String responseStr) throws IOException {
        t.getResponseHeaders().set("Content-Type", "application/json");
        t.sendResponseHeaders(statusCode, responseStr.length());
        OutputStream os = t.getResponseBody();
        os.write(responseStr.getBytes());
        os.close();
    }
}
