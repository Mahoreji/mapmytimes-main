package in.mapmytour.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@Table(name = "support_agents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSupportAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId; // Links to user auth system

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @ElementCollection
    private Set<String> skills; // e.g., ["TECHNICAL", "BILLING"]

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column
    private Integer maxActiveTickets; // Workload capacity
}