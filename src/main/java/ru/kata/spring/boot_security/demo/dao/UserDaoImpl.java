package ru.kata.spring.boot_security.demo.dao;

import org.springframework.stereotype.Repository;
import ru.kata.spring.boot_security.demo.model.User;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class UserDaoImpl implements UserDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<User> findAll() {
        return em.createQuery(
                "select distinct u from User u left join fetch u.roles order by u.id",
                User.class
        ).getResultList();
    }

    @Override
    public User findById(Long id) {
        return em.createQuery(
                "select u from User u left join fetch u.roles where u.id = :id",
                User.class
        ).setParameter("id", id)
         .getResultStream()
         .findFirst()
         .orElse(null);
    }

    @Override
    public User findByEmail(String email) {
        return em.createQuery(
                "select u from User u left join fetch u.roles where u.email = :email",
                User.class
        ).setParameter("email", email)
         .getResultStream()
         .findFirst()
         .orElse(null);
    }

    @Override
    public void save(User user) {
        em.persist(user);
    }

    @Override
    public void update(User user) {
        em.merge(user);
    }

    @Override
    public void delete(Long id) {
        User user = findById(id);
        if (user != null) {
            em.remove(user);
        }
    }
}
