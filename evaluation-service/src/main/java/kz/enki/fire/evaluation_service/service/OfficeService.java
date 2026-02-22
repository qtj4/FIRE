package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.request.OfficeCsvRequest;
import kz.enki.fire.evaluation_service.model.Office;
import kz.enki.fire.evaluation_service.repository.OfficeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OfficeService {
    private final OfficeRepository officeRepository;

    @Transactional
    public void saveOffices(List<OfficeCsvRequest> requests) {
        List<Office> offices = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            OfficeCsvRequest req = requests.get(i);
            String name = req.resolveName();
            if (name == null || name.isBlank()) {
                continue;
            }

            String address = req.resolveAddress();
            if (address == null || address.isBlank()) {
                int csvLine = i + 2; // + header
                throw new IllegalArgumentException(
                        "Office CSV validation failed at line " + csvLine
                                + ": address is required for office '" + name + "'. "
                                + "Use header Адрес or address"
                );
            }

            offices.add(Office.builder()
                    .name(name)
                    .address(address)
                    .latitude(req.resolveLatitude())
                    .longitude(req.resolveLongitude())
                    .build());
        }

        officeRepository.saveAll(offices);
    }
}
