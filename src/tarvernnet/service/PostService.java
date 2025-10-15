package tarvernnet.service;

import tarvernnet.model.Post;
import tarvernnet.repository.PostRepository;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class PostService {
    private final PostRepository posts;

    @Autowired
    public PostService(PostRepository posts) {
        this.posts = posts;
    }

    public List<Post> something() {
        return posts.findAll();
    }
}

