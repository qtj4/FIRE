package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.request.ManagerCsvRequest;
import kz.enki.fire.evaluation_service.model.Manager;
import kz.enki.fire.evaluation_service.repository.ManagerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private ManagerRepository managerRepository;

    @InjectMocks
    private ManagerService managerService;

    @Test
    @DisplayName("сохраняет менеджеров и маппит поля")
    void saveManagers_mapsCorrectly() {
        ManagerCsvRequest req = new ManagerCsvRequest();
        req.setFullName("Иванов И.И.");
        req.setPosition("менеджер");
        req.setOfficeName("Астана");
        req.setSkills("RU,VIP");
        req.setActiveTicketsCount(3);

        managerService.saveManagers(List.of(req));

        ArgumentCaptor<List<Manager>> captor = ArgumentCaptor.forClass(List.class);
        verify(managerRepository).saveAll(captor.capture());
        Manager manager = captor.getValue().get(0);
        assertThat(manager.getFullName()).isEqualTo("Иванов И.И.");
        assertThat(manager.getPosition()).isEqualTo("менеджер");
        assertThat(manager.getOfficeName()).isEqualTo("Астана");
        assertThat(manager.getSkills()).isEqualTo("RU,VIP");
        assertThat(manager.getActiveTicketsCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("дефолтит activeTicketsCount в 0 при null")
    void saveManagers_defaultsActiveTicketsCount() {
        ManagerCsvRequest req = new ManagerCsvRequest();
        req.setFullName("Петров");
        req.setOfficeName("Астана");
        req.setActiveTicketsCount(null);

        managerService.saveManagers(List.of(req));

        ArgumentCaptor<List<Manager>> captor = ArgumentCaptor.forClass(List.class);
        verify(managerRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getActiveTicketsCount()).isEqualTo(0);
    }
}
