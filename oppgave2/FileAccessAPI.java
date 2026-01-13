import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Oppgave 2 (Ny): API med Path Traversal og Simulert SQL Injection
 * 
 * Denne oppgaven demonstrerer to kritiske sikkerhetssårbarheter:
 * 1. Path Traversal: Lar angriperen lese filer utenfor den tiltenkte mappen.
 * 2. Simulert SQL Injection: Viser hvordan manglende input-validering kan misbrukes i søk.
 * 
 * Bruk:
 *   java FileAccessAPI <port> <data-mappe>
 * 
 * Eksempel:
 *   java FileAccessAPI 8002 ../testdata/data
 */
public class FileAccessAPI {
    
    private static String dataDirectory;
    
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Bruk: java FileAccessAPI <port> <data-mappe>");
            System.exit(1);
        }
        
        int port = Integer.parseInt(args[0]);
        dataDirectory = args[1];
        
        // Opprett HTTP-server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Endepunkter
        server.createContext("/api/files", FileAccessAPI::handleFileRequest);       // Sårbar for Path Traversal
        server.createContext("/api/search", FileAccessAPI::handleSearchRequest);    // Sårbar for "SQL Injection"
        server.createContext("/health", FileAccessAPI::handleHealthCheck);
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("FileAccessAPI server startet på port " + port);
        System.out.println("Data-mappe: " + dataDirectory);
        System.out.println("Endepunkter:");
        System.out.println("  GET /api/files?filename=...  (SÅRBAR: Path Traversal)");
        System.out.println("  GET /api/search?query=...    (SÅRBAR: Simulert SQLi)");
    }
    
    /**
     * Håndterer fil-lesing. SÅRBAR for Path Traversal.
     * Angriper kan bruke "../" for å gå ut av data-mappen.
     */
    private static void handleFileRequest(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String filename = getQueryParam(query, "filename");
        
        if (filename == null) {
            sendResponse(exchange, 400, "{\"error\":\"Missing filename parameter\"}");
            return;
        }
        
        // SÅRBARHET: Ingen sjekk av om filnavnet inneholder ".." eller "/"
        // Vi slår bare sammen data-mappen med filnavnet.
        Path filePath = Paths.get(dataDirectory, filename);
        
        System.out.println("Forsøker å lese fil: " + filePath.toString());
        
        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            try {
                String content = Files.readString(filePath);
                // Returner innholdet som JSON (enkelt pakket inn)
                String jsonContent = escapeJSON(content);
                sendResponse(exchange, 200, "{\"filename\":\"" + filename + "\", \"content\":\"" + jsonContent + "\"}");
            } catch (IOException e) {
                sendResponse(exchange, 500, "{\"error\":\"Could not read file\"}");
            }
        } else {
            sendResponse(exchange, 404, "{\"error\":\"File not found\"}");
        }
    }
    
    /**
     * Håndterer søk i brukere.csv. SÅRBAR for simulert SQL Injection.
     */
    private static void handleSearchRequest(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String searchQuery = getQueryParam(query, "query");
        System.out.println("searchQuery=" + searchQuery);
        if (searchQuery == null) {
            sendResponse(exchange, 400, "{\"error\":\"Missing query parameter\"}");
            return;
        }
        
        // Last inn brukere (forenklet/hardkodet for demo)
        Path userPath = Paths.get(dataDirectory, "brukere.csv");
        List<String> lines = Files.readAllLines(userPath);
        List<String> results = new ArrayList<>();
        
        // SÅRBARHET: Simulert SQL Injection logikk
        // Hvis input inneholder "' OR '1'='1", returnerer vi alt.
        boolean injectionSuccess = searchQuery.contains("' OR '1'='1");
        
        System.out.println("injectionSuccess=" + injectionSuccess);
        
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            // Enkel CSV parsing: id,email,navn
            String[] parts = line.split(",");
            if (parts.length < 3) continue;
            
            String email = parts[1];
            
            // SÅRBAR LOGIKK:
            // Normalt søk: email må inneholde søkestrengen
            // Injection: Hvis injectionSuccess er true, matcher vi ALT.
            if (injectionSuccess || email.contains(searchQuery)) {
                results.add(String.format("{\"id\":%s,\"email\":\"%s\",\"name\":\"%s\"}", 
                    parts[0], escapeJSON(parts[1]), escapeJSON(parts[2])));
            }
        }
        
        String jsonResponse = "[" + String.join(",", results) + "]";
        sendResponse(exchange, 200, jsonResponse);
    }
    
    private static void handleHealthCheck(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 200, "{\"status\":\"OK\"}");
    }
    
    // Hjelpemetoder
    
    private static String getQueryParam(String query, String paramName) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length > 1 && kv[0].equals(paramName)) {
                try {
                    return URLDecoder.decode(kv[1], "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    private static String escapeJSON(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
