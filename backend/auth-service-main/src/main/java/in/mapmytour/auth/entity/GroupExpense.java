package in.mapmytour.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private TravelGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy; // Who paid for this expense

    @Column(nullable = false, length = 200)
    private String description; // e.g., "Hotel booking", "Dinner at restaurant"

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 50)
    private String category; // FOOD, ACCOMMODATION, TRANSPORT, ACTIVITY, OTHER

    @Column(nullable = false)
    private LocalDateTime expenseDate;

    @Column(length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, SETTLED, CANCELLED

    @Column(length = 200)
    private String receiptUrl; // Receipt/document URL

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

