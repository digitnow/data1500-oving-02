import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Oppgave 4: CRUD-API med DELETE og Quiz-analyse
 * 
 * En webserver som tilbyr:
 * - DELETE-operasjon for å fjerne studenter
 * - Analyse-endepunkt som beregner statistikk fra quiz-resultater
 * 
 * Bruk:
 *   java QuizAnalyticsAPI <port> <students-csv> <quiz-results-csv>
 * 
 * Eksempel:
 *   java QuizAnalyticsAPI 8003 studenter.csv quiz-res.csv
 * 
 * Test:
 *   curl http://localhost:8003/api/analytics/quiz-stats
 *   curl http://localhost:8003/api/analytics/student-stats/101
 *   curl -X DELETE http://localhost:8003/api/students/101
 */
public class QuizAnalyticsAPI {
    
    private static Map<Integer, Student> students = new HashMap<>();
    private static List<QuizResult> quizResults = new ArrayList<>();
    private static String studentsCsvPath;
    private static String quizCsvPath;
    
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
    }
    
    static class QuizResult {
        int quizId;
        int studentId;
        int score;
        int maxScore;
        
        QuizResult(int quizId, int studentId, int score, int maxScore) {
            this.quizId = quizId;
            this.studentId = studentId;
            this.score = score;
            this.maxScore = maxScore;
        }
        
        double getPercentage() {
            return (double) score / maxScore * 100;
        }
    }
    
    static class QuizStats {
        int quizId;
        double averageScore;
        double standardDeviation;
        int minScore;
        int maxScore;
        int participantCount;
        
        String toJSON() {
            return String.format(
                "{\"quiz_id\":%d,\"average_score\":%.2f,\"std_dev\":%.2f," +
                "\"min_score\":%d,\"max_score\":%d,\"participants\":%d}",
                quizId, averageScore, standardDeviation, minScore, maxScore, participantCount
            );
        }
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Bruk: java QuizAnalyticsAPI <port> <students-csv> <quiz-results-csv>");
            System.exit(1);
        }
        
        int port = Integer.parseInt(args[0]);
        studentsCsvPath = args[1];
        quizCsvPath = args[2];
        
        loadStudentsFromCSV(studentsCsvPath);
        loadQuizResultsFromCSV(quizCsvPath);
        
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/api/students", QuizAnalyticsAPI::handleStudentsRequest);
        server.createContext("/api/students/", QuizAnalyticsAPI::handleStudentRequest);
        server.createContext("/api/analytics/quiz-stats", QuizAnalyticsAPI::handleQuizStatsRequest);
        server.createContext("/api/analytics/student-stats/", QuizAnalyticsAPI::handleStudentStatsRequest);
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("QuizAnalyticsAPI server startet på port " + port);
        System.out.println("Tilgjengelige endepunkter:");
        System.out.println("  GET    /api/students                      - Hent alle studenter");
        System.out.println("  GET    /api/students/{id}                 - Hent student");
        System.out.println("  DELETE /api/students/{id}                 - Slett student");
        System.out.println("  GET    /api/analytics/quiz-stats           - Hent quiz-statistikk");
        System.out.println("  GET    /api/analytics/student-stats/{id}   - Hent studentstatistikk");
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
    
    private static void loadQuizResultsFromCSV(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        boolean firstLine = true;
        for (String line : lines) {
            if (firstLine) {
                firstLine = false;
                continue; // Skip header
            }
            if (line.trim().isEmpty()) continue;
            
            String[] parts = line.split(",");
            if (parts.length >= 4) {
                try {
                    int quizId = Integer.parseInt(parts[0].trim());
                    int studentId = Integer.parseInt(parts[1].trim());
                    int score = Integer.parseInt(parts[2].trim());
                    int maxScore = Integer.parseInt(parts[3].trim());
                    quizResults.add(new QuizResult(quizId, studentId, score, maxScore));
                } catch (NumberFormatException e) {
                    System.err.println("Feil ved parsing av linje: " + line);
                }
            }
        }
        System.out.println("Lastet inn " + quizResults.size() + " quiz-resultater");
    }
    
    private static void handleStudentsRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        
        if ("GET".equals(method)) {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Student student : students.values()) {
                if (!first) json.append(",");
                json.append(student.toJSON());
                first = false;
            }
            json.append("]");
            sendResponse(exchange, 200, json.toString());
        } else {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }
    
    private static void handleStudentRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        
        if (parts.length < 4) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid request\"}");
            return;
        }
        
        try {
            int studentId = Integer.parseInt(parts[3]);
            
            if ("GET".equals(method)) {
                if (students.containsKey(studentId)) {
                    sendResponse(exchange, 200, students.get(studentId).toJSON());
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Student not found\"}");
                }
            } else if ("DELETE".equals(method)) {
                if (students.containsKey(studentId)) {
                    students.remove(studentId);
                    quizResults.removeIf(r -> r.studentId == studentId);
                    saveStudentsToCSV();
                    saveQuizResultsToCSV();
                    sendResponse(exchange, 204, ""); // generere advarsel siden 204 skal ikke ha respons
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Student not found\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid student ID\"}");
        }
    }
    
    private static void handleQuizStatsRequest(HttpExchange exchange) throws IOException {
        // Beregn statistikk for hver quiz
        Map<Integer, List<QuizResult>> byQuiz = quizResults.stream()
            .collect(Collectors.groupingBy(r -> r.quizId));
        
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        
        for (int quizId : new TreeSet<>(byQuiz.keySet())) {
            List<QuizResult> results = byQuiz.get(quizId);
            QuizStats stats = calculateQuizStats(quizId, results);
            
            if (!first) json.append(",");
            json.append(stats.toJSON());
            first = false;
        }
        
        json.append("]");
        sendResponse(exchange, 200, json.toString());
    }
    
    private static void handleStudentStatsRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        
        if (parts.length < 5) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid request\"}");
            return;
        }
        
        try {
            int studentId = Integer.parseInt(parts[4]);
            
            List<QuizResult> studentResults = quizResults.stream()
                .filter(r -> r.studentId == studentId)
                .collect(Collectors.toList());
            
            if (studentResults.isEmpty()) {
                sendResponse(exchange, 404, "{\"error\":\"No results found for student\"}");
                return;
            }
            
            double averageScore = studentResults.stream()
                .mapToDouble(QuizResult::getPercentage)
                .average()
                .orElse(0);
            
            String json = String.format(
                "{\"student_id\":%d,\"quizzes_taken\":%d,\"average_percentage\":%.2f}",
                studentId, studentResults.size(), averageScore
            );
            
            sendResponse(exchange, 200, json);
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid student ID\"}");
        }
    }
    
    private static QuizStats calculateQuizStats(int quizId, List<QuizResult> results) {
        QuizStats stats = new QuizStats();
        stats.quizId = quizId;
        stats.participantCount = results.size();
        
        if (results.isEmpty()) {
            return stats;
        }
        
        // Beregn gjennomsnitt
        double sum = results.stream().mapToDouble(r -> r.score).sum();
        stats.averageScore = sum / results.size();
        
        // Beregn standardavvik
        double variance = results.stream()
            .mapToDouble(r -> Math.pow(r.score - stats.averageScore, 2))
            .average()
            .orElse(0);
        stats.standardDeviation = Math.sqrt(variance);
        
        // Min og max
        stats.minScore = results.stream().mapToInt(r -> r.score).min().orElse(0);
        stats.maxScore = results.stream().mapToInt(r -> r.score).max().orElse(0);
        
        return stats;
    }
    
    private static void saveStudentsToCSV() throws IOException {
        StringBuilder csv = new StringBuilder();
        for (Student student : students.values()) {
            csv.append(student.id).append(",")
               .append(student.name).append(",")
               .append(student.program).append("\n");
        }
        Files.write(Paths.get(studentsCsvPath), csv.toString().getBytes());
    }
    
    private static void saveQuizResultsToCSV() throws IOException {
        StringBuilder csv = new StringBuilder("quiz_id,student_id,score,max_score\n");
        for (QuizResult result : quizResults) {
            csv.append(result.quizId).append(",")
               .append(result.studentId).append(",")
               .append(result.score).append(",")
               .append(result.maxScore).append("\n");
        }
        Files.write(Paths.get(quizCsvPath), csv.toString().getBytes());
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
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        
        byte[] responseBytes = response.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}
