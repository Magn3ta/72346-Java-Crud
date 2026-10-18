import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * A tiny web server using only the JDK. The HTML is built here in Java,
 * so there is no JavaScript and no outside library.
 */
public class Main {
    private static final int PORT = 8080;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final NoteStore store = new NoteStore("notes.txt");

    public static void main(String[] args) throws IOException {
        log("INFO", "Starting Notes CRUD web application...");
        log("INFO", "Loaded " + store.findAll().size() + " note(s) from " + store.getFile().toAbsolutePath());

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", Main::handle);
        server.start();

        log("INFO", "Application started successfully on http://localhost:" + PORT);
        log("INFO", "Press the red stop button in IntelliJ to shut down.");

        store.create("TestNote1", "Blablablabla");
        store.create("TestNote2", "Blablablabla123");
        store.create("TestNote3", "Blablablabla1234567890");
    }

    // ---------- routing ----------

    private static void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        int status = 200;
        try {
            if (method.equals("GET") && path.equals("/")) {
                Map<String, String> query = parseForm(ex.getRequestURI().getRawQuery());
                send(ex, 200, page(parseId(query.get("edit"))));
            } else if (method.equals("POST") && path.equals("/create")) {
                Map<String, String> form = readForm(ex);
                String title = form.getOrDefault("title", "").trim();
                if (!title.isEmpty()) {
                    Note note = store.create(title, form.getOrDefault("body", "").trim());
                    log("INFO", "CREATED note " + note);
                }
                status = redirect(ex, "/");
            } else if (method.equals("POST") && path.equals("/update")) {
                Map<String, String> form = readForm(ex);
                Integer id = parseId(form.get("id"));
                String title = form.getOrDefault("title", "").trim();
                if (id != null && !title.isEmpty()
                        && store.update(id, title, form.getOrDefault("body", "").trim())) {
                    log("INFO", "UPDATED note " + store.findById(id));
                }
                status = redirect(ex, "/");
            } else if (method.equals("POST") && path.equals("/delete")) {
                Integer id = parseId(readForm(ex).get("id"));
                if (id != null && store.delete(id)) {
                    log("INFO", "DELETED note #" + id);
                }
                status = redirect(ex, "/");
            } else {
                status = 404;
                send(ex, 404, layout("Not found", "<p>Page not found. <a href=\"/\">Back to notes</a></p>"));
            }
        } catch (Exception e) {
            status = 500;
            log("ERROR", "Request failed: " + e);
            send(ex, 500, layout("Error", "<p>Something went wrong.</p>"));
        }
        log("INFO", method + " " + path + " -> " + status);
    }

    // ---------- HTML (built in Java) ----------

    private static String page(Integer editId) {
        Note editing = editId == null ? null : store.findById(editId);
        StringBuilder sb = new StringBuilder();

        // Add / edit form
        if (editing == null) {
            sb.append("<form method=\"post\" action=\"/create\">")
              .append("<input name=\"title\" placeholder=\"Title\" required>")
              .append("<textarea name=\"body\" rows=\"3\" placeholder=\"Write something...\"></textarea>")
              .append("<div class=\"row\"><button type=\"submit\">Add note</button></div>")
              .append("</form>");
        } else {
            sb.append("<form method=\"post\" action=\"/update\">")
              .append("<input type=\"hidden\" name=\"id\" value=\"").append(editing.getId()).append("\">")
              .append("<input name=\"title\" required value=\"").append(esc(editing.getTitle())).append("\">")
              .append("<textarea name=\"body\" rows=\"3\">").append(esc(editing.getBody())).append("</textarea>")
              .append("<div class=\"row\"><button type=\"submit\">Save changes</button>")
              .append("<a class=\"button secondary\" href=\"/\">Cancel edit</a></div>")
              .append("</form>");
        }

        // Note list
        if (store.findAll().isEmpty()) {
            sb.append("<p class=\"muted\">No notes yet. Add your first one above.</p>");
        } else {
            sb.append("<ul>");
            for (Note n : store.findAll()) {
                sb.append("<li><h2>").append(esc(n.getTitle())).append("</h2>")
                  .append("<p>").append(esc(n.getBody())).append("</p>")
                  .append("<div class=\"row\">")
                  .append("<a class=\"button secondary\" href=\"/?edit=").append(n.getId()).append("\">Edit</a>")
                  .append("<form method=\"post\" action=\"/delete\">")
                  .append("<input type=\"hidden\" name=\"id\" value=\"").append(n.getId()).append("\">")
                  .append("<button class=\"danger\" type=\"submit\">Delete</button></form>")
                  .append("</div></li>");
            }
            sb.append("</ul>");
        }
        return layout("Notes", sb.toString());
    }

    private static String layout(String title, String content) {
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
            + "<link rel=\"icon\" href=\"data:,\">"
            + "<title>" + esc(title) + "</title><style>"
            + ":root{--bg:#f7f7f5;--surface:#fff;--text:#1c1c1c;--muted:#6b6b6b;--line:#deded9;--accent:#2f5d50;--danger:#b3382c}"
            + "@media(prefers-color-scheme:dark){:root{--bg:#161816;--surface:#1f221f;--text:#ececea;--muted:#9a9a96;--line:#333733;--accent:#7fb5a3;--danger:#e0786c}}"
            + "*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:16px/1.5 system-ui,-apple-system,'Segoe UI',sans-serif}"
            + "main{max-width:600px;margin:0 auto;padding:2rem 1rem 4rem}h1{margin:0 0 1.5rem;font-size:1.75rem}"
            + "form{display:grid;gap:.5rem}input,textarea{width:100%;padding:.6rem .75rem;font:inherit;color:inherit;background:var(--surface);border:1px solid var(--line);border-radius:6px}"
            + "main>form{margin-bottom:2rem}"
            + "button,.button{font:inherit;font-size:.95rem;padding:.4rem .9rem;border:1px solid var(--accent);background:var(--accent);color:var(--bg);border-radius:6px;cursor:pointer;text-decoration:none;display:inline-block}"
            + ".secondary{background:transparent;color:var(--accent)}.danger{background:transparent;color:var(--danger);border-color:var(--danger)}"
            + "input:focus,textarea:focus,button:focus-visible,a:focus-visible{outline:2px solid var(--accent);outline-offset:1px}"
            + ".row{display:flex;gap:.5rem;align-items:center}ul{list-style:none;margin:0;padding:0;display:grid;gap:.75rem}"
            + "li{background:var(--surface);border:1px solid var(--line);border-radius:6px;padding:.9rem 1rem}"
            + "li h2{margin:0 0 .25rem;font-size:1.05rem}li p{margin:0 0 .75rem;color:var(--muted);white-space:pre-wrap}.muted{color:var(--muted)}"
            + "</style></head><body><main><h1><a href=\"/\" style=\"color:inherit;text-decoration:none\">Notes</a></h1>"
            + content + "</main></body></html>";
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // ---------- helpers ----------

    private static void send(HttpExchange ex, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static int redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().set("Location", location);
        ex.sendResponseHeaders(303, -1);
        ex.close();
        return 303;
    }

    private static Map<String, String> readForm(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return parseForm(body);
    }

    private static Map<String, String> parseForm(String data) {
        Map<String, String> map = new HashMap<>();
        if (data == null || data.isEmpty()) return map;
        for (String pair : data.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(key, value);
        }
        return map;
    }

    private static Integer parseId(String text) {
        try {
            return Integer.valueOf(text.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static void log(String level, String message) {
        System.out.println("[" + level + "] " + LocalDateTime.now().format(TIME) + "  " + message);
    }
}
