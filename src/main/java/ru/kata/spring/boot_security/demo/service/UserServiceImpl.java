package ru.kata.spring.boot_security.demo.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kata.spring.boot_security.demo.dao.UserDao;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.repository.RoleRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService, UserDetailsService {
    private final UserDao userDao;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    public UserServiceImpl(UserDao userDao, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userDao = userDao; this.roleRepository = roleRepository; this.passwordEncoder = passwordEncoder;
    }
    @Override public List<User> findAll() { return userDao.findAll(); }
    @Override public User findById(Long id) { return userDao.findById(id); }
    @Override public User findByEmail(String email) { return userDao.findByEmail(email); }
    @Override public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userDao.findByEmail(username);
        if (user == null) throw new UsernameNotFoundException("User not found");
        return user;
    }
    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) throw new IllegalArgumentException("Select at least one role");
        List<Role> found = roleRepository.findAllById(roleIds);
        if (found.size() != roleIds.size()) throw new IllegalArgumentException("Unknown role");
        return new HashSet<>(found);
    }
    @Override @Transactional public void create(User user, Set<Long> roleIds) {
        if (userDao.findByEmail(user.getEmail()) != null) throw new IllegalArgumentException("Email already exists");
        if (user.getPassword() == null || user.getPassword().isBlank()) throw new IllegalArgumentException("Password required");
        user.setId(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(resolveRoles(roleIds));
        userDao.save(user);
    }
    @Override @Transactional public void update(Long id, User edited, Set<Long> roleIds) {
        User existing = userDao.findById(id);
        if (existing == null) throw new IllegalArgumentException("User not found");
        User duplicate = userDao.findByEmail(edited.getEmail());
        if (duplicate != null && !duplicate.getId().equals(id))
            throw new IllegalArgumentException("Email already exists");
        existing.setName(edited.getName());
        existing.setLastName(edited.getLastName());
        existing.setEmail(edited.getEmail());
        existing.setAge(edited.getAge());
        if (edited.getPassword() != null && !edited.getPassword().isBlank())
            existing.setPassword(passwordEncoder.encode(edited.getPassword()));
        existing.setRoles(resolveRoles(roleIds));
        userDao.update(existing);
    }
    @Override @Transactional public void delete(Long id) { userDao.delete(id); }
}
