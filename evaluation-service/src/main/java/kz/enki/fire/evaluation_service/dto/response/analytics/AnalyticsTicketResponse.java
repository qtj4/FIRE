package kz.enki.fire.evaluation_service.dto.response.analytics;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AnalyticsTicketResponse {
    String id;
    String clientId;
    String gender;
    String birthDate;
    String segment;
    String description;
    String type;
    Integer priority;
    List<String> attachments;
    String country;
    String region;
    String city;
    String street;
    String house;
    String office;
    String language;
    String sentiment;
    String summary;
    Double latitude;
    Double longitude;
    String assignedManager;
    String createdAt;
}
