package com.webJava.library.service.impl;

import com.webJava.library.dto.user.GetUserResponse;
import com.webJava.library.exceptions.EntityAlreadyExistsException;
import com.webJava.library.models.User;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.webJava.library.dto.PageDto;
import com.webJava.library.dto.book.CreateBookRequest;
import com.webJava.library.dto.book.GetBookResponse;
import com.webJava.library.exceptions.AccessDeniedException;
import com.webJava.library.exceptions.EntityNotFoundException;
import com.webJava.library.helpers.ImageUtils;
import com.webJava.library.helpers.PageDtoMaker;
import com.webJava.library.models.Image;
import com.webJava.library.models.Book;
import com.webJava.library.repository.ImageRepository;
import com.webJava.library.repository.BookRepository;
import com.webJava.library.repository.UserRepository;
import com.webJava.library.service.BookService;

import java.io.IOException;

@Service
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final PageDtoMaker<Book, GetBookResponse> pageDtoMaker;

    private final PageDtoMaker<User, GetUserResponse> pageDtoMakerUser;
    private final ImageUtils imageUtils;
    private final ImageRepository imageRepository;

    @Autowired
    public BookServiceImpl(BookRepository bookRepository,UserRepository userRepository, PageDtoMaker<Book, GetBookResponse> pageDtoMaker, ImageUtils imageUtils, ImageRepository imageRepository, PageDtoMaker<User, GetUserResponse> pageDtoMakerUser) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.pageDtoMaker = pageDtoMaker;
        this.imageUtils = imageUtils;
        this.imageRepository = imageRepository;
        this.pageDtoMakerUser = pageDtoMakerUser;
    }

    @Override
    public int create(CreateBookRequest request) throws IOException {
        var file = request.getImage();
        var image = new Image(file.getOriginalFilename(), file.getContentType(), imageUtils.compress(file.getBytes()));
        var post = new Book(image, request.getTitle(), request.getAuthor(), request.getAnnotation());

        var newPost = bookRepository.save(post);

        return newPost.getId();
    }

    @Override
    @Transactional
    public GetBookResponse getById(int id) {
        var book = bookRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Post not found"));

        return mapToResponse(book);
    }

    @Override
    @Transactional
    public byte[] getBookImage(int postId) {
        var book = bookRepository.findById(postId).orElseThrow(() -> new EntityNotFoundException("Post not found"));
        var image = book.getImage();

        return imageUtils.decompress(image.getData());
    }

    @Override
    @Transactional
    public PageDto<GetBookResponse> getAllByUserId(int pageNumber, int pageSize, int userId) {
        var pageRequest = PageRequest.of(pageNumber, pageSize);
        var page = bookRepository.findBooksByUsersId(userId, pageRequest);
        return pageDtoMaker.makePageDto(page, this::mapToResponse);
    }


    @Override
    @Transactional
    public PageDto<GetBookResponse> getAll(int pageNumber, int pageSize) {
        var pageRequest = PageRequest.of(pageNumber, pageSize);
        var page = bookRepository.findAll(pageRequest);

        return pageDtoMaker.makePageDto(page, this::mapToResponse);
    }

    @Override
    @Transactional
    public void addUser(int userId, int bookId) {
        if (bookRepository.existsByIdAndUsers_Id(bookId, userId)) throw new EntityAlreadyExistsException("user already has this book");
        var book = bookRepository.getById(bookId);
        var user = userRepository.getById(userId);
        user.getBooks().add(book);
        userRepository.save(user);
    }



    @Override
    public void delete(int id) {
        var book = bookRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Post not found"));
           bookRepository.delete(book);
    }


    public GetBookResponse mapToResponse(Book book) {
        return new GetBookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getAnnotation());
    }

}

