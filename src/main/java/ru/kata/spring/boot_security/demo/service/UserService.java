package ru.kata.spring.boot_security.demo.service;
import ru.kata.spring.boot_security.demo.model.User;
import java.util.List;
import java.util.Set;
public interface UserService {
    List<User> findAll();
    User findById(Long id);
    User findByEmail(String email);
    void create(User user, Set<Long> roleIds);
    void update(Long id, User edited, Set<Long> roleIds);
    void delete(Long id);
}
