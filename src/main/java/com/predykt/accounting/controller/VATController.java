package com.predykt.accounting.controller;

import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.VATSummaryResponse;
import com.predykt.accounting.service.VATService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/companies/{companyId}/vat")
@RequiredArgsConstructor
@Tag(name = "TVA", description = "Gestion de la TVA (Taxe sur la Valeur Ajoutee)")
public class VATController {

    private final VATService vatService;

    @GetMapping("/summary")
    @Operation(summary = "Resume TVA",
               description = "Calcule le resume TVA pour une periode (collectee, deductible, a payer)")
    public ResponseEntity<ApiResponse<VATSummaryResponse>> getVATSummary(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        VATSummaryResponse summary = vatService.calculateVATSummary(companyId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/detailed-report")
    @Operation(summary = "Rapport TVA detaille",
               description = "Genere un rapport TVA detaille par compte")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getDetailedVATReport(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, BigDecimal> report = vatService.generateDetailedVATReport(companyId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/registration-status")
    @Operation(summary = "Statut assujettissement TVA",
               description = "Verifie si l'entreprise est assujettie a la TVA")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRegistrationStatus(
            @PathVariable Long companyId) {

        boolean isRegistered = vatService.isVATRegistered(companyId);
        Map<String, Object> status = Map.of(
            "companyId", companyId,
            "isVATRegistered", isRegistered,
            "message", isRegistered ? "Entreprise assujettie a la TVA" : "Entreprise non assujettie"
        );

        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/rate")
    @Operation(summary = "Taux de TVA applicable",
               description = "Retourne le taux de TVA applicable selon la categorie de produit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getApplicableRate(
            @PathVariable Long companyId,
            @RequestParam(required = false) String productCategory) {

        BigDecimal rate = vatService.getApplicableVATRate(productCategory);
        Map<String, Object> result = Map.of(
            "productCategory", productCategory != null ? productCategory : "GENERAL",
            "vatRate", rate,
            "description", "Taux de TVA applicable en pourcentage"
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/calculate-ht")
    @Operation(summary = "Calculer montant HT",
               description = "Calcule le montant hors taxes a partir d'un montant TTC")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> calculateHT(
            @PathVariable Long companyId,
            @RequestParam BigDecimal amountTTC,
            @RequestParam(defaultValue = "19.25") BigDecimal vatRate) {

        BigDecimal amountHT = vatService.calculateAmountExcludingVAT(amountTTC, vatRate);
        BigDecimal vatAmount = amountTTC.subtract(amountHT);

        Map<String, BigDecimal> result = Map.of(
            "amountTTC", amountTTC,
            "amountHT", amountHT,
            "vatAmount", vatAmount,
            "vatRate", vatRate
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/calculate-ttc")
    @Operation(summary = "Calculer montant TTC",
               description = "Calcule le montant toutes taxes comprises a partir d'un montant HT")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> calculateTTC(
            @PathVariable Long companyId,
            @RequestParam BigDecimal amountHT,
            @RequestParam(defaultValue = "19.25") BigDecimal vatRate) {

        BigDecimal vatAmount = vatService.calculateVATAmount(amountHT, vatRate);
        BigDecimal amountTTC = vatService.calculateAmountIncludingVAT(amountHT, vatRate);

        Map<String, BigDecimal> result = Map.of(
            "amountHT", amountHT,
            "vatAmount", vatAmount,
            "amountTTC", amountTTC,
            "vatRate", vatRate
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/calculate-vat")
    @Operation(summary = "Calculer montant TVA",
               description = "Calcule le montant de TVA a partir d'un montant HT")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> calculateVAT(
            @PathVariable Long companyId,
            @RequestParam BigDecimal amountHT,
            @RequestParam(defaultValue = "19.25") BigDecimal vatRate) {

        BigDecimal vatAmount = vatService.calculateVATAmount(amountHT, vatRate);

        Map<String, BigDecimal> result = Map.of(
            "amountHT", amountHT,
            "vatAmount", vatAmount,
            "vatRate", vatRate
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
