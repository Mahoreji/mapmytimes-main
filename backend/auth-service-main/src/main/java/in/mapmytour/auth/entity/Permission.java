package in.mapmytour.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Unique machine-readable code, e.g. ACCOUNTING_INVOICE_READ
     */
    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * Optional module / domain tag (e.g. ACCOUNTING, SALES)
     */
    @Column(length = 100)
    private String module;
}


