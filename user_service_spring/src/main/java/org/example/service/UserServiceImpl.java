package org.example.service;

import org.example.dto.CreateUserDto;
import org.example.dto.UpdateUserDto;
import org.example.exception.UserNotFoundException;
import org.example.kafka.UserEvent;
import org.example.kafka.UserEventNotifier;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserEventNotifier userEventNotifier;

    public UserServiceImpl(UserRepository userRepository, UserEventNotifier userEventNotifier) {
        this.userRepository = userRepository;
        this.userEventNotifier = userEventNotifier;
    }

    @Override
    public User create(CreateUserDto dto) {
        dto.validate();
        User user = new User(null, dto.getName(), dto.getEmail(), dto.getAge(), null);
        User saved = userRepository.save(user);
        userEventNotifier.notify(new UserEvent("CREATED", saved.getEmail()));
        return saved;
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User update(Long id, UpdateUserDto dto) {
        dto.validate();
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        user.setEmail(dto.getEmail());
        user.setAge(dto.getAge());
        return userRepository.save(user);
    }

    @Override
    public void delete(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        String email = user.getEmail();
        userRepository.deleteById(id);
        userEventNotifier.notify(new UserEvent("DELETED", email));
    }
}
