package com.webJava.library.controllers;

import com.webJava.library.dto.book.GetBookResponse;
import com.webJava.library.dto.post.GetPostResponse;
import com.webJava.library.security.JwtGenerator;
import com.webJava.library.service.PostService;
import com.webJava.library.service.StatusService;
import com.webJava.library.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.webJava.library.service.StatusService;
import java.util.List;

@Controller
public class NewsController {
    private final PostService postService;
    private final UserService userService;
    private final JwtGenerator jwt;
    private final StatusService statusService;

    public NewsController(PostService postService, UserService userService, JwtGenerator jwt, StatusService statusService){
        this.postService = postService;
        this.userService = userService;
        this.jwt = jwt;
        this.statusService = statusService;
    }

    @GetMapping("/")
    public String home(Model model) {
        return "login";
    }

    @GetMapping("/regist")
    public String regist(Model model) {
        model.addAttribute("title", "Регистрация");
        return "regist";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("title", "Вход");
        return "login";
    }

    @GetMapping("/about")
    public String about(@CookieValue(value = "AccessToken", required = false) String token, Model model) {
        if (token == null)
        {
            return "login";
        }
        model.addAttribute("count", statusService.getVisitors());
        var username = jwt.getUsernameFromJwt(token);
        var user = userService.getUserByName(username);
        model.addAttribute("title", "Новости");
        List<GetPostResponse> postList = retrievePostList();
        model.addAttribute("posts", postList);
        model.addAttribute("postService", postService);
        model.addAttribute("role", user.getRoleId());
        return "about";
    }

    @GetMapping(value = "/{id}/postImage", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getPostImage(@PathVariable int id) {
        return ResponseEntity.ok(postService.getPostImage(id));
    }

    private List<GetPostResponse> retrievePostList() {
        var posts = this.postService.getAll(0, 10).getContent();
        return posts;
    }


    @GetMapping(value = "/post/{id}")
    public String getPost(@PathVariable int id, Model model, @CookieValue("AccessToken") String token) {
        if (token == null)
        {
            return "login";
        }
        var username = jwt.getUsernameFromJwt(token);
        var user = userService.getUserByName(username);
        model.addAttribute("role", user.getRoleId());
        model.addAttribute("post", this.postService.getById(id));
        model.addAttribute("postService", this.postService);
        return "post";
    }
}
