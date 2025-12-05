package com.predykt.accounting.controller;

import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.NotesAnnexesResponse;
import com.predykt.accounting.service.NotesAnnexesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller pour les Notes Annexes OHADA
 * 12 notes obligatoires selon le système comptable OHADA
 */
@RestController
@RequestMapping("/companies/{companyId}/notes-annexes")
@RequiredArgsConstructor
@Tag(name = "Notes Annexes", description = "Génération des notes annexes OHADA obligatoires")
public class NotesAnnexesController {

    private final NotesAnnexesService notesAnnexesService;

    @GetMapping
    @Operation(summary = "Générer les notes annexes complètes",
               description = "Génère les 12 notes annexes OHADA obligatoires pour un exercice fiscal - " +
                   "NOTE 1: Principes comptables, NOTE 2: Immobilisations, NOTE 3: Immob. financières, " +
                   "NOTE 4: Stocks, NOTE 5: Créances/Dettes, NOTE 6: Capitaux propres, " +
                   "NOTE 7: Emprunts, NOTE 8: Autres passifs, NOTE 9: Produits/Charges, " +
                   "NOTE 10: Impôts, NOTE 11: Engagements hors bilan, NOTE 12: Événements postérieurs")
    public ResponseEntity<ApiResponse<NotesAnnexesResponse>> getNotesAnnexes(
            @PathVariable Long companyId,
            @RequestParam Integer fiscalYear) {

        NotesAnnexesResponse notes = notesAnnexesService.generateNotesAnnexes(companyId, fiscalYear);

        return ResponseEntity.ok(ApiResponse.success(notes,
            String.format("Notes annexes OHADA générées pour l'exercice %d - 12 notes complètes", fiscalYear)));
    }
}
