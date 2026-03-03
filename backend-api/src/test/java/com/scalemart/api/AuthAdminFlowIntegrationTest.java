package com.scalemart.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalemart.api.domain.User;
import com.scalemart.api.repository.RefreshTokenRepository;
import com.scalemart.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:scalemart-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "app.jwt.secret=c2NhbGVtYXJ0LXByb2plY3QtamRrMjEtc2VjdXJlLXNpZ25pbmcta2V5LTIwMjY=",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "app.auth.expose-debug-tokens=true",
    "app.mail.enabled=false",
    "management.health.mail.enabled=false"
})
class AuthAdminFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        seedAdminUser();
    }

    @Test
    void registerVerifyAndLoginFlowWorks() throws Exception {
        String username = uniqueUsername("flow");
        String password = "TempPass123!";

        String verificationToken = registerAndExtractToken(username, password);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("""
                    {"username":"%s","password":"%s"}
                    """.formatted(username, password))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("Email is not verified")));

        mockMvc.perform(post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("""
                    {"username":"%s","token":"%s"}
                    """.formatted(username, verificationToken))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(containsString("Email verified successfully")));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("""
                    {"username":"%s","password":"%s"}
                    """.formatted(username, password))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void resendVerificationAndChangePasswordFlowWorks() throws Exception {
        String username = uniqueUsername("secure");
        String oldPassword = "TempPass123!";
        String newPassword = "ChangedPass123!";

        registerAndExtractToken(username, oldPassword);

        MvcResult resendResult = mockMvc.perform(post("/api/v1/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("""
                    {"username":"%s"}
                    """.formatted(username))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.debugToken").isString())
            .andReturn();
        String resentToken = jsonField(resendResult, "debugToken");

        mockMvc.perform(post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("""
                    {"username":"%s","token":"%s"}
                    """.formatted(username, resentToken))))
            .andExpect(status().isOk());

        String accessToken = loginAndGetAccessToken(username, oldPassword);

        mockMvc.perform(post("/api/v1/auth/change-password")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("""
                    {"currentPassword":"%s","newPassword":"%s"}
                    """.formatted(oldPassword, newPassword))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(containsString("Password changed successfully")));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("""
                    {"username":"%s","password":"%s"}
                    """.formatted(username, oldPassword))))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("""
                    {"username":"%s","password":"%s"}
                    """.formatted(username, newPassword))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void adminUserUpdateCreatesAuditLogAndCsvExportWorks() throws Exception {
        String adminToken = loginAndGetAccessToken("admin@scalemart.dev", "AdminPass123!");
        User target = seedVerifiedUser(uniqueUsername("target"), "TargetPass123!");

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/role", target.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("""
                    {"role":"ADMIN"}
                    """)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/role", target.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("""
                    {"role":"USER"}
                    """)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("USER"));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                .header("Authorization", "Bearer " + adminToken)
                .param("page", "0")
                .param("size", "10")
                .param("target", target.getUsername())
                .param("action", "USER_ROLE_UPDATED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.content[0].action").value("USER_ROLE_UPDATED"));

        mockMvc.perform(get("/api/v1/admin/audit-logs/export")
                .header("Authorization", "Bearer " + adminToken)
                .param("limit", "20")
                .param("target", target.getUsername())
                .param("action", "USER_ROLE_UPDATED"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andExpect(content().string(containsString("id,createdAt,action,actorUsername,targetUsername,details")))
            .andExpect(content().string(containsString(target.getUsername())));
    }

    private String registerAndExtractToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("""
                    {"username":"%s","password":"%s"}
                    """.formatted(username, password))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.debugToken").isString())
            .andReturn();
        return jsonField(result, "debugToken");
    }

    private String loginAndGetAccessToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("""
                    {"username":"%s","password":"%s"}
                    """.formatted(username, password))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andReturn();
        return jsonField(result, "token");
    }

    private String jsonField(MvcResult result, String fieldName) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.path(fieldName).asText();
    }

    private String json(String value) {
        return value.replace("\n", "").replace("  ", "").trim();
    }

    private String uniqueUsername(String prefix) {
        return prefix + "-" + System.nanoTime() + "@scalemart.dev";
    }

    private void seedAdminUser() {
        User admin = new User();
        admin.setUsername("admin@scalemart.dev");
        admin.setPassword(passwordEncoder.encode("AdminPass123!"));
        admin.setEmail("admin@scalemart.dev");
        admin.setRole("ADMIN");
        admin.setEnabled(true);
        admin.setEmailVerified(true);
        admin.setEmailVerificationTokenHash(null);
        admin.setEmailVerificationExpiresAt(null);
        admin.setPasswordResetTokenHash(null);
        admin.setPasswordResetExpiresAt(null);
        userRepository.save(admin);
    }

    private User seedVerifiedUser(String username, String rawPassword) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(username);
        user.setRole("USER");
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationExpiresAt(null);
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        return userRepository.save(user);
    }
}
