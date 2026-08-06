package in.mapmytour.blog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_reader_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReaderPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "font_size_idx", nullable = false)
    @Builder.Default
    private int fontSizeIdx = 2;

    @Column(name = "font_stack", nullable = false)
    @Builder.Default
    private String fontStack = "sans";

    @Column(name = "theme", nullable = false)
    @Builder.Default
    private String theme = "light";

    @Column(name = "line_spacing", nullable = false)
    @Builder.Default
    private String lineSpacing = "normal";

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
