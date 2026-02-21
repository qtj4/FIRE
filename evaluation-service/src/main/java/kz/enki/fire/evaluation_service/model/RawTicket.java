package kz.enki.fire.evaluation_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "raw_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_guid", nullable = false)
    private UUID clientGuid;

    @Column(name = "client_gender")
    private String clientGender;

    @Column(name = "birth_date")
    private LocalDateTime birthDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String attachments;

    @Column(name = "client_segment")
    private String clientSegment;

    private String country;
    private String region;
    private String city;
    private String street;

    @Column(name = "house_number")
    private String houseNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
