package tarvernnet.controller;

import tarvernnet.model.Post;
import tarvernnet.service.PostService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import org.bson.types.ObjectId;

@RestController
@RequestMapping("posts")
public class PostController {
    PostService posts;

    @Autowired
    public PostController(PostService posts) {
        this.posts = posts;
    }

    @GetMapping
    @Valid
    public List<Post> getPosts() {
        return posts.getPosts();
    }
}
