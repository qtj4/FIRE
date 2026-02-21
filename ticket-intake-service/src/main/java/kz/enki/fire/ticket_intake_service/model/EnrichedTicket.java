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

    @Column(name = "type_")
    private String type;

    private Integer priority;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "language_")
    private String language;

    private String sentiment;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private LocalDateTime enrichedAt;

    @Column(name = "assigned_manager_id")
    private Long assignedManagerId;

    @Column(name = "assigned_manager_name")
    private String assignedManagerName;

    @Column(name = "assigned_office_id")
    private Long assignedOfficeId;

    @Column(name = "assigned_office_name")
    private String assignedOfficeName;

    @Column(name = "assignment_status")
    private String assignmentStatus;

    @Column(name = "assignment_message")
    private String assignmentMessage;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @PrePersist
    protected void onEnrich() {
        enrichedAt = LocalDateTime.now();
    }
}
