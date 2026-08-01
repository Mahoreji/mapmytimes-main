package in.mapmytour.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_feedbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String ticketId; // Optional, if related to a ticket

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private Integer rating; // 1-5 scale

    @Column(columnDefinition = "TEXT")
    private String comments;

    @CreationTimestamp
    private LocalDateTime submittedAt;

    @Column
    private boolean isFollowUpRequired;
}