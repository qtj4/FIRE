package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.request.ManagerCsvRequest;
import kz.enki.fire.evaluation_service.model.Manager;
import kz.enki.fire.evaluation_service.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerService {
    private final ManagerRepository managerRepository;

    @Transactional
    public void saveManagers(List<ManagerCsvRequest> requests) {
        List<Manager> managers = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            ManagerCsvRequest req = requests.get(i);
            String fullName = req.resolveFullName();
            if (fullName == null || fullName.isBlank()) {
                int csvLine = i + 2; // + header
                throw new IllegalArgumentException(
                        "Manager CSV validation failed at line " + csvLine
                                + ": required column with manager name is empty. "
                                + "Use one of headers: ФИО, full_name, fullName, name"
                );
            }

            Integer activeTickets = req.resolveActiveTicketsCount();
            managers.add(Manager.builder()
                    .fullName(fullName)
                    .position(req.resolvePosition())
                    .officeName(req.resolveOfficeName())
                    .skills(req.resolveSkills())
                    .activeTicketsCount(activeTickets != null ? activeTickets : 0)
                    .build());
        }

        managerRepository.saveAll(managers);
    }
}
