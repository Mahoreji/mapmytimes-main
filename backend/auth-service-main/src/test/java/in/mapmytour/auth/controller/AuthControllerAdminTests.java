package in.mapmytour.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.mapmytour.auth.dto.auth.UserResponse;
import in.mapmytour.auth.service.AuthService;
import in.mapmytour.auth.service.UserContextService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
class AuthControllerAdminTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserContextService userContextService;

    @MockBean
    private in.mapmytour.auth.utils.SignatureUtils signatureUtils;

    @Test
    @DisplayName("GET /api/v1/auth/admin/users returns forbidden when current user is not admin")
    @WithMockUser(roles = "USER")
    void getAllUsers_forbiddenForNonAdmin() throws Exception {
        Mockito.when(userContextService.isCurrentUserAdmin(any())).thenReturn(false);

        mockMvc.perform(get("/api/v1/auth/admin/users")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Admin access required")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/admin/users returns paged users for admin")
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_returnsUsersForAdmin() throws Exception {
        Mockito.when(userContextService.isCurrentUserAdmin(any())).thenReturn(true);

        UserResponse user = new UserResponse();
        Page<UserResponse> page = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);

        Mockito.when(authService.getAllUsers(any(Pageable.class), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/auth/admin/users")
                .param("page", "0")
                .param("size", "20")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Users retrieved successfully")))
                .andExpect(jsonPath("$.data.content.length()", is(1)));
    }
}
