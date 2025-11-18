// ============================================
// DataImportController.java
// ============================================
package com.predykt.accounting.controller;

import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.ImportResultResponse;
import com.predykt.accounting.service.CsvImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/companies/{companyId}/import")
@RequiredArgsConstructor
@Tag(name = "Import de Données", description = "Import CSV des activités comptables")
public class DataImportController {
    
    private final CsvImportService csvImportService;
    
    @PostMapping(value = "/activities-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importer un fichier CSV d'activités",
               description = "Importe et traite automatiquement un fichier CSV contenant les activités comptables")
    public ResponseEntity<ApiResponse<ImportResultResponse>> importActivitiesCsv(
            @PathVariable Long companyId,
            @RequestParam("file") MultipartFile file) {
        
        ImportResultResponse result = csvImportService.importActivitiesCsv(companyId, file);
        
        return ResponseEntity
            .status(result.getErrorCount() > 0 ? HttpStatus.PARTIAL_CONTENT : HttpStatus.CREATED)
            .body(ApiResponse.success(result, result.getMessage()));
    }
}