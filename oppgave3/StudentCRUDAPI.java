import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Oppgave 3: CRUD-API med UPDATE
 * 
 * En webserver som tilbyr CREATE, READ, UPDATE, DELETE-operasjoner.
 * Denne oppgaven fokuserer på UPDATE (PUT).
 * 
 * Bruk:
 *   java StudentCRUDAPI <port> <csv-fil>
 * 
 * Eksempel:
 *   java StudentCRUDAPI 8002 studenter.csv
 * 
 * Test:
 *   curl http://localhost:8002/api/students/101                    # GET
 *   curl -X PUT -H "Content-Type: application/json" \
 *        -d '{"name":"Mickey Mouse","program":"CS"}' \
 *        http://localhost:8002/api/students/101                    # PUT
 */
public class StudentCRUDAPI {
    
    private static Map<Integer, Student> students = new HashMap<>();
    private static String csvFilePath;
    
    static class Student {
        int id;
        String name;
        String program;
        
        Student(int id, String name, String program) {
            this.id = id;
            this.name = name;
            this.program = program;
        }
        
        String toJSON() {
            return String.format("{\"id\":%d,\"name\":\"%s\",\"program\":\"%s\"}", 
                id, escapeJSON(name), escapeJSON(program));
        }
        
        static Student fromJSON(String json) throws Exception {
            // Enkel JSON-parsing (ikke robust, men minimalistisk)
            Map<String, String> map = parseJSON(json);
            
            if (!map.containsKey("name") || !map.containsKey("program")) {
                throw new Exception("Missing required fields: name, program");
            }
            
            String name = map.get("name");
            String program = map.get("program");
            
            if (name.isEmpty() || program.isEmpty()) {
                throw new Exception("Fields cannot be empty");
            }
            
            return new Student(-1, name, program);
        }
        
        private static Map<String, String> parseJSON(String json) throws Exception {
            Map<String, String> map = new HashMap<>();
            json = json.trim();
            
            if (!json.startsWith("{") || !json.endsWith("}")) {
                throw new Exception("Invalid JSON format");
            }
            
            String content = json.substring(1, json.length() - 1);
            for (String pair : content.split(",")) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    String key = kv[0].trim().replaceAll("\"", "");
                    String value = kv[1].trim().replaceAll("\"", "");
                    map.put(key, value);
                }
            }
            
            return map;
        }
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Bruk: java StudentCRUDAPI <port> <csv-fil>");
            System.exit(1);
        }
        
        int port = Integer.parseInt(args[0]);
        csvFilePath = args[1];
        
        loadStudentsFromCSV(csvFilePath);
        
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/api/students", StudentCRUDAPI::handleStudentsRequest);
        server.createContext("/api/students/", StudentCRUDAPI::handleStudentRequest);
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("StudentCRUDAPI server startet på port " + port);
        System.out.println("Tilgjengelige endepunkter:");
        System.out.println("  GET    /api/students          - Hent alle studenter");
        System.out.println("  GET    /api/students/{id}     - Hent student");
        System.out.println("  POST   /api/students          - Opprett ny student");
        System.out.println("  PUT    /api/students/{id}     - Oppdater student");
        System.out.println("  DELETE /api/students/{id}     - Slett student");
    }
    
    private static void loadStudentsFromCSV(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            
            String[] parts = line.split(",");
            if (parts.length >= 3) {
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim();
                    String program = parts[2].trim();
                    students.put(id, new Student(id, name, program));
                } catch (NumberFormatException e) {
                    System.err.println("Feil ved parsing av linje: " + line);
                }
            }
        }
        System.out.println("Lastet inn " + students.size() + " studenter");
    }
    
    private static void handleStudentsRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        // Legg til CORS-headers
        //addCORSHeaders(exchange);
        if ("OPTIONS".equals(method)) {
                sendResponse(exchange, 200, "");
        } else if ("GET".equals(method)) {
            handleGetAllStudents(exchange);
        } else if ("POST".equals(method)) {
            handleCreateStudent(exchange);
        } else {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }
    
    private static void handleStudentRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        // Legg til CORS-headers
        //addCORSHeaders(exchange);

        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        
        if (parts.length < 4) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid request\"}");
            return;
        }
        
        try {
            int studentId = Integer.parseInt(parts[3]);
            
            if ("OPTIONS".equals(method)) {
                sendResponse(exchange, 200, "");
            } else if ("GET".equals(method)) {
                handleGetStudent(exchange, studentId);
            } else if ("PUT".equals(method)) {
                handleUpdateStudent(exchange, studentId);
            } else if ("DELETE".equals(method)) {
                handleDeleteStudent(exchange, studentId);
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid student ID\"}");
        }
    }
    
    // Henter ikke de nyeste data, hvis andre prosesser har oppdatert de på disken
    private static void handleGetAllStudents(HttpExchange exchange) throws IOException {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (Student student : students.values()) {
            if (!first) json.append(",");
            json.append(student.toJSON());
            first = false;
        }
        json.append("]");
        
        sendResponse(exchange, 200, json.toString());
    }
    
    private static void handleGetStudent(HttpExchange exchange, int studentId) throws IOException {
        if (students.containsKey(studentId)) {
            Student student = students.get(studentId);
            sendResponse(exchange, 200, student.toJSON());
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Student not found\"}");
        }
    }
    
    private static void handleCreateStudent(HttpExchange exchange) throws IOException {
        try {
            String body = readRequestBody(exchange);
            Student newStudent = Student.fromJSON(body);
            
            // Finn neste ID
            int newId = students.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
            newStudent.id = newId;
            
            students.put(newId, newStudent);
            saveStudentsToCSV();
            
            sendResponse(exchange, 201, newStudent.toJSON());
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    private static void handleUpdateStudent(HttpExchange exchange, int studentId) throws IOException {
        if (!students.containsKey(studentId)) {
            sendResponse(exchange, 404, "{\"error\":\"Student not found\"}");
            return;
        }
        
        try {
            String body = readRequestBody(exchange);
            Student updated = Student.fromJSON(body);
            
            Student existing = students.get(studentId);
            existing.name = updated.name;
            existing.program = updated.program;
            
            saveStudentsToCSV();
            
            sendResponse(exchange, 200, existing.toJSON());
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    private static void handleDeleteStudent(HttpExchange exchange, int studentId) throws IOException {
        if (!students.containsKey(studentId)) {
            sendResponse(exchange, 404, "{\"error\":\"Student not found\"}");
            return;
        }
        
        students.remove(studentId);
        saveStudentsToCSV();
        
        sendResponse(exchange, 204, "");
    }
    
    private static void saveStudentsToCSV() throws IOException {
        StringBuilder csv = new StringBuilder();
        for (Student student : students.values()) {
            csv.append(student.id).append(",")
               .append(student.name).append(",")
               .append(student.program).append("\n");
        }
        Files.write(Paths.get(csvFilePath), csv.toString().getBytes());
        System.out.println("Lagret " + students.size() + " studenter til CSV");
    }
    
    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }
    
    private static String escapeJSON(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }
    
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) 
            throws IOException {
       
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
      
        
        byte[] responseBytes = response.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

    private static void addCORSHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }
}
