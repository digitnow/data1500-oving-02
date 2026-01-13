import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Oppgave 2: API med Søk og SQL Injection-illustrasjon
 * 
 * En webserver som tilbyr søkefunksjonalitet med eksempel på SQL injection-sårbarhet.
 * Oppgaven illustrerer viktigheten av input-validering.
 * 
 * Bruk:
 *   java UserSearchAPI <port> <csv-fil>
 * 
 * Eksempel:
 *   java UserSearchAPI 8001 brukere.csv
 * 
 * Test:
 *   curl "http://localhost:8001/api/search?email=bruker1@epost.no"
 *   curl "http://localhost:8001/api/search?email=bruker1@epost.no' OR '1'='1"  (SQL injection-forsøk)
 *   En simulering av "injection" angrep gjennom felt for bruker-input, som ikke blir testet godt nok 
 *   Parameter til URL email kan spesifiseres på klient siden som email=bruker1@epost.no' OR '1'='1 
 *   curl "http://localhost:8001/api/search?email=bruker1@epost.no%27%20OR%20%271%27%3D%271"
 */
public class UserSearchAPI {
    
    private static Map<String, User> users = new HashMap<>();
    private static String csvFilePath;
    
    // Indre klasse for User
    static class User {
        int id;
        String email;
        String name;
        
        User(int id, String email, String name) {
            this.id = id;
            this.email = email;
            this.name = name;
        }
        
        String toJSON() {
            return String.format("{\"id\":%d,\"email\":\"%s\",\"name\":\"%s\"}", 
                id, escapeJSON(email), escapeJSON(name));
        }
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Bruk: java UserSearchAPI <port> <csv-fil>");
            System.exit(1);
        }
        
        int port = Integer.parseInt(args[0]);
        csvFilePath = args[1];
        
        // Last inn CSV-filen (fra disk til primært minne)
        loadUsersFromCSV(csvFilePath);
        
        // Opprett HTTP-server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Definer kontekster (endpoints)
        server.createContext("/api/search", UserSearchAPI::handleSearchRequest);
        server.createContext("/api/search-safe", UserSearchAPI::handleSearchSafeRequest);
        server.createContext("/api/users", UserSearchAPI::handleUsersRequest);
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("UserSearchAPI server startet på port " + port);
        System.out.println("Tilgjengelige endepunkter:");
        System.out.println("  GET /api/users                      - Hent alle brukere");
        System.out.println("  GET /api/search?email=...           - Søk etter bruker (SÅRBAR for SQL injection)");
        System.out.println("  GET /api/search-safe?email=...      - Søk etter bruker (SIKKER)");
    }
    
    private static void loadUsersFromCSV(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            
            String[] parts = line.split(",");
            if (parts.length >= 3) {
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    String email = parts[1].trim();
                    String name = parts[2].trim();
                    users.put(email, new User(id, email, name));
                } catch (NumberFormatException e) {
                    System.err.println("Feil ved parsing av linje: " + line);
                }
            }
        }
        System.out.println("Lastet inn " + users.size() + " brukere fra " + filePath);
    }
    
    private static void handleSearchRequest(HttpExchange exchange) throws IOException {
        // SÅRBAR versjon - illustrerer SQL injection-prinsippet
        String query = exchange.getRequestURI().getQuery();
        
        if (query == null || query.isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"Missing email parameter\"}");
            return;
        }
        
        // Parse query-parameter
        String email = null;
        try {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv[0].equals("email")) {
                    email = URLDecoder.decode(kv[1], "UTF-8");
                }
            }
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid query parameter\"}");
            return;
        }
        
        if (email == null || email.isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"Missing email parameter\"}");
            return;
        }
        
        // SÅRBAR: Ingen input-validering - SQL injection-prinsipp
        // Hvis email = "bruker1@epost.no' OR '1'='1", ville en SQL-database returnert alle poster
        // Med CSV-filer er det mindre kritisk, men prinsippet er det samme
        
        System.out.println("Søk etter: " + email);
        System.out.flush();
        
        // Illustrer problemet: Søk som ikke er eksakt match
        StringBuilder results = new StringBuilder("[");
        boolean first = true;
        
        for (User user : users.values()) {
            // SÅRBAR: Naiv string-matching uten escape
            if (user.email.contains(email) || email.contains("'")) {
                // Hvis email inneholder SQL-injection-tegn, kan det forårsake problemer
                if (!first) results.append(",");
                results.append(user.toJSON());
                first = false;
            }
        }
        results.append("]");
        
        sendResponse(exchange, 200, results.toString());
    }
    
    private static void handleSearchSafeRequest(HttpExchange exchange) throws IOException {
        // SIKKER versjon - med input-validering
        String query = exchange.getRequestURI().getQuery();
        
        if (query == null || query.isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"Missing email parameter\"}");
            return;
        }
        
        // Parse query-parameter
        String email = null;
        try {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv[0].equals("email")) { // input-validering
                    email = URLDecoder.decode(kv[1], "UTF-8");
                }
            }
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid query parameter\"}");
            return;
        }
        
        if (email == null || email.isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"Missing email parameter\"}");
            return;
        }
        
        // SIKKER: Validering av input
        if (!isValidEmail(email)) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid email format\"}");
            return;
        }
        
        // Eksakt søk
        if (users.containsKey(email)) {
            User user = users.get(email);
            sendResponse(exchange, 200, "[" + user.toJSON() + "]");
        } else {
            sendResponse(exchange, 200, "[]");
        }
    }
    
    private static void handleUsersRequest(HttpExchange exchange) throws IOException {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (User user : users.values()) {
            if (!first) json.append(",");
            json.append(user.toJSON());
            first = false;
        }
        json.append("]");
        
        sendResponse(exchange, 200, json.toString());
    }
    
    private static boolean isValidEmail(String email) {
        // Enkel validering - sjekk for tegn som brukes i SQL injection
        if (email.contains("'") || email.contains("\"") || email.contains(";") || 
            email.contains("--") || email.contains("/*") || email.contains("*/")) {
            return false;
        }
        
        // Sjekk for gyldig e-postformat (enkel versjon)
        return email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    }
    
    private static String escapeJSON(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) 
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
