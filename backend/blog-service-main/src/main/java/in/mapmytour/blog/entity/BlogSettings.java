package in.mapmytour.blog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "blog_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String settingKey;

    @Column(columnDefinition = "TEXT")
    private String settingValue;
}