// ============================================
// JournalEntryValidator.java
// ============================================
package com.predykt.accounting.validator;

import com.predykt.accounting.dto.request.JournalEntryLineRequest;
import com.predykt.accounting.dto.request.JournalEntryRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class JournalEntryValidator {
    
    public List<String> validate(JournalEntryRequest request) {
        List<String> errors = new ArrayList<>();
        
        // Vérifier qu'il y a au moins 2 lignes
        if (request.getLines() == null || request.getLines().size() < 2) {
            errors.add("Une écriture doit contenir au moins 2 lignes");
        }
        
        // Vérifier l'équilibre
        BigDecimal totalDebit = request.getLines().stream()
            .map(JournalEntryLineRequest::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredit = request.getLines().stream()
            .map(JournalEntryLineRequest::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalDebit.compareTo(totalCredit) != 0) {
            errors.add(String.format("Écriture déséquilibrée: Débit=%s, Crédit=%s", 
                                    totalDebit, totalCredit));
        }
        
        // Vérifier que chaque ligne a soit débit soit crédit (pas les deux)
        for (int i = 0; i < request.getLines().size(); i++) {
            JournalEntryLineRequest line = request.getLines().get(i);
            
            boolean hasDebit = line.getDebitAmount().compareTo(BigDecimal.ZERO) > 0;
            boolean hasCredit = line.getCreditAmount().compareTo(BigDecimal.ZERO) > 0;
            
            if (hasDebit && hasCredit) {
                errors.add(String.format("Ligne %d: Une ligne ne peut avoir à la fois un débit et un crédit", i + 1));
            }
            
            if (!hasDebit && !hasCredit) {
                errors.add(String.format("Ligne %d: Une ligne doit avoir un montant au débit ou au crédit", i + 1));
            }
        }
        
        return errors;
    }
    
    public boolean isValid(JournalEntryRequest request) {
        return validate(request).isEmpty();
    }
}