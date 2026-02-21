package kz.enki.fire.evaluation_service;

import kz.enki.fire.evaluation_service.model.EnrichedTicket;
import kz.enki.fire.evaluation_service.model.Manager;
import kz.enki.fire.evaluation_service.model.Office;
import kz.enki.fire.evaluation_service.model.RawTicket;
import kz.enki.fire.evaluation_service.repository.EnrichedTicketRepository;
import kz.enki.fire.evaluation_service.repository.ManagerRepository;
import kz.enki.fire.evaluation_service.repository.OfficeRepository;
import kz.enki.fire.evaluation_service.repository.RawTicketRepository;
import kz.enki.fire.evaluation_service.consumer.EnrichedTicketConsumer;
import kz.enki.fire.evaluation_service.service.AssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест полного цикла: создание офисов, менеджеров, тикета и назначение менеджера.
 */
@SpringBootTest
@ActiveProfiles("test")
class EvaluationServiceIntegrationTest {

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    @Autowired
    private RawTicketRepository rawTicketRepository;

    @Autowired
    private EnrichedTicketRepository enrichedTicketRepository;

    @Autowired
    private AssignmentService assignmentService;

    @MockBean
    private EnrichedTicketConsumer enrichedTicketConsumer;

    @BeforeEach
    void setUp() {
        enrichedTicketRepository.deleteAll();
        managerRepository.deleteAll();
        officeRepository.deleteAll();
        rawTicketRepository.deleteAll();
    }

    @Test
    @DisplayName("полный цикл: офис + менеджер + тикет -> assignManager назначает менеджера")
    void fullFlow_assignsManager() {
        Office office = officeRepository.save(Office.builder()
                .name("Астана")
                .address("ул. Тест")
                .latitude(new BigDecimal("51.1694"))
                .longitude(new BigDecimal("71.4491"))
                .build());

        Manager manager = managerRepository.save(Manager.builder()
                .fullName("Иванов И.И.")
                .officeName("Астана")
                .position("менеджер")
                .skills("RU")
                .activeTicketsCount(0)
                .build());

        RawTicket raw = rawTicketRepository.save(RawTicket.builder()
                .clientGuid(UUID.randomUUID())
                .build());

        EnrichedTicket ticket = enrichedTicketRepository.save(EnrichedTicket.builder()
                .rawTicket(raw)
                .type("обычный")
                .priority(5)
                .language("RU")
                .latitude(new BigDecimal("51.17"))
                .longitude(new BigDecimal("71.45"))
                .build());

        assignmentService.assignManager(ticket.getId());

        EnrichedTicket updated = enrichedTicketRepository.findById(ticket.getId()).orElseThrow();
        assertThat(updated.getAssignedManager()).isNotNull();
        assertThat(updated.getAssignedManager().getFullName()).isEqualTo("Иванов И.И.");
        assertThat(updated.getAssignedOffice()).isNotNull();
        assertThat(updated.getAssignedOffice().getName()).isEqualTo("Астана");

        Manager updatedManager = managerRepository.findById(manager.getId()).orElseThrow();
        assertThat(updatedManager.getActiveTicketsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("повторный вызов assignManager не перезаписывает (идемпотентность)")
    void assignManager_idempotent() {
        Office office = officeRepository.save(Office.builder()
                .name("Астана")
                .address("ул. Тест")
                .latitude(new BigDecimal("51.1694"))
                .longitude(new BigDecimal("71.4491"))
                .build());

        Manager manager = managerRepository.save(Manager.builder()
                .fullName("Иванов")
                .officeName("Астана")
                .skills("RU")
                .activeTicketsCount(0)
                .build());

        RawTicket raw = rawTicketRepository.save(RawTicket.builder()
                .clientGuid(UUID.randomUUID())
                .build());

        EnrichedTicket ticket = enrichedTicketRepository.save(EnrichedTicket.builder()
                .rawTicket(raw)
                .type("обычный")
                .language("RU")
                .latitude(new BigDecimal("51.17"))
                .longitude(new BigDecimal("71.45"))
                .build());

        assignmentService.assignManager(ticket.getId());
        assignmentService.assignManager(ticket.getId());

        Manager updatedManager = managerRepository.findById(manager.getId()).orElseThrow();
        assertThat(updatedManager.getActiveTicketsCount()).isEqualTo(1);
    }
}
