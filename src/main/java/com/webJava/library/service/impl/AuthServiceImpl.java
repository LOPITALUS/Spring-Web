package com.webJava.library.service.impl;

import com.webJava.library.dto.user.CreateUserRequest;
import com.webJava.library.exceptions.RegistEx;
import com.webJava.library.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.webJava.library.dto.auth.LoginRequest;
import com.webJava.library.dto.auth.LoginResponse;
import com.webJava.library.dto.auth.RegisterRequest;
import com.webJava.library.exceptions.EntityAlreadyExistsException;
import com.webJava.library.exceptions.EntityNotFoundException;
import com.webJava.library.helpers.ImageUtils;
import com.webJava.library.models.Image;
import com.webJava.library.models.User;
import com.webJava.library.repository.RoleRepository;
import com.webJava.library.repository.UserRepository;
import com.webJava.library.security.JwtGenerator;
import com.webJava.library.service.AuthService;

import java.io.IOException;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtGenerator jwtGenerator;
    private final ImageUtils imageUtils;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleRepository roleRepository,
                           AuthenticationManager authenticationManager, JwtGenerator jwtGenerator, ImageUtils imageUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.authenticationManager = authenticationManager;
        this.jwtGenerator = jwtGenerator;
        this.imageUtils = imageUtils;
    }


    @Override
    @Transactional
    public int register(RegisterRequest request) throws IOException {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new RegistEx("Username is taken");

        var role = roleRepository.findByName("default").orElseThrow(() -> new EntityNotFoundException("Role not found"));
        var avatar = request.getAvatar();
        var image = new Image(avatar.getOriginalFilename(), avatar.getContentType(), imageUtils.compress(avatar.getBytes()));

        var user = new User(role, image, request.getUsername(), passwordEncoder.encode(request.getPassword()),
                request.getFirstName(), request.getLastName());

        var newUser = userRepository.save(user);

        return newUser.getId();
    }

    @Override
    @Transactional
    public int create(CreateUserRequest request) throws IOException {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new EntityAlreadyExistsException("Username is taken");

        var role = roleRepository.findByName(request.getRole()).orElseThrow(() -> new EntityNotFoundException("Role not found"));
        var avatar = request.getAvatar();
        var image = new Image(avatar.getOriginalFilename(), avatar.getContentType(), imageUtils.compress(avatar.getBytes()));

        var user = new User(role, image, request.getUsername(), passwordEncoder.encode(request.getPassword()),
                request.getFirstName(), request.getLastName());

        var newUser = userRepository.save(user);

        return newUser.getId();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        var token = jwtGenerator.generateToken(auth);

        return new LoginResponse(token);
    }
}
