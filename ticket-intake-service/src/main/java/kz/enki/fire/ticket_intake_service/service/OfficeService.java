package kz.enki.fire.ticket_intake_service.service;

import kz.enki.fire.ticket_intake_service.dto.request.OfficeCsvRequest;
import kz.enki.fire.ticket_intake_service.model.Office;
import kz.enki.fire.ticket_intake_service.repository.OfficeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OfficeService {
    private final OfficeRepository officeRepository;

    @Transactional
    public void saveOffices(List<OfficeCsvRequest> requests) {
        List<Office> offices = requests.stream()
                .filter(req -> req.getName() != null && !req.getName().isBlank())
                .map(req -> Office.builder()
                        .name(req.getName())
                        .address(req.getAddress())
                        .build())
                .toList();
        officeRepository.saveAll(offices);
    }
}
