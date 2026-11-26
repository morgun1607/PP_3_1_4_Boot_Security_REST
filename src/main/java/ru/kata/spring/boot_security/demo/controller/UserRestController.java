package ru.kata.spring.boot_security.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.repository.RoleRepository;
import ru.kata.spring.boot_security.demo.service.UserService;
import java.util.List;
import java.util.Set;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserRestController {
    private final UserService users;
    private final RoleRepository roles;
    public UserRestController(UserService users, RoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    public record RoleDto(Long id, String name) {
        static RoleDto from(Role role) { return new RoleDto(role.getId(), role.getName()); }
    }
    public record UserDto(Long id, String name, String lastName, Integer age,
                          String email, List<RoleDto> roles, String roleNames) {
        static UserDto from(User user) {
            return new UserDto(user.getId(), user.getName(), user.getLastName(),
                    user.getAge(), user.getEmail(), user.getRoles().stream()
                    .map(RoleDto::from).toList(), user.getRoleNames());
        }
    }
    public record UserRequest(String name, String lastName, Integer age,
                              String email, String password, Set<Long> roleIds) {
        User toUser() {
            if (name == null || name.isBlank() || lastName == null || lastName.isBlank()
                    || email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
                    || age == null || age < 0 || age > 150) {
                throw new IllegalArgumentException("Invalid user data");
            }
            User user = new User(name, lastName, email, password);
            user.setAge(age);
            return user;
        }
    }

    private User requireUser(Long id) {
        User user = users.findById(id);
        if (user == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return user;
    }
    @GetMapping("/users")
    public List<UserDto> all() { return users.findAll().stream().map(UserDto::from).toList(); }
    @GetMapping("/users/{id}")
    public UserDto one(@PathVariable Long id) { return UserDto.from(requireUser(id)); }
    @GetMapping("/user")
    public UserDto current(@AuthenticationPrincipal User principal) {
        return UserDto.from(requireUser(principal.getId()));
    }
    @GetMapping("/roles")
    public List<RoleDto> roles() { return roles.findAll().stream().map(RoleDto::from).toList(); }
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@RequestBody UserRequest request) { users.create(request.toUser(), request.roleIds()); }
    @PutMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable Long id, @RequestBody UserRequest request) {
        requireUser(id);
        users.update(id, request.toUser(), request.roleIds());
    }
    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        requireUser(id);
        users.delete(id);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalid(IllegalArgumentException e) { return Map.of("message", e.getMessage()); }
}