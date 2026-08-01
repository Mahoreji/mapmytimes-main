package in.mapmytour.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String roleType; // AGENT, SUPPLIER

    @Column(nullable = false)
    private String fieldName; // e.g. gstin, pan, bankStatement

    @Column(nullable = false)
    @Builder.Default
    private Boolean isMandatory = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAutomated = false;

    @Enumerated(EnumType.STRING)
    private AutomationType automationType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum AutomationType {
        REGEX,
        EXTERNAL_API,
        DOCUMENT_AI
    }
}
