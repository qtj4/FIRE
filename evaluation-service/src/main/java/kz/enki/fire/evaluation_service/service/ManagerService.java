package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.request.ManagerCreateOrUpdateRequest;
import kz.enki.fire.evaluation_service.dto.request.ManagerCsvRequest;
import kz.enki.fire.evaluation_service.dto.response.ManagerResponse;
import kz.enki.fire.evaluation_service.model.Manager;
import kz.enki.fire.evaluation_service.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerService {
    private final ManagerRepository managerRepository;

    @Transactional
    public void saveManagers(List<ManagerCsvRequest> requests) {
        List<Manager> managers = requests.stream()
                .map(req -> Manager.builder()
                        .fullName(req.getFullName())
                        .position(req.getPosition())
                        .officeName(req.getOfficeName())
                        .officeCode(req.getOfficeCode())
                        .skills(req.getSkills())
                        .activeTicketsCount(req.getActiveTicketsCount() != null ? req.getActiveTicketsCount() : 0)
                        .build())
                .toList();
        managerRepository.saveAll(managers);
    }

    public List<ManagerResponse> findAll() {
        return managerRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ManagerResponse findById(Long id) {
        return managerRepository.findById(id)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public ManagerResponse create(ManagerCreateOrUpdateRequest req) {
        Manager manager = Manager.builder()
                .fullName(req.getFullName() != null ? req.getFullName() : "")
                .position(req.getPosition())
                .officeName(req.getOfficeName())
                .officeCode(req.getOfficeCode())
                .skills(req.getSkills())
                .activeTicketsCount(req.getActiveTicketsCount() != null ? req.getActiveTicketsCount() : 0)
                .build();
        manager = managerRepository.save(manager);
        return toResponse(manager);
    }

    @Transactional
    public ManagerResponse update(Long id, ManagerCreateOrUpdateRequest req) {
        Manager manager = managerRepository.findById(id).orElse(null);
        if (manager == null) return null;
        if (req.getFullName() != null) manager.setFullName(req.getFullName());
        if (req.getPosition() != null) manager.setPosition(req.getPosition());
        if (req.getOfficeName() != null) manager.setOfficeName(req.getOfficeName());
        if (req.getOfficeCode() != null) manager.setOfficeCode(req.getOfficeCode());
        if (req.getSkills() != null) manager.setSkills(req.getSkills());
        if (req.getActiveTicketsCount() != null) manager.setActiveTicketsCount(req.getActiveTicketsCount());
        manager = managerRepository.save(manager);
        return toResponse(manager);
    }

    @Transactional
    public boolean deleteById(Long id) {
        if (!managerRepository.existsById(id)) return false;
        managerRepository.deleteById(id);
        return true;
    }

    private ManagerResponse toResponse(Manager m) {
        return ManagerResponse.builder()
                .id(m.getId())
                .fullName(m.getFullName())
                .position(m.getPosition())
                .officeName(m.getOfficeName())
                .officeCode(m.getOfficeCode())
                .skills(m.getSkills())
                .activeTicketsCount(m.getActiveTicketsCount())
                .build();
    }
}
