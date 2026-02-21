package kz.enki.fire.evaluation_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketContext {
    @NotBlank(message = "Text cannot be empty")
    private String text;
    
    @NotBlank(message = "Location cannot be empty")
    private String location;
    
    private String language;
}
