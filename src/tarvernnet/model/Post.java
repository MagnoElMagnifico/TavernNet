package tarvernnet.model;

import java.util.ArrayList;
import java.util.Date;

public class Post {
    private String title;
    private Date date;
    private ArrayList<Integer> likes;
    private ArrayList<Comment> comments;

    public Post(String title, Date date, ArrayList<Integer> likes, ArrayList<Comment> comments) {
        this.title = title;
        this.date = date;
        this.likes = likes;
        this.comments = comments;
    }
}
