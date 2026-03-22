package org.example.console;

import org.example.dto.CreateUserDto;
import org.example.dto.UpdateUserDto;
import org.example.exception.UserServiceException;
import org.example.model.User;
import org.example.service.UserService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@Component
public class UserMenuHandler {

    private final Scanner scanner = new Scanner(System.in);

    private final UserService userService;

    public UserMenuHandler(UserService userService) {
        this.userService = userService;
    }

    public void run() {
        while (true) {
            printMenu();
            String choice = scanner.nextLine();

            try {
                switch (choice) {
                    case "1" -> createUser();
                    case "2" -> listAllUsers();
                    case "3" -> findUserById();
                    case "4" -> updateUser();
                    case "5" -> deleteUser();
                    case "0" -> {
                        System.out.println("Выход...");
                        return;
                    }
                    default -> System.out.println("Неизвестная команда. Введите 0–5.");
                }
            } catch (UserServiceException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== User Service ===");
        System.out.println("1. Создать пользователя");
        System.out.println("2. Показать всех пользователей");
        System.out.println("3. Найти пользователя по id");
        System.out.println("4. Обновить email и возраст");
        System.out.println("5. Удалить пользователя по id");
        System.out.println("0. Выход");
        System.out.print("Выберите действие: ");
    }

    private void createUser() {
        System.out.print("Имя: ");
        String name = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Возраст: ");
        String ageStr = scanner.nextLine();

        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: возраст должен быть числом.");
            return;
        }

        CreateUserDto dto = new CreateUserDto(name, email, age);
        User created = userService.create(dto);
        System.out.println("Пользователь создан: " + created);
    }

    private void listAllUsers() {
        List<User> users = userService.findAll();
        if (users.isEmpty()) {
            System.out.println("Список пуст.");
            return;
        }
        System.out.println("Список пользователей:");
        users.forEach(System.out::println);
    }

    private void findUserById() {
        Long id = readId();
        if (id == null) return;

        Optional<User> found = userService.findById(id);
        System.out.println(found.map(User::toString).orElse("Пользователь не найден."));
    }

    private void updateUser() {
        Long id = readId();
        if (id == null) return;

        System.out.print("Новый email: ");
        String email = scanner.nextLine();
        System.out.print("Новый возраст: ");
        String ageStr = scanner.nextLine();

        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: возраст должен быть числом.");
            return;
        }

        UpdateUserDto dto = new UpdateUserDto(email, age);
        User updated = userService.update(id, dto);
        System.out.println("Пользователь обновлён: " + updated);
    }

    private void deleteUser() {
        Long id = readId();
        if (id == null) return;

        userService.delete(id);
        System.out.println("Пользователь удалён.");
    }


    private Long readId() {
        System.out.print("ID пользователя: ");
        try {
            return Long.parseLong(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: ID должен быть числом.");
            return null;
        }
    }
}
