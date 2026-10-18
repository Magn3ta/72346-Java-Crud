public class Note {
    private final int id;
    private String title;
    private String body;

    public Note(int id, String title, String body) {
        this.id = id;
        this.title = title;
        this.body = body;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }

    public void setTitle(String title) { this.title = title; }
    public void setBody(String body) { this.body = body; }

    @Override
    public String toString() {
        return "#" + id + "  " + title + (body.isEmpty() ? "" : "  -  " + body);
    }
}
