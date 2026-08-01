package in.mapmytour.auth.client;

import in.mapmytour.auth.dto.client.CreateSupplierRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "supplier-service", url = "${application.supplier-service.url:http://localhost:8093}")
public interface SupplierServiceClient {

        @PostMapping("/api/v1/suppliers")
        ResponseEntity<Map<String, Object>> createSupplier(
                        @RequestBody CreateSupplierRequest request,
                        @RequestHeader("Authorization") String token);

        @GetMapping("/api/v1/suppliers/email/{email}")
        ResponseEntity<Map<String, Object>> getSupplierByEmail(
                        @PathVariable("email") String email,
                        @RequestHeader("Authorization") String token);

        @PutMapping("/api/v1/suppliers/{id}")
        ResponseEntity<Map<String, Object>> updateSupplier(
                        @PathVariable("id") java.util.UUID id,
                        @RequestBody in.mapmytour.auth.dto.client.UpdateSupplierRequest request,
                        @RequestHeader("Authorization") String token);

        @DeleteMapping("/api/v1/suppliers/{id}")
        ResponseEntity<Void> deleteSupplier(
                        @PathVariable("id") java.util.UUID id,
                        @RequestHeader("Authorization") String token);
}
