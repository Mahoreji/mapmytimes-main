package in.mapmytour.auth.client;

import in.mapmytour.auth.dto.client.CreateAgentRequest;
import in.mapmytour.auth.dto.client.UpdateAgentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "agent-service", url = "${application.agent-service.url:http://localhost:8094}")
public interface AgentServiceClient {

        @PostMapping("/api/v1/agent")
        ResponseEntity<Map<String, Object>> createAgent(
                        @RequestBody CreateAgentRequest request,
                        @RequestHeader("Authorization") String token);

        @GetMapping("/api/v1/agents/email/{email}")
        ResponseEntity<Map<String, Object>> getAgentByEmail(
                        @PathVariable("email") String email,
                        @RequestHeader("Authorization") String token);

        @PutMapping("/api/v1/agents/{id}")
        ResponseEntity<Map<String, Object>> updateAgent(
                        @PathVariable("id") UUID id,
                        @RequestBody UpdateAgentRequest request,
                        @RequestHeader("Authorization") String token);

        @DeleteMapping("/api/v1/agents/{id}")
        ResponseEntity<Void> deleteAgent(
                        @PathVariable("id") UUID id,
                        @RequestHeader("Authorization") String token);
}
