package ru.kata.spring.boot_security.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:securitytest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "app.admin.password=integration-admin-pass",
        "app.user.password=integration-user-pass"
})
class SecurityIntegrationTest {
    @Autowired private MockMvc mvc;

    private MockHttpSession login(String email, String password) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin().user(email).password(password))
                .andExpect(status().is3xxRedirection())
                .andReturn().getRequest().getSession(false);
    }

    @Test void anonymousCannotAccessProtectedPages() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/users")).andExpect(status().is3xxRedirection());
    }

    @Test void regularUserCanReadOnlyOwnProfile() throws Exception {
        MockHttpSession session = login("user@example.com", "integration-user-pass");
        mvc.perform(get("/api/user").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/users").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/users").with(csrf()).session(session)).andExpect(status().isForbidden());
    }

    @Test void adminCanManageUsersViaRest() throws Exception {
        MockHttpSession session = login("admin@example.com", "integration-admin-pass");
        mvc.perform(get("/admin").session(session)).andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mvc.perform(get("/api/users").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist());
        String roles = mvc.perform(get("/api/roles").session(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long roleId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(roles).get(0).get("id").asLong();
        String body = "{\"name\":\"Test\",\"lastName\":\"Person\",\"age\":25,\"email\":\"new-integration@example.com\",\"password\":\"test-password\",\"roleIds\":[" + roleId + "]}";
        mvc.perform(post("/api/users").session(session).contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/users").session(session).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isCreated());
        String list = mvc.perform(get("/api/users").session(session)).andReturn().getResponse().getContentAsString();
        long id = 0;
        for (com.fasterxml.jackson.databind.JsonNode user : new com.fasterxml.jackson.databind.ObjectMapper().readTree(list)) {
            if ("new-integration@example.com".equals(user.get("email").asText())) id = user.get("id").asLong();
        }
        org.junit.jupiter.api.Assertions.assertTrue(id > 0);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/users/" + id)
                .session(session).with(csrf()).contentType("application/json").content(body.replace("Person", "Updated")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/users/" + id).session(session)).andExpect(jsonPath("$.lastName").value("Updated"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/users/" + id)
                .session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/users/" + id).session(session)).andExpect(status().isNotFound());
    }
}
