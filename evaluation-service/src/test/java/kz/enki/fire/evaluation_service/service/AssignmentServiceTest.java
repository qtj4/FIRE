package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.kafka.EnrichedTicketEvent;
import kz.enki.fire.evaluation_service.mapper.EnrichedTicketMapper;
import kz.enki.fire.evaluation_service.model.EnrichedTicket;
import kz.enki.fire.evaluation_service.model.Manager;
import kz.enki.fire.evaluation_service.model.Office;
import kz.enki.fire.evaluation_service.model.RawTicket;
import kz.enki.fire.evaluation_service.repository.EnrichedTicketRepository;
import kz.enki.fire.evaluation_service.repository.ManagerRepository;
import kz.enki.fire.evaluation_service.repository.OfficeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private EnrichedTicketRepository enrichedTicketRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private EnrichedTicketMapper enrichedTicketMapper;

    @InjectMocks
    private AssignmentService assignmentService;

    private EnrichedTicket ticket;
    private RawTicket rawTicket;
    private Office officeAstana;
    private Office officeAlmaty;
    private Manager manager;

    @BeforeEach
    void setUp() {
        rawTicket = RawTicket.builder()
                .id(1L)
                .clientGuid(UUID.randomUUID())
                .build();

        ticket = EnrichedTicket.builder()
                .id(1L)
                .rawTicket(rawTicket)
                .type("обычный")
                .priority(5)
                .language("RU")
                .build();

        officeAstana = Office.builder()
                .id(1L)
                .name("Астана")
                .address("ул. Тест")
                .latitude(new BigDecimal("51.1694"))
                .longitude(new BigDecimal("71.4491"))
                .build();

        officeAlmaty = Office.builder()
                .id(2L)
                .name("Алматы")
                .address("ул. Тест 2")
                .latitude(new BigDecimal("43.2389"))
                .longitude(new BigDecimal("76.9457"))
                .build();

        manager = Manager.builder()
                .id(1L)
                .fullName("Иванов И.И.")
                .officeName("Астана")
                .position("менеджер")
                .skills("RU")
                .activeTicketsCount(0)
                .build();

        lenient().when(enrichedTicketMapper.toEvent(any(EnrichedTicket.class)))
                .thenAnswer(invocation -> {
                    EnrichedTicket source = invocation.getArgument(0);
                    return EnrichedTicketEvent.builder()
                            .enrichedTicketId(source.getId())
                            .clientGuid(source.getClientGuid())
                            .type(source.getType())
                            .priority(source.getPriority())
                            .language(source.getLanguage())
                            .sentiment(source.getSentiment())
                            .latitude(source.getLatitude())
                            .longitude(source.getLongitude())
                            .build();
                });
    }

    @Nested
    @DisplayName("assignManager - успешное назначение")
    class SuccessfulAssignment {

        @Test
        @DisplayName("назначает менеджера при наличии координат - выбирает ближайший офис")
        void assignsManagerWithCoordinates_selectsNearestOffice() {
            ticket.setLatitude(new BigDecimal("51.17"));
            ticket.setLongitude(new BigDecimal("71.45"));

            when(enrichedTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(officeRepository.findAll()).thenReturn(List.of(officeAstana, officeAlmaty));
            when(managerRepository.findByOfficeName("Астана")).thenReturn(List.of(manager));

            assignmentService.assignManager(1L);

            ArgumentCaptor<EnrichedTicket> ticketCaptor = ArgumentCaptor.forClass(EnrichedTicket.class);
            verify(enrichedTicketRepository).save(ticketCaptor.capture());
            assertThat(ticketCaptor.getValue().getAssignedOffice().getName()).isEqualTo("Астана");
            assertThat(ticketCaptor.getValue().getAssignedManager().getFullName()).isEqualTo("Иванов И.И.");

            ArgumentCaptor<Manager> managerCaptor = ArgumentCaptor.forClass(Manager.class);
            verify(managerRepository).save(managerCaptor.capture());
            assertThat(managerCaptor.getValue().getActiveTicketsCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("назначает менеджера без координат - splitUnknownAddress")
        void assignsManagerWithoutCoordinates_usesSplitUnknownAddress() {
            when(enrichedTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(officeRepository.findAll()).thenReturn(List.of(officeAstana, officeAlmaty));
            when(managerRepository.findByOfficeName(any())).thenReturn(List.of(manager));

            assignmentService.assignManager(1L);

            verify(enrichedTicketRepository).save(any(EnrichedTicket.class));
            verify(managerRepository).save(any(Manager.class));
        }

        @Test
        @DisplayName("распределяет зарубежные обращения по правилу 50/50 между Астаной и Алматы")
        void splitsForeignCountryBetweenAstanaAndAlmaty() {
            rawTicket.setCountry("Россия");

            Manager almatyManager = Manager.builder()
                    .id(2L)
                    .fullName("Алматы менеджер")
                    .officeName("Алматы")
                    .position("менеджер")
                    .skills("RU")
                    .activeTicketsCount(0)
                    .build();

            when(enrichedTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(officeRepository.findAll()).thenReturn(List.of(officeAstana, officeAlmaty));
            when(managerRepository.findByOfficeName("Астана")).thenReturn(List.of(manager));
            when(managerRepository.findByOfficeName("Алматы")).thenReturn(List.of(almatyManager));

            assignmentService.assignManager(1L);

            ArgumentCaptor<EnrichedTicket> ticketCaptor = ArgumentCaptor.forClass(EnrichedTicket.class);
            verify(enrichedTicketRepository).save(ticketCaptor.capture());
            assertThat(ticketCaptor.getValue().getAssignedOffice().getName())
                    .isIn("Астана", "Алматы");
        }

        @Test
        @DisplayName("фильтрует менеджеров по VIP для типа vip")
        void filtersManagersByVipForVipType() {
            ticket.setType("vip");
            when(enrichedTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(officeRepository.findAll()).thenReturn(List.of(officeAstana));
            Manager vipManager = Manager.builder()
                    .id(2L)
                    .fullName("VIP Менеджер")
                    .officeName("Астана")
                    .skills("VIP,RU")
                    .activeTicketsCount(0)
                    .build();
            when(managerRepository.findByOfficeName("Астана")).thenReturn(List.of(manager, vipManager));

            assignmentService.assignManager(1L);

            ArgumentCaptor<EnrichedTicket> captor = ArgumentCaptor.forClass(EnrichedTicket.class);
            verify(enrichedTicketRepository).save(captor.capture());
            assertThat(captor.getValue().getAssignedManager().getSkills()).contains("VIP");
        }

        @Test
        @DisplayName("фильтрует менеджеров по языку - RU всегда проходит")
        void allowsRuLanguageWithoutSkill() {
            ticket.setLanguage("RU");
            when(enrichedTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(officeRepository.findAll()).thenReturn(List.of(officeAstana));
            when(managerRepository.findByOfficeName("Астана")).thenReturn(List.of(manager));

            assignmentService.assignManager(1L);

            verify(enrichedTicketRepository).save(any(EnrichedTicket.class));
        }

        @Test
        @DisplayName("фильтрует менеджеров по языку - KZ требует навык")
        void requiresLanguageSkillForNonRu() {
            ticket.setLanguage("KZ");
            Manager kzManager = Manager.builder()
                    .id(2L)
                    .fullName("KZ Менеджер")
                    .officeName("Астана")
                    .skills("KZ,RU")
                    .activeTicketsCount(0)
                    .build();
            when(enrichedTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(officeRepository.findAll()).thenReturn(List.of(officeAstana));
            when(managerRepository.findByOfficeName("Астана")).thenReturn(List.of(manager, kzManager));

            assignmentService.assignManager(1L);

            ArgumentCaptor<EnrichedTicket> captor = ArgumentCaptor.forClass(EnrichedTicket.class);
            verify(enrichedTicketRepository).save(captor.capture());
            assertThat(captor.getValue().getAssignedManager().getSkills()).contains("KZ");
        }

        @Test
        @DisplayName("фильтрует менеджеров по должности для типа смена данных")
        void filtersByPositionForDataChangeType() {
            ticket.setType("смена данных");
            Manager chiefManager = Manager.builder()
                    .id(2L)
                    .fullName("Главный менеджер")
                    .officeName("Астана")
                    .position("главный специалист")
                    .skills("RU")
                    .activeTicketsCount(0)
                    .build();
            when(enrichedTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(officeRepository.findAll()).thenReturn(List.of(officeAstana));
            when(managerRepository.findByOfficeName("Астана")).thenReturn(List.of(manager, chiefManager));

            assignmentService.assignManager(1L);

            ArgumentCaptor<EnrichedTicket> captor = ArgumentCaptor.forClass(EnrichedTicket.class);
            verify(enrichedTicketRepository).save(captor.capture());
            assertThat(captor.getValue().getAssignedManager().getPosition()).contains("глав");
        }
    }

    @Nested
    @DisplayName("assignManager - идемпотентность и граничные случаи")
    class IdempotencyAndEdgeCases {

        @Test
        @DisplayName("не перезаписывает при уже назначенном менеджере")
        void doesNotOverwriteWhenAlreadyAssigned() {
            ticket.setAssignedManager(manager);
            ticket.setAssignedOffice(officeAstana);
            when(enrichedTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            assignmentService.assignManager(1L);

            verify(enrichedTicketRepository, never()).save(any());
            verify(managerRepository, never()).save(any());
        }

        @Test
        @DisplayName("бросает исключение при отсутствии тикета")
        void throwsWhenTicketNotFound() {
            when(enrichedTicketRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> assignmentService.assignManager(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Enriched ticket not found: 999");
        }

        @Test
        @DisplayName("не падает при отсутствии офисов")
        void doesNothingWhenNoOfficeFound() {
            when(enrichedTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(officeRepository.findAll()).thenReturn(List.of());

            assignmentService.assignManager(1L);

            verify(enrichedTicketRepository, never()).save(any());
            verify(managerRepository, never()).save(any());
        }

        @Test
        @DisplayName("не падает при отсутствии подходящих менеджеров")
        void doesNothingWhenNoSuitableManagers() {
            when(enrichedTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(officeRepository.findAll()).thenReturn(List.of(officeAstana));
            when(managerRepository.findByOfficeName("Астана")).thenReturn(List.of());

            assignmentService.assignManager(1L);

            verify(enrichedTicketRepository, never()).save(any());
            verify(managerRepository, never()).save(any());
        }

        @Test
        @DisplayName("VIP требуется при priority >= 8")
        void requiresVipForHighPriority() {
            ticket.setPriority(8);
            ticket.setType("обычный");
            Manager vipManager = Manager.builder()
                    .id(2L)
                    .fullName("VIP")
                    .officeName("Астана")
                    .skills("VIP,RU")
                    .activeTicketsCount(0)
                    .build();
            when(enrichedTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(officeRepository.findAll()).thenReturn(List.of(officeAstana));
            when(managerRepository.findByOfficeName("Астана")).thenReturn(List.of(manager, vipManager));

            assignmentService.assignManager(1L);

            ArgumentCaptor<EnrichedTicket> captor = ArgumentCaptor.forClass(EnrichedTicket.class);
            verify(enrichedTicketRepository).save(captor.capture());
            assertThat(captor.getValue().getAssignedManager().getSkills()).contains("VIP");
        }
    }
}
