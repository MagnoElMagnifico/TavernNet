package tarvernnet.service;

import tarvernnet.model.Post;
import tarvernnet.repository.PostsRepository;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class PostService {
    private final PostsRepository posts;

    @Autowired
    public PostService(PostsRepository posts) {
        this.posts = posts;
    }

    public List<Post> getPosts() {
        return posts.findAll();
    }
}

