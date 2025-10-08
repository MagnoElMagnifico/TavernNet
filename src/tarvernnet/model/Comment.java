package tarvernnet.model;

import java.util.Date;

public class Comment {
    private int user;
    private String content;
    private Date date;

    public Comment(int user, String content, Date date) {
        this.user = user;
        this.content = content;
        this.date = date;
    }

    public int getUser() {
        return user;
    }

    public String getContent() {
        return content;
    }

    public Date getDate() {
        return date;
    }
}
