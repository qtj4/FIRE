package kz.enki.fire.evaluation_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "managers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Manager {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String position;

    @Column(name = "office_name")
    private String officeName;

    @Column(name = "office_code")
    private String officeCode;

    private String skills;

    @Column(name = "active_tickets_count")
    private Integer activeTicketsCount;
}
