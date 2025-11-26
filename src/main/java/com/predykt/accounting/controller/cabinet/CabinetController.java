package com.predykt.accounting.controller.cabinet;

import com.predykt.accounting.domain.entity.Cabinet;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.User;
import com.predykt.accounting.dto.CabinetDTO;
import com.predykt.accounting.service.CabinetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST pour la gestion des cabinets (MODE CABINET)
 */
@RestController
@RequestMapping("/api/cabinets")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('CABINET_MANAGER')")
public class CabinetController {

    private final CabinetService cabinetService;

    /**
     * Crée un nouveau cabinet
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Cabinet> createCabinet(@Valid @RequestBody CabinetDTO cabinetDTO) {
        log.info("Requête de création de cabinet: {}", cabinetDTO.getName());

        Cabinet cabinet = mapToEntity(cabinetDTO);
        Cabinet created = cabinetService.createCabinet(cabinet);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Met à jour un cabinet
     */
    @PutMapping("/{id}")
    public ResponseEntity<Cabinet> updateCabinet(
            @PathVariable Long id,
            @Valid @RequestBody CabinetDTO cabinetDTO) {
        log.info("Requête de mise à jour du cabinet ID: {}", id);

        Cabinet cabinet = mapToEntity(cabinetDTO);
        Cabinet updated = cabinetService.updateCabinet(id, cabinet);

        return ResponseEntity.ok(updated);
    }

    /**
     * Récupère un cabinet par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Cabinet> getCabinetById(@PathVariable Long id) {
        Cabinet cabinet = cabinetService.getCabinetById(id);
        return ResponseEntity.ok(cabinet);
    }

    /**
     * Récupère tous les cabinets
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Cabinet>> getAllCabinets() {
        List<Cabinet> cabinets = cabinetService.getAllCabinets();
        return ResponseEntity.ok(cabinets);
    }

    /**
     * Récupère un cabinet par nom
     */
    @GetMapping("/by-name/{name}")
    public ResponseEntity<Cabinet> getCabinetByName(@PathVariable String name) {
        Cabinet cabinet = cabinetService.getCabinetByName(name);
        return ResponseEntity.ok(cabinet);
    }

    /**
     * Récupère un cabinet par code
     */
    @GetMapping("/by-code/{code}")
    public ResponseEntity<Cabinet> getCabinetByCode(@PathVariable String code) {
        Cabinet cabinet = cabinetService.getCabinetByCode(code);
        return ResponseEntity.ok(cabinet);
    }

    /**
     * Récupère toutes les entreprises d'un cabinet
     */
    @GetMapping("/{id}/companies")
    public ResponseEntity<List<Company>> getCabinetCompanies(@PathVariable Long id) {
        List<Company> companies = cabinetService.getCompaniesByCabinet(id);
        return ResponseEntity.ok(companies);
    }

    /**
     * Ajoute une entreprise à un cabinet
     */
    @PostMapping("/{cabinetId}/companies/{companyId}")
    public ResponseEntity<Company> addCompanyToCabinet(
            @PathVariable Long cabinetId,
            @PathVariable Long companyId) {
        log.info("Ajout de l'entreprise {} au cabinet {}", companyId, cabinetId);

        Company company = cabinetService.addCompanyToCabinet(cabinetId, companyId);
        return ResponseEntity.ok(company);
    }

    /**
     * Retire une entreprise d'un cabinet
     */
    @DeleteMapping("/{cabinetId}/companies/{companyId}")
    public ResponseEntity<Void> removeCompanyFromCabinet(
            @PathVariable Long cabinetId,
            @PathVariable Long companyId) {
        log.info("Retrait de l'entreprise {} du cabinet {}", companyId, cabinetId);

        cabinetService.removeCompanyFromCabinet(cabinetId, companyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère tous les utilisateurs d'un cabinet
     */
    @GetMapping("/{id}/users")
    public ResponseEntity<List<User>> getCabinetUsers(@PathVariable Long id) {
        List<User> users = cabinetService.getUsersByCabinet(id);
        return ResponseEntity.ok(users);
    }

    /**
     * Ajoute un utilisateur à un cabinet
     */
    @PostMapping("/{cabinetId}/users/{userId}")
    public ResponseEntity<User> addUserToCabinet(
            @PathVariable Long cabinetId,
            @PathVariable Long userId) {
        log.info("Ajout de l'utilisateur {} au cabinet {}", userId, cabinetId);

        User user = cabinetService.addUserToCabinet(cabinetId, userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Met à jour le plan d'un cabinet
     */
    @PutMapping("/{id}/plan")
    public ResponseEntity<Cabinet> updateCabinetPlan(
            @PathVariable Long id,
            @RequestParam String plan,
            @RequestParam(required = false) Integer maxCompanies,
            @RequestParam(required = false) Integer maxUsers) {
        log.info("Mise à jour du plan du cabinet {} vers {}", id, plan);

        Cabinet updated = cabinetService.updateCabinetPlan(id, plan, maxCompanies, maxUsers);
        return ResponseEntity.ok(updated);
    }

    /**
     * Vérifie si un cabinet peut ajouter une entreprise
     */
    @GetMapping("/{id}/can-add-company")
    public ResponseEntity<Boolean> canAddCompany(@PathVariable Long id) {
        boolean canAdd = cabinetService.canAddCompany(id);
        return ResponseEntity.ok(canAdd);
    }

    /**
     * Vérifie si un cabinet peut ajouter un utilisateur
     */
    @GetMapping("/{id}/can-add-user")
    public ResponseEntity<Boolean> canAddUser(@PathVariable Long id) {
        boolean canAdd = cabinetService.canAddUser(id);
        return ResponseEntity.ok(canAdd);
    }

    /**
     * Compte le nombre d'entreprises d'un cabinet
     */
    @GetMapping("/{id}/companies/count")
    public ResponseEntity<Long> countCompanies(@PathVariable Long id) {
        Long count = cabinetService.countCompanies(id);
        return ResponseEntity.ok(count);
    }

    /**
     * Compte le nombre d'utilisateurs d'un cabinet
     */
    @GetMapping("/{id}/users/count")
    public ResponseEntity<Long> countUsers(@PathVariable Long id) {
        Long count = cabinetService.countUsers(id);
        return ResponseEntity.ok(count);
    }

    /**
     * Supprime un cabinet
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteCabinet(@PathVariable Long id) {
        log.info("Suppression du cabinet ID: {}", id);

        cabinetService.deleteCabinet(id);
        return ResponseEntity.noContent().build();
    }

    // Méthodes utilitaires

    private Cabinet mapToEntity(CabinetDTO dto) {
        return Cabinet.builder()
            .name(dto.getName())
            .code(dto.getCode())
            .address(dto.getAddress())
            .phone(dto.getPhone())
            .email(dto.getEmail())
            .maxCompanies(dto.getMaxCompanies())
            .maxUsers(dto.getMaxUsers())
            .plan(dto.getPlan())
            .build();
    }
}
