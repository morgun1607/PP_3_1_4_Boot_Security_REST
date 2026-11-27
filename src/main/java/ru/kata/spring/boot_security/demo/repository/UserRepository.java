package ru.kata.spring.boot_security.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.kata.spring.boot_security.demo.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Override
    @Query("select distinct u from User u left join fetch u.roles order by u.id")
    List<User> findAll();

    @Override
    @Query("select distinct u from User u left join fetch u.roles where u.id = :id")
    Optional<User> findById(@Param("id") Long id);

    @Query("select distinct u from User u left join fetch u.roles where u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);
}
