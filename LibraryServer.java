import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;

public class LibraryServer {

    public static void main(String[] args) throws Exception {
        String portStr = System.getenv("PORT");
        int port = (portStr != null) ? Integer.parseInt(portStr) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // API Endpoints
        server.createContext("/api/login", LibraryServer::handleLogin);
        server.createContext("/api/books", LibraryServer::handleBooks);
        server.createContext("/api/members", LibraryServer::handleMembers);
        server.createContext("/api/issued", LibraryServer::handleIssued);
        server.createContext("/api/issue", LibraryServer::handleIssue);
        server.createContext("/api/return", LibraryServer::handleReturn);

        // Static file server
        server.createContext("/", LibraryServer::handleStatic);

        server.setExecutor(null);
        server.start();
        System.out.println("========================================");
        System.out.println("  Library Server started on port " + port);
        System.out.println("  Open http://localhost:" + port + " in browser");
        System.out.println("========================================");
    }

    // ─── CORS helper ─────────────────────────────────────────────
    static void cors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    // ─── Send JSON response ──────────────────────────────────────
    static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        cors(ex);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes("UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    // ─── Read POST body ─────────────────────────────────────────
    static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), "UTF-8");
    }

    // ─── Simple JSON value extractor ────────────────────────────
    static String jsonVal(String json, String key) {
        String search = "\"" + key + "\"";
        int i = json.indexOf(search);
        if (i == -1) return "";
        i = json.indexOf(":", i) + 1;
        // skip whitespace
        while (i < json.length() && json.charAt(i) == ' ') i++;
        if (i >= json.length()) return "";
        if (json.charAt(i) == '"') {
            int end = json.indexOf('"', i + 1);
            return json.substring(i + 1, end);
        } else {
            int end = i;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(i, end).trim();
        }
    }

    // ─── /api/books ─────────────────────────────────────────────
    static void handleBooks(HttpExchange ex) throws IOException {
        cors(ex);
        if (ex.getRequestMethod().equals("OPTIONS")) {
            ex.sendResponseHeaders(204, -1); return;
        }
        if (ex.getRequestMethod().equals("GET")) {
            sendJson(ex, 200, FileHandler.getBooksJson());
        } else if (ex.getRequestMethod().equals("POST")) {
            String body = readBody(ex);
            String title = jsonVal(body, "title");
            String author = jsonVal(body, "author");
            String msg = FileHandler.addBook(title, author);
            sendJson(ex, 200, "{\"message\":\"" + msg.replace("\"", "\\\"") + "\"}");
        } else {
            ex.sendResponseHeaders(405, -1);
        }
    }

    // ─── /api/members ───────────────────────────────────────────
    static void handleMembers(HttpExchange ex) throws IOException {
        cors(ex);
        if (ex.getRequestMethod().equals("OPTIONS")) {
            ex.sendResponseHeaders(204, -1); return;
        }
        if (ex.getRequestMethod().equals("GET")) {
            sendJson(ex, 200, FileHandler.getMembersJson());
        } else if (ex.getRequestMethod().equals("POST")) {
            String body = readBody(ex);
            String name = jsonVal(body, "name");
            String msg = FileHandler.addMember(name);
            sendJson(ex, 200, "{\"message\":\"" + msg.replace("\"", "\\\"") + "\"}");
        } else {
            ex.sendResponseHeaders(405, -1);
        }
    }

    // ─── /api/issued ────────────────────────────────────────────
    static void handleIssued(HttpExchange ex) throws IOException {
        cors(ex);
        if (ex.getRequestMethod().equals("OPTIONS")) {
            ex.sendResponseHeaders(204, -1); return;
        }
        sendJson(ex, 200, FileHandler.getIssuedJson());
    }

    // ─── /api/issue ─────────────────────────────────────────────
    static void handleIssue(HttpExchange ex) throws IOException {
        cors(ex);
        if (ex.getRequestMethod().equals("OPTIONS")) {
            ex.sendResponseHeaders(204, -1); return;
        }
        String body = readBody(ex);
        int bookId = Integer.parseInt(jsonVal(body, "bookId"));
        int memberId = Integer.parseInt(jsonVal(body, "memberId"));
        String msg = FileHandler.issueBook(bookId, memberId);
        sendJson(ex, 200, "{\"message\":\"" + msg.replace("\"", "\\\"") + "\"}");
    }

    // ─── /api/return ────────────────────────────────────────────
    static void handleReturn(HttpExchange ex) throws IOException {
        cors(ex);
        if (ex.getRequestMethod().equals("OPTIONS")) {
            ex.sendResponseHeaders(204, -1); return;
        }
        String body = readBody(ex);
        int bookId = Integer.parseInt(jsonVal(body, "bookId"));
        String msg = FileHandler.returnBook(bookId);
        sendJson(ex, 200, "{\"message\":\"" + msg.replace("\"", "\\\"") + "\"}");
    }

    // ─── /api/login ─────────────────────────────────────────────
    static void handleLogin(HttpExchange ex) throws IOException {
        cors(ex);
        if (ex.getRequestMethod().equals("OPTIONS")) {
            ex.sendResponseHeaders(204, -1); return;
        }
        if (ex.getRequestMethod().equals("POST")) {
            String body = readBody(ex);
            String username = jsonVal(body, "username");
            String password = jsonVal(body, "password");
            if ("admin".equals(username) && "password".equals(password)) {
                sendJson(ex, 200, "{\"success\":true, \"message\":\"Login successful\"}");
            } else {
                sendJson(ex, 401, "{\"success\":false, \"message\":\"Invalid credentials\"}");
            }
        } else {
            ex.sendResponseHeaders(405, -1);
        }
    }

    // ─── Static files ───────────────────────────────────────────
    static void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/login.html";

        File file = new File("web" + path);
        if (!file.exists() || file.isDirectory()) {
            String msg = "404 Not Found";
            ex.sendResponseHeaders(404, msg.length());
            ex.getResponseBody().write(msg.getBytes());
            ex.getResponseBody().close();
            return;
        }

        String mime = "text/plain";
        if (path.endsWith(".html")) mime = "text/html";
        else if (path.endsWith(".css"))  mime = "text/css";
        else if (path.endsWith(".js"))   mime = "application/javascript";
        else if (path.endsWith(".png"))  mime = "image/png";
        else if (path.endsWith(".jpg"))  mime = "image/jpeg";
        else if (path.endsWith(".svg"))  mime = "image/svg+xml";

        ex.getResponseHeaders().set("Content-Type", mime + "; charset=UTF-8");
        byte[] bytes = Files.readAllBytes(file.toPath());
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }
}
