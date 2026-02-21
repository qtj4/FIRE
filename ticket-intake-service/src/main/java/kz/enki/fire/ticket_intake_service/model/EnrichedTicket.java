package kz.enki.fire.ticket_intake_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "enriched_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrichedTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "raw_ticket_id", nullable = false)
    private RawTicket rawTicket;

    @Column(name = "client_guid")
    private UUID clientGuid;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "detected_language")
    private String detectedLanguage;

    private String sentiment;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @ManyToOne
    @JoinColumn(name = "assigned_office_id")
    private Office assignedOffice;

    @Column(name = "enriched_at")
    private LocalDateTime enrichedAt;

    @PrePersist
    protected void onEnrich() {
        enrichedAt = LocalDateTime.now();
    }
}
