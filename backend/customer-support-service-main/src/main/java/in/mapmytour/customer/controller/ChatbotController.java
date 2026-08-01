package in.mapmytour.customer.controller;

import in.mapmytour.customer.dto.APIResponse;
import in.mapmytour.customer.dto.ChatbotRequest;
import in.mapmytour.customer.dto.ChatbotResponse;
import in.mapmytour.customer.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/chat")
    public ResponseEntity<APIResponse<ChatbotResponse>> chat(
            @Valid @RequestBody ChatbotRequest request) {
        
        log.info("Chatbot request from user: {}", request.getUserId());
        
        ChatbotResponse response = chatbotService.processMessage(request);
        
        return ResponseEntity.ok(APIResponse.<ChatbotResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Chatbot response generated")
                .data(response)
                .build());
    }

    @GetMapping("/suggestions")
    public ResponseEntity<APIResponse<ChatbotResponse.Suggestions>> getSuggestions(
            @RequestParam(required = false) String context) {
        
        ChatbotResponse.Suggestions suggestions = chatbotService.getSuggestions(context);
        
        return ResponseEntity.ok(APIResponse.<ChatbotResponse.Suggestions>builder()
                .success(true)
                .statusCode(200)
                .message("Suggestions retrieved")
                .data(suggestions)
                .build());
    }
}

