package ru.kata.spring.boot_security.demo.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import ru.kata.spring.boot_security.demo.service.UserServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@SuppressWarnings("deprecation")
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private final UserServiceImpl userService;
    private final SuccessUserHandler successUserHandler;
    private final PasswordEncoder passwordEncoder;
    public WebSecurityConfig(UserServiceImpl userService, SuccessUserHandler successUserHandler, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.successUserHandler = successUserHandler;
        this.passwordEncoder = passwordEncoder;
    }
    @Override protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService).passwordEncoder(passwordEncoder);
    }
    @Override protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers("/", "/login", "/css/**", "/js/**", "/error").permitAll()
            .antMatchers("/admin", "/admin/**", "/api/users", "/api/users/**", "/api/roles").hasRole("ADMIN")
            .antMatchers("/user", "/api/user").hasAnyRole("USER", "ADMIN")
            .anyRequest().authenticated()
            .and().formLogin().loginPage("/login").successHandler(successUserHandler).permitAll()
            .and().logout().logoutUrl("/logout").logoutSuccessUrl("/login?logout").permitAll();
    }
}
