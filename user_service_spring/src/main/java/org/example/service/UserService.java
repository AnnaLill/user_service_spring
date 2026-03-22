package org.example.service;

import org.example.dto.CreateUserDto;
import org.example.dto.UpdateUserDto;
import org.example.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User create(CreateUserDto dto);

    Optional<User> findById(Long id);

    List<User> findAll();

    User update(Long id, UpdateUserDto dto);

    void delete(Long id);
}
