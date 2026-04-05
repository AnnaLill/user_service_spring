package org.example.controller;

import org.example.dto.CreateUserDto;
import org.example.dto.UpdateUserDto;
import org.example.dto.UserDto;
import org.example.model.User;
import org.example.exception.UserNotFoundException;
import org.example.service.UserService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public CollectionModel<UserDto> getAll() {
        List<UserDto> dtos = userService.findAll().stream()
                .map(UserController::toDto)
                .toList();
        Link selfLink = linkTo(methodOn(UserController.class).getAll()).withSelfRel();

        return CollectionModel.of(dtos, selfLink);
    }

    @GetMapping("/{id}")
    public EntityModel <UserDto> getById(@PathVariable("id") Long id) {
        User user = userService.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        UserDto dto = toDto(user);

        Link selfLink = WebMvcLinkBuilder
                .linkTo(WebMvcLinkBuilder.methodOn(UserController.class).getById(id))
                        .withSelfRel();
        Link allUsersLink = WebMvcLinkBuilder
                .linkTo(methodOn(UserController.class).getAll())
                        .withRel("all-users");

        return EntityModel.of(dto, selfLink, allUsersLink);
    }

    @PostMapping
    public ResponseEntity <EntityModel <UserDto>> create(@RequestBody CreateUserDto dto) {
        User created = userService.create(dto);
        UserDto userDto = toDto(created);

        Link selfLink = linkTo(methodOn(UserController.class).getById(created.getId()))
                .withSelfRel();
        EntityModel<UserDto> model = EntityModel.of(userDto, selfLink);
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PutMapping("/{id}")
    public ResponseEntity <EntityModel <UserDto>> update(@PathVariable("id") Long id, @RequestBody UpdateUserDto updateDto) {
        User updated = userService.update(id, updateDto);
        UserDto userDto = toDto(updated);

        Link selfLink = linkTo(methodOn(UserController.class).getById(id))
                .withSelfRel();
        EntityModel<UserDto> model = EntityModel.of(userDto, selfLink);
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getCreatedAt()
        );
    }
}

