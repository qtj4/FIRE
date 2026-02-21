package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.request.OfficeCsvRequest;
import kz.enki.fire.evaluation_service.model.Office;
import kz.enki.fire.evaluation_service.repository.OfficeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OfficeServiceTest {

    @Mock
    private OfficeRepository officeRepository;

    @InjectMocks
    private OfficeService officeService;

    @Test
    @DisplayName("сохраняет офисы и фильтрует пустые имена")
    void saveOffices_filtersBlankNames() {
        OfficeCsvRequest valid = new OfficeCsvRequest();
        valid.setName("Астана");
        valid.setAddress("ул. Тест");
        valid.setLatitude(new BigDecimal("51.1694"));
        valid.setLongitude(new BigDecimal("71.4491"));

        OfficeCsvRequest blankName = new OfficeCsvRequest();
        blankName.setName("   ");
        blankName.setAddress("адрес");

        OfficeCsvRequest nullName = new OfficeCsvRequest();
        nullName.setAddress("адрес");

        List<OfficeCsvRequest> requests = List.of(valid, blankName, nullName);

        officeService.saveOffices(requests);

        ArgumentCaptor<List<Office>> captor = ArgumentCaptor.forClass(List.class);
        verify(officeRepository).saveAll(captor.capture());
        List<Office> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getName()).isEqualTo("Астана");
        assertThat(saved.get(0).getLatitude()).isEqualByComparingTo("51.1694");
    }

    @Test
    @DisplayName("маппит все поля корректно")
    void saveOffices_mapsAllFields() {
        OfficeCsvRequest req = new OfficeCsvRequest();
        req.setName("Алматы");
        req.setAddress("пр. Абая 1");
        req.setLatitude(new BigDecimal("43.2389"));
        req.setLongitude(new BigDecimal("76.9457"));

        officeService.saveOffices(List.of(req));

        ArgumentCaptor<List<Office>> captor = ArgumentCaptor.forClass(List.class);
        verify(officeRepository).saveAll(captor.capture());
        Office office = captor.getValue().get(0);
        assertThat(office.getName()).isEqualTo("Алматы");
        assertThat(office.getAddress()).isEqualTo("пр. Абая 1");
        assertThat(office.getLatitude()).isEqualByComparingTo("43.2389");
        assertThat(office.getLongitude()).isEqualByComparingTo("76.9457");
    }
}
