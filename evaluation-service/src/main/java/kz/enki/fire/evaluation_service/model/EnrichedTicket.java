package kz.enki.fire.evaluation_service.model;

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

    @Column(name = "client_guid")
    private UUID clientGuid;

    @Column(name = "raw_ticket_id")
    private Long rawTicketId;

    @Column(name = "type")
    private String type;

    private Integer priority;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "detected_language")
    private String language;

    private String sentiment;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Column(name = "geo_normalized", length = 512)
    private String geoNormalized;

    @ManyToOne
    @JoinColumn(name = "assigned_office_id")
    private Office assignedOffice;

    @ManyToOne
    @JoinColumn(name = "assigned_manager_id")
    private Manager assignedManager;

    @Column(name = "enriched_at")
    private LocalDateTime enrichedAt;

    @PrePersist
    protected void onEnrich() {
        enrichedAt = LocalDateTime.now();
    }
}
