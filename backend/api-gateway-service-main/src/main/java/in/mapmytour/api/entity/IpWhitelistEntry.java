package in.mapmytour.api.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import org.springframework.data.domain.Persistable;
import org.springframework.data.annotation.Transient;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * R2DBC entity representing a whitelisted admin IP address.
 *
 * Redis is used as the hot-path lookup cache (SecurityCacheService).
 * This table is the persistent source-of-truth, loaded into Redis on startup
 * and written back on every CRUD operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("gateway_ip_whitelist")
public class IpWhitelistEntry implements Persistable<String> {

    @Id
    private String id;

    @Transient
    @JsonIgnore
    private boolean isNew = false;

    @Override
    @JsonIgnore
    public boolean isNew() {
        return this.isNew || id == null;
    }

    @Column("ip_address")
    private String ipAddress;

    @Column("label")
    private String label;        // Human-readable note

    @Column("added_by")
    private String addedBy;      // Admin user_id

    @Column("expires_at")
    private LocalDateTime expiresAt;  // null = never expires

    @Column("is_active")
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
