package in.mapmytour.api.controller;

import in.mapmytour.api.dto.APIResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class IndexController {

    @GetMapping("/")
    public Mono<ResponseEntity<APIResponse<Map<String, String>>>> index() {
        Map<String, String> data = Map.of(
            "name", "MapMyTimes API Gateway",
            "status", "UP",
            "message", "Gateway is operational and ready for adventure!"
        );
        
        APIResponse<Map<String, String>> response = APIResponse.<Map<String, String>>builder()
                .success(true)
                .statusCode(200)
                .message("Welcome to MapMyTimes API")
                .data(data)
                .build();
                
        return Mono.just(ResponseEntity.ok(response));
    }
}
