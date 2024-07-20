package com.webJava.library.controllers;

import com.webJava.library.dto.auth.LoginRequest;
import com.webJava.library.dto.auth.RegisterRequest;
import com.webJava.library.dto.book.CreateBookRequest;
import com.webJava.library.dto.book.GetBookResponse;
import com.webJava.library.dto.post.CreatePostRequest;
import com.webJava.library.dto.post.UpdatePostRequest;
import com.webJava.library.dto.user.CreateUserRequest;
import com.webJava.library.dto.user.GetUserResponse;
import com.webJava.library.dto.user.UpdateUserRequest;
import com.webJava.library.exceptions.RegistEx;
import com.webJava.library.security.JwtGenerator;
import com.webJava.library.service.AuthService;
import com.webJava.library.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Controller
public class UserController {
    private final AuthService authService;
    private final UserService userService;
    private final JwtGenerator jwt;

    public UserController(UserService userService, JwtGenerator jwt, AuthService authService){
        this.userService = userService;
        this.jwt = jwt;
        this.authService = authService;
    }

    @GetMapping("/user-edit")
    public String add(Model model, @CookieValue(value = "AccessToken", required = false) String token) throws IOException {
        if (token == null)
        {
            return "login";
        }
        List<GetUserResponse> userList = retrieveUserList();
        model.addAttribute("users", userList);
        return "user-edit";
    }
    @GetMapping(value = "/{id}/avatarImage", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getAvatarImage(@PathVariable int id) {
        var avatar = userService.getAvatar(id);
        return ResponseEntity.ok(userService.getUserAvatar(id));
    }

    @PostMapping("/{id}/user-delete")
    public void deleteUser(HttpServletResponse response, Model model, @PathVariable int id) throws IOException {
        model.addAttribute("user", userService.getById(id));
        userService.delete(id);
        response.sendRedirect("/user-edit");
    }

    @GetMapping("/user-create")
    public String home2(@CookieValue(value = "AccessToken", required = false) String token) {
        if (token == null)
        {
            return "login";
        }
        var username = jwt.getUsernameFromJwt(token);
        var user = userService.getUserByName(username);
        if (user.getRoleId() <2)
        {
            return ("AccessDenied");
        }
        return "user-create";
    }

    @PostMapping("/user-create")
    public String addPost(@Valid @ModelAttribute CreateUserRequest request, HttpServletResponse response, Model model, @CookieValue("AccessToken") String token) throws IOException {
        if (!userService.isUniqueLogin(request.getUsername())){
            model.addAttribute("error", "usernameTaken");
            return "user-create";
        }

         if (!request.getFirstName().matches("^[\\p{IsCyrillic}]+$") || !request.getLastName().matches("^[\\p{IsCyrillic}]+$")){
             model.addAttribute("error", "notRussian");
             return "user-create";
         }
         var username = jwt.getUsernameFromJwt(token);
         var user = userService.getUserByName(username);
         if (user.getRoleId() < 2) {
             return "AccessDenied";
         }

         var register = authService.create(request);
         return "user-create";
    }

    @PostMapping("/{id}/user-update")
    public String updatePost(@Valid @ModelAttribute UpdateUserRequest request, HttpServletResponse response, Model model, @PathVariable int id) throws IOException {
        model.addAttribute("user", userService.getById(id));

        if (!request.getUsername().equals(userService.getById(id).getUsername())){
            if (!userService.isUniqueLogin(request.getUsername())){
                model.addAttribute("error", "usernameTaken");
                return "user-update";
            }
        }

        if (!request.getFirstName().matches("^[\\p{IsCyrillic}]+$") || !request.getLastName().matches("^[\\p{IsCyrillic}]+$")){
            model.addAttribute("error", "notRussian");
            return "user-update";
        }

        model.addAttribute("user", userService.getById(id));
        var update = userService.update(id, request);

        response.sendRedirect("/user-edit");
        return null;
    }

    @GetMapping("/{id}/user-update")
    public String getUpdatePost(HttpServletResponse response, Model model, @PathVariable int id, @CookieValue(value = "AccessToken", required = false) String token) throws IOException {
        if (token == null)
        {
            return "login";
        }
        model.addAttribute("user", userService.getById(id));
        var username = jwt.getUsernameFromJwt(token);
        var user = userService.getUserByName(username);
        if (user.getRoleId() <2)
        {
            return "AccessDenied";
        }
        return "user-update";
    }


    private List<GetUserResponse> retrieveUserList() {
        var users = this.userService.getAll(0, 10).getContent();
        return users;
    }
}
