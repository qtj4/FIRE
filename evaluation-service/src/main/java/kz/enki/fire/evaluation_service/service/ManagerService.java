package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.request.ManagerCsvRequest;
import kz.enki.fire.evaluation_service.model.Manager;
import kz.enki.fire.evaluation_service.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
                        .skills(req.getSkills())
                        .activeTicketsCount(req.getActiveTicketsCount() != null ? req.getActiveTicketsCount() : 0)
                        .build())
                .collect(Collectors.toList());
        managerRepository.saveAll(managers);
    }
}
