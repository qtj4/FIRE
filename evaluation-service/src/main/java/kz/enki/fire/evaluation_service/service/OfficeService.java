package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.request.OfficeCreateOrUpdateRequest;
import kz.enki.fire.evaluation_service.dto.request.OfficeCsvRequest;
import kz.enki.fire.evaluation_service.dto.response.OfficeResponse;
import kz.enki.fire.evaluation_service.model.Office;
import kz.enki.fire.evaluation_service.repository.OfficeRepository;
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
                        .code(req.getCode() != null && !req.getCode().isBlank() ? req.getCode() : toOfficeCode(req.getName()))
                        .name(req.getName())
                        .address(req.getAddress() != null ? req.getAddress() : "")
                        .latitude(req.getLatitude())
                        .longitude(req.getLongitude())
                        .build())
                .toList();
        officeRepository.saveAll(offices);
    }

    public List<OfficeResponse> findAll() {
        return officeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public OfficeResponse findById(Long id) {
        return officeRepository.findById(id)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public OfficeResponse create(OfficeCreateOrUpdateRequest req) {
        String code = req.getCode() != null && !req.getCode().isBlank() ? req.getCode() : toOfficeCode(req.getName());
        Office office = Office.builder()
                .code(code)
                .name(req.getName())
                .address(req.getAddress() != null ? req.getAddress() : "")
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .build();
        office = officeRepository.save(office);
        return toResponse(office);
    }

    @Transactional
    public OfficeResponse update(Long id, OfficeCreateOrUpdateRequest req) {
        Office office = officeRepository.findById(id).orElse(null);
        if (office == null) return null;
        if (req.getCode() != null && !req.getCode().isBlank()) office.setCode(req.getCode());
        if (req.getName() != null) office.setName(req.getName());
        if (req.getAddress() != null) office.setAddress(req.getAddress());
        if (req.getLatitude() != null) office.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) office.setLongitude(req.getLongitude());
        office = officeRepository.save(office);
        return toResponse(office);
    }

    @Transactional
    public boolean deleteById(Long id) {
        if (!officeRepository.existsById(id)) return false;
        officeRepository.deleteById(id);
        return true;
    }

    private OfficeResponse toResponse(Office o) {
        return OfficeResponse.builder()
                .id(o.getId())
                .code(o.getCode())
                .name(o.getName())
                .address(o.getAddress())
                .latitude(o.getLatitude())
                .longitude(o.getLongitude())
                .build();
    }

    private static String toOfficeCode(String name) {
        if (name == null || name.isBlank()) return "O";
        String s = name.replaceAll("[^a-zA-Zа-яА-ЯёЁ0-9]", "").toUpperCase();
        return s.length() > 20 ? s.substring(0, 20) : (s.isEmpty() ? "O" : s);
    }
}
