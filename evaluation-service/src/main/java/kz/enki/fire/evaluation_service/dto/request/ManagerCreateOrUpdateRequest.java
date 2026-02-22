package kz.enki.fire.evaluation_service.dto.request;

import lombok.Data;

@Data
public class ManagerCreateOrUpdateRequest {
    private String fullName;
    private String position;
    private String officeName;
    private String officeCode;
    private String skills;
    private Integer activeTicketsCount;
}
