package kz.enki.fire.evaluation_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.enki.fire.evaluation_service.dto.request.OfficeCreateOrUpdateRequest;
import kz.enki.fire.evaluation_service.dto.response.OfficeResponse;
import kz.enki.fire.evaluation_service.service.OfficeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/offices")
@RequiredArgsConstructor
@Tag(name = "Offices", description = "CRUD for offices")
public class OfficeController {

    private final OfficeService officeService;

    @GetMapping
    @Operation(summary = "Get all offices")
    public List<OfficeResponse> getAll() {
        return officeService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get office by id")
    public ResponseEntity<OfficeResponse> getById(@PathVariable Long id) {
        OfficeResponse o = officeService.findById(id);
        return o != null ? ResponseEntity.ok(o) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Create office")
    public ResponseEntity<OfficeResponse> create(@RequestBody OfficeCreateOrUpdateRequest request) {
        OfficeResponse created = officeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update office")
    public ResponseEntity<OfficeResponse> update(@PathVariable Long id, @RequestBody OfficeCreateOrUpdateRequest request) {
        OfficeResponse updated = officeService.update(id, request);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete office")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return officeService.deleteById(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
