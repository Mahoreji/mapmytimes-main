package in.mapmytour.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.mapmytour.auth.dto.auth.SendOtpRequest;
import in.mapmytour.auth.dto.auth.MessageResponse;
import in.mapmytour.auth.service.AuthService;
import in.mapmytour.auth.service.UserContextService;
import in.mapmytour.auth.utils.SignatureUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class, properties = "app.security.gateway-only.enabled=false")
class AuthControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserContextService userContextService;

    @MockBean
    private SignatureUtils signatureUtils;

    @Test
    @WithMockUser
    void sendOtp_WithAcceptJson_ReturnsJson() throws Exception {
        SendOtpRequest request = new SendOtpRequest();
        request.setEmail("test@example.com");

        MessageResponse mockResponse = new MessageResponse("OTP sent successfully");
        Mockito.when(authService.sendOtp(anyString())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/send-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser
    void sendOtp_WithAcceptJavascript_IsOverriddenToJson() throws Exception {
        SendOtpRequest request = new SendOtpRequest();
        request.setEmail("test@example.com");

        MessageResponse mockResponse = new MessageResponse("OTP sent successfully");
        Mockito.when(authService.sendOtp(anyString())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/send-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept("application/javascript"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser
    void sendOtp_ErrorResponse_ReturnsJson() throws Exception {
        SendOtpRequest request = new SendOtpRequest();
        request.setEmail("test@example.com");

        Mockito.when(authService.sendOtp(anyString())).thenThrow(new RuntimeException("Service failure"));

        mockMvc.perform(post("/api/v1/auth/send-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept("application/javascript"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser
    void globalExceptionHandler_ValidationFailure_ReturnsJson() throws Exception {
        mockMvc.perform(post("/api/v1/auth/send-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .accept("application/javascript"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
