package kz.enki.fire.evaluation_service.consumer;

import kz.enki.fire.evaluation_service.dto.kafka.EnrichedTicketEvent;
import kz.enki.fire.evaluation_service.service.AssignmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrichedTicketConsumerTest {

    @Mock
    private AssignmentService assignmentService;

    @InjectMocks
    private EnrichedTicketConsumer consumer;

    @Test
    @DisplayName("вызывает assignManager при валидном событии")
    void consume_validEvent_callsAssignManager() {
        EnrichedTicketEvent event = EnrichedTicketEvent.builder()
                .enrichedTicketId(42L)
                .build();

        consumer.consume(event);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(assignmentService).assignManager(captor.capture());
        assertThat(captor.getValue()).isEqualTo(42L);
    }

    @Test
    @DisplayName("игнорирует null событие")
    void consume_nullEvent_doesNothing() {
        consumer.consume(null);

        verifyNoInteractions(assignmentService);
    }

    @Test
    @DisplayName("игнорирует событие с null enrichedTicketId")
    void consume_eventWithNullId_doesNothing() {
        EnrichedTicketEvent event = EnrichedTicketEvent.builder()
                .enrichedTicketId(null)
                .build();

        consumer.consume(event);

        verifyNoInteractions(assignmentService);
    }

    @Test
    @DisplayName("не пробрасывает исключение при ошибке в assignManager")
    void consume_exceptionInAssignment_swallowsException() {
        EnrichedTicketEvent event = EnrichedTicketEvent.builder()
                .enrichedTicketId(1L)
                .build();
        doThrow(new RuntimeException("DB error")).when(assignmentService).assignManager(1L);

        consumer.consume(event);

        verify(assignmentService).assignManager(1L);
        // исключение не должно пробрасываться
    }
}
