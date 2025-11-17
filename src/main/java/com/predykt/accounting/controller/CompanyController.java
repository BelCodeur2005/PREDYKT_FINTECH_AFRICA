package com.predykt.accounting.controller;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.dto.request.CompanyCreateRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.CompanyResponse;
import com.predykt.accounting.mapper.CompanyMapper;
import com.predykt.accounting.service.ChartOfAccountsService;
import com.predykt.accounting.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
@Tag(name = "Entreprises", description = "Gestion des entreprises")
public class CompanyController {
    
    private final CompanyService companyService;
    private final ChartOfAccountsService chartService;
    private final CompanyMapper companyMapper;
    
    @PostMapping
    @Operation(summary = "Créer une entreprise",
               description = "Crée une nouvelle entreprise et initialise son plan comptable OHADA")
    public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
            @Valid @RequestBody CompanyCreateRequest request) {
        
        Company company = companyService.createCompany(request);
        
        // Initialiser le plan comptable par défaut
        chartService.initializeDefaultChartOfAccounts(company.getId());
        
        CompanyResponse response = companyMapper.toResponse(company);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Entreprise créée avec succès"));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une entreprise par ID")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompanyById(@PathVariable Long id) {
        Company company = companyService.getCompanyById(id);
        CompanyResponse response = companyMapper.toResponse(company);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping
    @Operation(summary = "Lister toutes les entreprises actives")
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> getAllActiveCompanies() {
        List<Company> companies = companyService.getAllActiveCompanies();
        List<CompanyResponse> responses = companies.stream()
            .map(companyMapper::toResponse)
            .toList();
        
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une entreprise")
    public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyCreateRequest request) {
        
        Company company = companyService.updateCompany(id, request);
        CompanyResponse response = companyMapper.toResponse(company);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Entreprise mise à jour"));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Désactiver une entreprise")
    public ResponseEntity<ApiResponse<Void>> deactivateCompany(@PathVariable Long id) {
        companyService.deactivateCompany(id);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Entreprise désactivée"));
    }
}