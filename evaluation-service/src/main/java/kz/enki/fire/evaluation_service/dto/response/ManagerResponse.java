package kz.enki.fire.evaluation_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManagerResponse {
    private Long id;
    private String fullName;
    private String position;
    private String officeName;
    private String officeCode;
    private String skills;
    private Integer activeTicketsCount;
}
