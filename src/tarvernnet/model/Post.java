package tarvernnet.model;

import java.util.ArrayList;
import java.util.Date;

public class Post {
    private String title;
    private Date date;
    private ArrayList<Like> likes;
    private ArrayList<Comment> comments;

    public Post(String title, Date date, ArrayList<Like> likes, ArrayList<Comment> comments) {
        this.title = title;
        this.date = date;
        this.likes = likes;
        this.comments = comments;
    }
}
