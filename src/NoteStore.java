import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class NoteStore {
    private final Path file;
    private final List<Note> notes = new ArrayList<>();
    private int nextId = 1;

    public NoteStore(String fileName) {
        this.file = Paths.get(fileName);
        load();
    }

    // CREATE
    public Note create(String title, String body) {
        Note note = new Note(nextId++, title, body);
        notes.add(note);
        save();
        return note;
    }

    // READ
    public List<Note> findAll() {
        return new ArrayList<>(notes);
    }

    public Note findById(int id) {
        for (Note n : notes) {
            if (n.getId() == id) return n;
        }
        return null;
    }

    // UPDATE
    public boolean update(int id, String title, String body) {
        Note note = findById(id);
        if (note == null) return false;
        note.setTitle(title);
        note.setBody(body);
        save();
        return true;
    }

    // DELETE
    public boolean delete(int id) {
        Note note = findById(id);
        if (note == null) return false;
        notes.remove(note);
        save();
        return true;
    }

    public Path getFile() { return file; }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] parts = line.split("\t", -1);
                if (parts.length != 3) continue;
                int id = Integer.parseInt(parts[0]);
                notes.add(new Note(id, unescape(parts[1]), unescape(parts[2])));
                nextId = Math.max(nextId, id + 1);
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("[WARN] Could not read " + file + ": " + e.getMessage());
        }
    }

    private void save() {
        List<String> lines = new ArrayList<>();
        for (Note n : notes) {
            lines.add(n.getId() + "\t" + escape(n.getTitle()) + "\t" + escape(n.getBody()));
        }
        try {
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("[ERROR] Could not save " + file + ": " + e.getMessage());
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n");
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                sb.append(next == 't' ? '\t' : next == 'n' ? '\n' : next);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
