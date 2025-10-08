package tarvernnet.model;

import java.util.Date;

public class Like {
    private int user;
    private int post;

    public Like(int user, int post) {
        this.user = user;
        this.post = post;
    }

    public int getUser() {
        return user;
    }

    public int getPost() {
        return post;
    }

    public void setPost(int post) {
        this.post = post;
    }
}
