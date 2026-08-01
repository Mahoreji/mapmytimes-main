package in.mapmytour.blog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_media")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private BlogPost post;

    @Column(nullable = false)
    private String mediaUrl;

    @Column
    private String mediaType; // image, video, etc.

    @Column(columnDefinition = "TEXT")
    private String caption; // Keep for backward compatibility

    @Column(columnDefinition = "TEXT")
    private String description; // Longer description for the image

    @Column(columnDefinition = "TEXT")
    private String subtitle; // Subtitle for the image group

    @Column
    private Integer subtitleGroupIndex; // Groups images that share the same subtitle (0, 1, 2, etc.)

    @Column(nullable = false)
    private String userId; // Reference to user-service who uploaded

    @CreationTimestamp
    private LocalDateTime uploadedAt;

    @Column
    private Integer displayOrder; // For ordering media in a post
}