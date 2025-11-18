package com.predykt.accounting.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.predykt.accounting.domain.entity.*;
import com.predykt.accounting.domain.enums.AccountType;
import com.predykt.accounting.dto.request.JournalEntryLineRequest;
import com.predykt.accounting.dto.request.JournalEntryRequest;
import com.predykt.accounting.dto.response.ImportResultResponse;
import com.predykt.accounting.exception.ImportException;
import com.predykt.accounting.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service d'import des données comptables depuis CSV
 * Format attendu : date de saisie;Activitées;description;Montant Brut;Type;Années
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CsvImportService {
    
    private final CompanyRepository companyRepository;
    private final ChartOfAccountsRepository chartRepository;
    private final GeneralLedgerService glService;
    
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("d/M/yyyy")
    };
    
    /**
     * Import principal du fichier CSV des activités
     */
    @Transactional
    public ImportResultResponse importActivitiesCsv(Long companyId, MultipartFile file) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ImportException("Entreprise non trouvée: " + companyId));
        
        log.info("Début import CSV pour l'entreprise {}: {}", companyId, file.getOriginalFilename());
        
        List<ActivityRow> activities = parseCsvFile(file);
        log.info("Fichier parsé: {} lignes détectées", activities.size());
        
        // Grouper par date et référence pour créer des écritures équilibrées
        Map<String, List<ActivityRow>> groupedByDate = activities.stream()
            .filter(a -> a.amount != null && a.amount.compareTo(BigDecimal.ZERO) != 0)
            .collect(Collectors.groupingBy(a -> a.date.toString()));
        
        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (Map.Entry<String, List<ActivityRow>> entry : groupedByDate.entrySet()) {
            LocalDate entryDate = LocalDate.parse(entry.getKey());
            List<ActivityRow> dailyActivities = entry.getValue();
            
            for (ActivityRow activity : dailyActivities) {
                try {
                    createJournalEntryFromActivity(company, activity);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = String.format("Ligne %s - %s: %s", 
                        activity.date, activity.description, e.getMessage());
                    errors.add(errorMsg);
                    log.warn("Erreur import: {}", errorMsg);
                }
            }
        }
        
        log.info("Import terminé: {} succès, {} erreurs", successCount, errorCount);
        
        return ImportResultResponse.builder()
            .totalRows(activities.size())
            .successCount(successCount)
            .errorCount(errorCount)
            .errors(errors)
            .message(String.format("Import terminé: %d/%d lignes importées", successCount, activities.size()))
            .build();
    }
    
    /**
     * Parse le fichier CSV avec détection du séparateur
     */
    private List<ActivityRow> parseCsvFile(MultipartFile file) {
        List<ActivityRow> activities = new ArrayList<>();
        
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {
            
            List<String[]> allRows = csvReader.readAll();
            
            if (allRows.isEmpty()) {
                throw new ImportException("Fichier CSV vide");
            }
            
            // Déterminer le séparateur (priorité: ;, puis ,)
            char separator = detectSeparator(allRows.get(0));
            log.info("Séparateur détecté: '{}'", separator);
            
            // Skip header
            boolean isFirstRow = true;
            
            for (String[] row : allRows) {
                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }
                
                // Ignorer les lignes vides
                if (row.length == 0 || (row.length == 1 && (row[0] == null || row[0].trim().isEmpty()))) {
                    continue;
                }
                
                // Re-split si le séparateur n'est pas le bon
                String[] columns = row;
                if (separator != ',' && row.length == 1) {
                    columns = row[0].split(String.valueOf(separator));
                }
                
                try {
                    ActivityRow activity = parseActivityRow(columns);
                    if (activity != null) {
                        activities.add(activity);
                    }
                } catch (Exception e) {
                    log.warn("Ligne ignorée (parsing error): {} - {}", Arrays.toString(row), e.getMessage());
                }
            }
            
        } catch (IOException | CsvException e) {
            throw new ImportException("Erreur lecture fichier CSV: " + e.getMessage());
        }
        
        return activities;
    }
    
    /**
     * Détecte le séparateur le plus probable
     */
    private char detectSeparator(String[] firstRow) {
        if (firstRow.length == 1 && firstRow[0].contains(";")) {
            return ';';
        }
        return ',';
    }
    
    /**
     * Parse une ligne CSV en ActivityRow
     * Format: date de saisie;Activitées;description;Montant Brut;Type;Années
     */
    private ActivityRow parseActivityRow(String[] columns) {
        if (columns.length < 5) {
            return null; // Ligne incomplète
        }
        
        try {
            ActivityRow row = new ActivityRow();
            
            // Colonne 0: Date
            row.date = parseDate(columns[0].trim());
            if (row.date == null) {
                return null;
            }
            
            // Colonne 1: Activité (catégorie)
            row.activity = columns[1].trim();
            
            // Colonne 2: Description
            row.description = columns.length > 2 ? columns[2].trim() : "";
            if (row.description.isEmpty() || row.description.equalsIgnoreCase("Description manquante")) {
                row.description = row.activity; // Utiliser l'activité comme description par défaut
            }
            
            // Colonne 3: Montant
            row.amount = parseAmount(columns[3].trim());
            
            // Colonne 4: Type (Revenu, Dépenses, Capex, Financing)
            row.type = columns[4].trim();
            
            // Colonne 5: Année
            if (columns.length > 5 && !columns[5].trim().isEmpty()) {
                try {
                    row.year = Integer.parseInt(columns[5].trim());
                } catch (NumberFormatException e) {
                    row.year = row.date.getYear();
                }
            } else {
                row.year = row.date.getYear();
            }
            
            return row;
            
        } catch (Exception e) {
            log.warn("Erreur parsing ligne: {} - {}", Arrays.toString(columns), e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse une date avec plusieurs formats possibles
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        
        log.warn("Format de date non reconnu: {}", dateStr);
        return null;
    }
    
    /**
     * Parse un montant avec gestion des formats français/internationaux
     */
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        try {
            // Supprimer les espaces et remplacer les virgules par des points
            String cleaned = amountStr
                .replaceAll("\\s", "")  // Supprimer espaces
                .replace(",", ".");      // Virgule -> point
            
            // Gérer le cas des montants négatifs entre parenthèses
            if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
                cleaned = "-" + cleaned.substring(1, cleaned.length() - 1);
            }
            
            return new BigDecimal(cleaned);
            
        } catch (NumberFormatException e) {
            log.warn("Format de montant invalide: {}", amountStr);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Crée une écriture comptable à partir d'une activité
     */
    private void createJournalEntryFromActivity(Company company, ActivityRow activity) {
        // Déterminer le compte comptable selon l'activité et le type
        String accountNumber = determineAccountNumber(activity);
        
        // Vérifier que le compte existe
        ChartOfAccounts account = chartRepository
            .findByCompanyAndAccountNumber(company, accountNumber)
            .orElseThrow(() -> new ImportException(
                String.format("Compte %s non trouvé pour l'activité: %s", accountNumber, activity.activity)
            ));
        
        // Créer la requête d'écriture
        JournalEntryRequest request = new JournalEntryRequest();
        request.setEntryDate(activity.date);
        request.setReference("IMPORT-" + UUID.randomUUID().toString().substring(0, 8));
        request.setJournalCode(determineJournalCode(activity.type));
        
        List<JournalEntryLineRequest> lines = new ArrayList<>();
        
        // Ligne principale (compte de l'activité)
        JournalEntryLineRequest mainLine = new JournalEntryLineRequest();
        mainLine.setAccountNumber(accountNumber);
        mainLine.setDescription(activity.description);
        
        // Ligne de contrepartie (compte de trésorerie ou autre)
        JournalEntryLineRequest contraLine = new JournalEntryLineRequest();
        String contraAccountNumber = determineContraAccount(activity);
        contraLine.setAccountNumber(contraAccountNumber);
        contraLine.setDescription(activity.description);
        
        // Répartir débit/crédit selon le type
        BigDecimal absAmount = activity.amount.abs();
        
        if (activity.type.equalsIgnoreCase("Revenu")) {
            // Revenu: Débit Trésorerie, Crédit Compte de Produit
            mainLine.setDebitAmount(BigDecimal.ZERO);
            mainLine.setCreditAmount(absAmount);
            contraLine.setDebitAmount(absAmount);
            contraLine.setCreditAmount(BigDecimal.ZERO);
        } else if (activity.type.equalsIgnoreCase("Dépenses") || activity.type.equalsIgnoreCase("Capex")) {
            // Dépense: Débit Compte de Charge, Crédit Trésorerie
            mainLine.setDebitAmount(absAmount);
            mainLine.setCreditAmount(BigDecimal.ZERO);
            contraLine.setDebitAmount(BigDecimal.ZERO);
            contraLine.setCreditAmount(absAmount);
        } else if (activity.type.equalsIgnoreCase("Financing")) {
            // Financement: selon le signe
            if (activity.amount.compareTo(BigDecimal.ZERO) > 0) {
                // Entrée de cash (emprunt reçu)
                mainLine.setDebitAmount(absAmount);
                mainLine.setCreditAmount(BigDecimal.ZERO);
                contraLine.setDebitAmount(BigDecimal.ZERO);
                contraLine.setCreditAmount(absAmount);
            } else {
                // Sortie de cash (remboursement)
                mainLine.setDebitAmount(BigDecimal.ZERO);
                mainLine.setCreditAmount(absAmount);
                contraLine.setDebitAmount(absAmount);
                contraLine.setCreditAmount(BigDecimal.ZERO);
            }
        }
        
        lines.add(mainLine);
        lines.add(contraLine);
        request.setLines(lines);
        
        // Enregistrer l'écriture via le service
        glService.recordJournalEntry(company.getId(), request);
    }
    
    /**
     * Détermine le numéro de compte OHADA selon l'activité
     */
    private String determineAccountNumber(ActivityRow activity) {
        String activityLower = activity.activity.toLowerCase();
        
        // Mapping des activités vers les comptes OHADA
        if (activityLower.contains("sales") || activityLower.contains("vente")) {
            if (activityLower.contains("wholesale")) {
                return "701"; // Ventes de marchandises
            } else if (activityLower.contains("retail")) {
                return "701"; // Ventes de marchandises
            } else if (activityLower.contains("export")) {
                return "701"; // Ventes de marchandises
            } else if (activityLower.contains("service")) {
                return "706"; // Prestations de services
            }
            return "701"; // Par défaut
        }
        
        if (activityLower.contains("income")) {
            return "75"; // Autres produits
        }
        
        if (activityLower.contains("purchase") || activityLower.contains("achat")) {
            if (activityLower.contains("raw material")) {
                return "601"; // Achats de matières premières
            } else if (activityLower.contains("packaging")) {
                return "605"; // Achats de fournitures
            }
            return "601"; // Par défaut
        }
        
        if (activityLower.contains("labor") || activityLower.contains("salaries")) {
            return "661"; // Rémunérations du personnel
        }
        
        if (activityLower.contains("rent") || activityLower.contains("loyer")) {
            return "622"; // Loyers
        }
        
        if (activityLower.contains("logistics") || activityLower.contains("distribution")) {
            return "628"; // Transports
        }
        
        if (activityLower.contains("marketing")) {
            return "627"; // Publicité
        }
        
        if (activityLower.contains("professional fees") || activityLower.contains("honoraires")) {
            return "632"; // Honoraires
        }
        
        if (activityLower.contains("maintenance")) {
            return "625"; // Entretien et réparations
        }
        
        if (activityLower.contains("depreciation")) {
            return "681"; // Dotations aux amortissements
        }
        
        if (activityLower.contains("utilities") || activityLower.contains("overheads")) {
            return "624"; // Eau, électricité
        }
        
        if (activityLower.contains("capex") || activityLower.contains("equipment")) {
            return "24"; // Matériel (immobilisations)
        }
        
        if (activityLower.contains("loan") || activityLower.contains("emprunt")) {
            return "16"; // Emprunts
        }
        
        // Par défaut: compte de charges diverses
        if (activity.type.equalsIgnoreCase("Dépenses")) {
            return "658"; // Charges diverses
        }
        
        return "701"; // Par défaut: Ventes
    }
    
    /**
     * Détermine le compte de contrepartie (généralement trésorerie)
     */
    private String determineContraAccount(ActivityRow activity) {
        // Pour le Capex, utiliser compte fournisseur d'immobilisations
        if (activity.type.equalsIgnoreCase("Capex")) {
            return "404"; // Fournisseurs d'immobilisations
        }
        
        // Pour les emprunts/financements
        if (activity.type.equalsIgnoreCase("Financing")) {
            return "521"; // Banques locales
        }
        
        // Par défaut: Compte bancaire principal
        return "521"; // Banques locales
    }
    
    /**
     * Détermine le code journal selon le type
     */
    private String determineJournalCode(String type) {
        return switch (type.toUpperCase()) {
            case "REVENU" -> "VE"; // Journal des ventes
            case "DÉPENSES" -> "AC"; // Journal des achats
            case "CAPEX" -> "AC"; // Journal des achats (immobilisations)
            case "FINANCING" -> "BQ"; // Journal de banque
            default -> "OD"; // Opérations diverses
        };
    }
    
    /**
     * Classe interne pour représenter une ligne d'activité
     */
    private static class ActivityRow {
        LocalDate date;
        String activity;
        String description;
        BigDecimal amount;
        String type;
        Integer year;
    }
}

