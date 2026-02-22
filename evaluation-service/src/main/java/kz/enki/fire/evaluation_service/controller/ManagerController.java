package kz.enki.fire.evaluation_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.enki.fire.evaluation_service.dto.request.ManagerCreateOrUpdateRequest;
import kz.enki.fire.evaluation_service.dto.response.ManagerResponse;
import kz.enki.fire.evaluation_service.service.ManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/managers")
@RequiredArgsConstructor
@Tag(name = "Managers", description = "CRUD for managers")
public class ManagerController {

    private final ManagerService managerService;

    @GetMapping
    @Operation(summary = "Get all managers")
    public List<ManagerResponse> getAll() {
        return managerService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get manager by id")
    public ResponseEntity<ManagerResponse> getById(@PathVariable Long id) {
        ManagerResponse m = managerService.findById(id);
        return m != null ? ResponseEntity.ok(m) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Create manager")
    public ResponseEntity<ManagerResponse> create(@RequestBody ManagerCreateOrUpdateRequest request) {
        ManagerResponse created = managerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update manager")
    public ResponseEntity<ManagerResponse> update(@PathVariable Long id, @RequestBody ManagerCreateOrUpdateRequest request) {
        ManagerResponse updated = managerService.update(id, request);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete manager")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return managerService.deleteById(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
