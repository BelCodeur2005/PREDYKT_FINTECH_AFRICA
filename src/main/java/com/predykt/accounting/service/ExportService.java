package com.predykt.accounting.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.dto.response.BalanceSheetResponse;
import com.predykt.accounting.dto.response.IncomeStatementResponse;
import com.predykt.accounting.dto.response.NotesAnnexesResponse;
import com.predykt.accounting.dto.response.SubledgerResponse;
import com.predykt.accounting.repository.CompanyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service pour l'export de rapports financiers en PDF et Excel
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final FinancialReportService reportService;
    private final CompanyRepository companyRepository;
    private final GeneralLedgerService generalLedgerService;
    private final AgingReportService agingReportService;
    private final DashboardService dashboardService;
    private final TAFIREService tafireService;
    private final AuxiliaryJournalsService auxiliaryJournalsService;
    private final NotesAnnexesService notesAnnexesService;
    private final SubledgerService subledgerService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Exporte le bilan en PDF
     */
    public byte[] exportBalanceSheetToPdf(Long companyId, LocalDate asOfDate) throws IOException {
        log.info("Export du bilan en PDF pour l'entreprise {} au {}", companyId, asOfDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        BalanceSheetResponse balanceSheet = reportService.generateBalanceSheet(companyId, asOfDate);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // En-tete
        document.add(new Paragraph("BILAN COMPTABLE")
            .setFontSize(20)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(company.getName())
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Au " + asOfDate.format(DATE_FORMATTER))
            .setFontSize(12)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20));

        // Table ACTIF
        document.add(new Paragraph("ACTIF")
            .setFontSize(14)
            .setBold()
            .setMarginTop(10));

        Table assetTable = new Table(UnitValue.createPercentArray(new float[]{3, 2}))
            .useAllAvailableWidth();

        addTableHeader(assetTable, "Poste", "Montant (" + balanceSheet.getCurrency() + ")");
        addTableRow(assetTable, "Actifs immobilisés", formatAmount(balanceSheet.getFixedAssets()));
        addTableRow(assetTable, "Actifs circulants", formatAmount(balanceSheet.getCurrentAssets()));
        addTableRow(assetTable, "Trésorerie", formatAmount(balanceSheet.getCash()));
        addTableTotal(assetTable, "TOTAL ACTIF", formatAmount(balanceSheet.getTotalAssets()));

        document.add(assetTable);

        // Table PASSIF
        document.add(new Paragraph("PASSIF")
            .setFontSize(14)
            .setBold()
            .setMarginTop(20));

        Table liabilityTable = new Table(UnitValue.createPercentArray(new float[]{3, 2}))
            .useAllAvailableWidth();

        addTableHeader(liabilityTable, "Poste", "Montant (" + balanceSheet.getCurrency() + ")");
        addTableRow(liabilityTable, "Capitaux propres", formatAmount(balanceSheet.getEquity()));
        addTableRow(liabilityTable, "Dettes à long terme", formatAmount(balanceSheet.getLongTermLiabilities()));
        addTableRow(liabilityTable, "Dettes à court terme", formatAmount(balanceSheet.getCurrentLiabilities()));
        addTableTotal(liabilityTable, "TOTAL PASSIF", formatAmount(balanceSheet.getTotalLiabilities()));

        document.add(liabilityTable);

        // Pied de page
        document.add(new Paragraph("\nDocument généré le " + LocalDate.now().format(DATE_FORMATTER))
            .setFontSize(10)
            .setTextAlignment(TextAlignment.RIGHT)
            .setMarginTop(30));

        document.close();

        log.info("Export PDF du bilan terminé - {} octets", baos.size());
        return baos.toByteArray();
    }

    /**
     * Exporte le bilan en Excel
     */
    public byte[] exportBalanceSheetToExcel(Long companyId, LocalDate asOfDate) throws IOException {
        log.info("Export du bilan en Excel pour l'entreprise {} au {}", companyId, asOfDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        BalanceSheetResponse balanceSheet = reportService.generateBalanceSheet(companyId, asOfDate);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Bilan");

            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            int rowNum = 0;

            // Titre
            Row titleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BILAN COMPTABLE");
            titleCell.setCellStyle(titleStyle);

            // Entreprise
            Row companyRow = sheet.createRow(rowNum++);
            companyRow.createCell(0).setCellValue(company.getName());

            // Date
            Row dateRow = sheet.createRow(rowNum++);
            dateRow.createCell(0).setCellValue("Au " + asOfDate.format(DATE_FORMATTER));

            rowNum++; // Ligne vide

            // ACTIF
            Row actifHeaderRow = sheet.createRow(rowNum++);
            actifHeaderRow.createCell(0).setCellValue("ACTIF");
            actifHeaderRow.getCell(0).setCellStyle(headerStyle);

            Row actifColHeaderRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell col1 = actifColHeaderRow.createCell(0);
            col1.setCellValue("Poste");
            col1.setCellStyle(headerStyle);
            org.apache.poi.ss.usermodel.Cell col2 = actifColHeaderRow.createCell(1);
            col2.setCellValue("Montant (" + balanceSheet.getCurrency() + ")");
            col2.setCellStyle(headerStyle);

            addExcelRow(sheet, rowNum++, "Actifs immobilisés", balanceSheet.getFixedAssets(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Actifs circulants", balanceSheet.getCurrentAssets(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Trésorerie", balanceSheet.getCash(), currencyStyle);
            addExcelRow(sheet, rowNum++, "TOTAL ACTIF", balanceSheet.getTotalAssets(), totalStyle);

            rowNum++; // Ligne vide

            // PASSIF
            Row passifHeaderRow = sheet.createRow(rowNum++);
            passifHeaderRow.createCell(0).setCellValue("PASSIF");
            passifHeaderRow.getCell(0).setCellStyle(headerStyle);

            Row passifColHeaderRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell col3 = passifColHeaderRow.createCell(0);
            col3.setCellValue("Poste");
            col3.setCellStyle(headerStyle);
            org.apache.poi.ss.usermodel.Cell col4 = passifColHeaderRow.createCell(1);
            col4.setCellValue("Montant (" + balanceSheet.getCurrency() + ")");
            col4.setCellStyle(headerStyle);

            addExcelRow(sheet, rowNum++, "Capitaux propres", balanceSheet.getEquity(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Dettes à long terme", balanceSheet.getLongTermLiabilities(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Dettes à court terme", balanceSheet.getCurrentLiabilities(), currencyStyle);
            addExcelRow(sheet, rowNum++, "TOTAL PASSIF", balanceSheet.getTotalLiabilities(), totalStyle);

            // Auto-size columns
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            log.info("Export Excel du bilan terminé - {} octets", baos.size());
            return baos.toByteArray();
        }
    }

    /**
     * Exporte le compte de résultat en PDF
     */
    public byte[] exportIncomeStatementToPdf(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export du compte de résultat en PDF pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        IncomeStatementResponse incomeStatement = reportService.generateIncomeStatement(companyId, startDate, endDate);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // En-tete
        document.add(new Paragraph("COMPTE DE RÉSULTAT")
            .setFontSize(20)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(company.getName())
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Du " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER))
            .setFontSize(12)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20));

        // Table PRODUITS
        document.add(new Paragraph("PRODUITS")
            .setFontSize(14)
            .setBold()
            .setMarginTop(10));

        Table revenueTable = new Table(UnitValue.createPercentArray(new float[]{3, 2}))
            .useAllAvailableWidth();

        addTableHeader(revenueTable, "Poste", "Montant (" + incomeStatement.getCurrency() + ")");
        addTableRow(revenueTable, "Ventes de marchandises", formatAmount(incomeStatement.getSalesRevenue()));
        addTableRow(revenueTable, "Prestations de services", formatAmount(incomeStatement.getServiceRevenue()));
        addTableRow(revenueTable, "Produits financiers", formatAmount(incomeStatement.getFinancialIncome()));
        addTableRow(revenueTable, "Autres produits", formatAmount(incomeStatement.getOtherOperatingIncome()));
        addTableTotal(revenueTable, "TOTAL PRODUITS", formatAmount(incomeStatement.getTotalRevenue()));

        document.add(revenueTable);

        // Table CHARGES
        document.add(new Paragraph("CHARGES")
            .setFontSize(14)
            .setBold()
            .setMarginTop(20));

        Table expenseTable = new Table(UnitValue.createPercentArray(new float[]{3, 2}))
            .useAllAvailableWidth();

        addTableHeader(expenseTable, "Poste", "Montant (" + incomeStatement.getCurrency() + ")");
        addTableRow(expenseTable, "Achats consommés", formatAmount(incomeStatement.getPurchasesCost()));
        addTableRow(expenseTable, "Charges de personnel", formatAmount(incomeStatement.getPersonnelCost()));
        addTableRow(expenseTable, "Charges d'exploitation", formatAmount(incomeStatement.getOperatingExpenses()));
        addTableRow(expenseTable, "Charges financières", formatAmount(incomeStatement.getFinancialExpenses()));
        addTableRow(expenseTable, "Impôts et taxes", formatAmount(incomeStatement.getTaxesAndDuties()));
        addTableTotal(expenseTable, "TOTAL CHARGES", formatAmount(incomeStatement.getTotalExpenses()));

        document.add(expenseTable);

        // Table RÉSULTATS
        document.add(new Paragraph("RÉSULTATS")
            .setFontSize(14)
            .setBold()
            .setMarginTop(20));

        Table resultTable = new Table(UnitValue.createPercentArray(new float[]{3, 2}))
            .useAllAvailableWidth();

        addTableHeader(resultTable, "Indicateur", "Montant (" + incomeStatement.getCurrency() + ")");
        addTableRow(resultTable, "Marge brute", formatAmount(incomeStatement.getGrossProfit()));
        addTableRow(resultTable, "Résultat d'exploitation", formatAmount(incomeStatement.getOperatingIncome()));
        addTableTotal(resultTable, "RÉSULTAT NET", formatAmount(incomeStatement.getNetIncome()));

        document.add(resultTable);

        // Ratios
        document.add(new Paragraph("RATIOS")
            .setFontSize(14)
            .setBold()
            .setMarginTop(20));

        Table ratioTable = new Table(UnitValue.createPercentArray(new float[]{3, 2}))
            .useAllAvailableWidth();

        addTableHeader(ratioTable, "Ratio", "Valeur");
        addTableRow(ratioTable, "Marge brute (%)", formatPercentage(incomeStatement.getGrossMarginPercentage()));
        addTableRow(ratioTable, "Marge nette (%)", formatPercentage(incomeStatement.getNetMarginPercentage()));

        document.add(ratioTable);

        // Pied de page
        document.add(new Paragraph("\nDocument généré le " + LocalDate.now().format(DATE_FORMATTER))
            .setFontSize(10)
            .setTextAlignment(TextAlignment.RIGHT)
            .setMarginTop(30));

        document.close();

        log.info("Export PDF du compte de résultat terminé - {} octets", baos.size());
        return baos.toByteArray();
    }

    /**
     * Exporte le compte de résultat en Excel
     */
    public byte[] exportIncomeStatementToExcel(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export du compte de résultat en Excel pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        IncomeStatementResponse incomeStatement = reportService.generateIncomeStatement(companyId, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Compte de Résultat");

            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);

            int rowNum = 0;

            // Titre
            Row titleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("COMPTE DE RÉSULTAT");
            titleCell.setCellStyle(titleStyle);

            // Entreprise
            Row companyRow = sheet.createRow(rowNum++);
            companyRow.createCell(0).setCellValue(company.getName());

            // Date
            Row dateRow = sheet.createRow(rowNum++);
            dateRow.createCell(0).setCellValue("Du " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER));

            rowNum++; // Ligne vide

            // PRODUITS
            Row revenueHeaderRow = sheet.createRow(rowNum++);
            revenueHeaderRow.createCell(0).setCellValue("PRODUITS");
            revenueHeaderRow.getCell(0).setCellStyle(headerStyle);

            Row revenueColHeaderRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell col1 = revenueColHeaderRow.createCell(0);
            col1.setCellValue("Poste");
            col1.setCellStyle(headerStyle);
            org.apache.poi.ss.usermodel.Cell col2 = revenueColHeaderRow.createCell(1);
            col2.setCellValue("Montant (" + incomeStatement.getCurrency() + ")");
            col2.setCellStyle(headerStyle);

            addExcelRow(sheet, rowNum++, "Ventes de marchandises", incomeStatement.getSalesRevenue(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Prestations de services", incomeStatement.getServiceRevenue(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Produits financiers", incomeStatement.getFinancialIncome(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Autres produits", incomeStatement.getOtherOperatingIncome(), currencyStyle);
            addExcelRow(sheet, rowNum++, "TOTAL PRODUITS", incomeStatement.getTotalRevenue(), totalStyle);

            rowNum++; // Ligne vide

            // CHARGES
            Row expenseHeaderRow = sheet.createRow(rowNum++);
            expenseHeaderRow.createCell(0).setCellValue("CHARGES");
            expenseHeaderRow.getCell(0).setCellStyle(headerStyle);

            Row expenseColHeaderRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell col3 = expenseColHeaderRow.createCell(0);
            col3.setCellValue("Poste");
            col3.setCellStyle(headerStyle);
            org.apache.poi.ss.usermodel.Cell col4 = expenseColHeaderRow.createCell(1);
            col4.setCellValue("Montant (" + incomeStatement.getCurrency() + ")");
            col4.setCellStyle(headerStyle);

            addExcelRow(sheet, rowNum++, "Achats consommés", incomeStatement.getPurchasesCost(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Charges de personnel", incomeStatement.getPersonnelCost(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Charges d'exploitation", incomeStatement.getOperatingExpenses(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Charges financières", incomeStatement.getFinancialExpenses(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Impôts et taxes", incomeStatement.getTaxesAndDuties(), currencyStyle);
            addExcelRow(sheet, rowNum++, "TOTAL CHARGES", incomeStatement.getTotalExpenses(), totalStyle);

            rowNum++; // Ligne vide

            // RÉSULTATS
            Row resultHeaderRow = sheet.createRow(rowNum++);
            resultHeaderRow.createCell(0).setCellValue("RÉSULTATS");
            resultHeaderRow.getCell(0).setCellStyle(headerStyle);

            Row resultColHeaderRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell col5 = resultColHeaderRow.createCell(0);
            col5.setCellValue("Indicateur");
            col5.setCellStyle(headerStyle);
            org.apache.poi.ss.usermodel.Cell col6 = resultColHeaderRow.createCell(1);
            col6.setCellValue("Montant (" + incomeStatement.getCurrency() + ")");
            col6.setCellStyle(headerStyle);

            addExcelRow(sheet, rowNum++, "Marge brute", incomeStatement.getGrossProfit(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Résultat d'exploitation", incomeStatement.getOperatingIncome(), currencyStyle);
            addExcelRow(sheet, rowNum++, "RÉSULTAT NET", incomeStatement.getNetIncome(), totalStyle);

            rowNum++; // Ligne vide

            // RATIOS
            Row ratioHeaderRow = sheet.createRow(rowNum++);
            ratioHeaderRow.createCell(0).setCellValue("RATIOS");
            ratioHeaderRow.getCell(0).setCellStyle(headerStyle);

            Row ratioColHeaderRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell col7 = ratioColHeaderRow.createCell(0);
            col7.setCellValue("Ratio");
            col7.setCellStyle(headerStyle);
            org.apache.poi.ss.usermodel.Cell col8 = ratioColHeaderRow.createCell(1);
            col8.setCellValue("Valeur");
            col8.setCellStyle(headerStyle);

            addExcelRow(sheet, rowNum++, "Marge brute (%)", incomeStatement.getGrossMarginPercentage(), percentStyle);
            addExcelRow(sheet, rowNum++, "Marge nette (%)", incomeStatement.getNetMarginPercentage(), percentStyle);

            // Auto-size columns
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            log.info("Export Excel du compte de résultat terminé - {} octets", baos.size());
            return baos.toByteArray();
        }
    }

    // ==================== Méthodes utilitaires PDF ====================

    private void addTableHeader(Table table, String... headers) {
        for (String header : headers) {
            com.itextpdf.layout.element.Cell headerCell = new com.itextpdf.layout.element.Cell().add(new Paragraph(header).setBold());
            headerCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            headerCell.setTextAlignment(TextAlignment.CENTER);
            table.addHeaderCell(headerCell);
        }
    }

    private void addTableRow(Table table, String label, String value) {
        table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(label)));
        table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(value)).setTextAlignment(TextAlignment.RIGHT));
    }

    private void addTableTotal(Table table, String label, String value) {
        table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(label).setBold())
            .setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(value).setBold())
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            .setTextAlignment(TextAlignment.RIGHT));
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }

    private String formatPercentage(BigDecimal percentage) {
        if (percentage == null) return "0.00 %";
        return String.format("%.2f %%", percentage);
    }

    // ==================== Méthodes utilitaires Excel ====================

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    private CellStyle createTotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00\"%\""));
        return style;
    }

    private void addExcelRow(Sheet sheet, int rowNum, String label, BigDecimal value, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        org.apache.poi.ss.usermodel.Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value != null ? value.doubleValue() : 0.0);
        valueCell.setCellStyle(valueStyle);
    }

    /**
     * Exporte l'historique des ratios financiers en Excel
     */
    public byte[] exportRatiosHistoryToExcel(Long companyId) throws IOException {
        log.info("Export de l'historique des ratios en Excel pour l'entreprise {}", companyId);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        List<com.predykt.accounting.domain.entity.FinancialRatio> ratios =
            reportService.getFinancialRatioRepository().findByCompanyOrderByFiscalYearDesc(company);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Historique Ratios");

            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);
            CellStyle numberStyle = createCurrencyStyle(workbook);

            int rowNum = 0;

            // Titre
            Row titleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("HISTORIQUE DES RATIOS FINANCIERS");
            titleCell.setCellStyle(titleStyle);

            // Entreprise
            Row companyRow = sheet.createRow(rowNum++);
            companyRow.createCell(0).setCellValue(company.getName());

            rowNum++; // Ligne vide

            // En-têtes de colonnes
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {
                "Année Fiscale", "ROA (%)", "ROE (%)", "Marge Brute (%)", "Marge Nette (%)",
                "Ratio Courant", "Ratio Rapide", "DSO (jours)", "DIO (jours)", "DPO (jours)",
                "Dette/Capitaux", "Ratio Dette (%)", "Cycle Conv. Tréso"
            };

            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Données
            for (com.predykt.accounting.domain.entity.FinancialRatio ratio : ratios) {
                Row dataRow = sheet.createRow(rowNum++);
                int colNum = 0;

                dataRow.createCell(colNum++).setCellValue(ratio.getFiscalYear());

                org.apache.poi.ss.usermodel.Cell roaCell = dataRow.createCell(colNum++);
                roaCell.setCellValue(ratio.getRoaPct() != null ? ratio.getRoaPct().doubleValue() : 0.0);
                roaCell.setCellStyle(percentStyle);

                org.apache.poi.ss.usermodel.Cell roeCell = dataRow.createCell(colNum++);
                roeCell.setCellValue(ratio.getRoePct() != null ? ratio.getRoePct().doubleValue() : 0.0);
                roeCell.setCellStyle(percentStyle);

                org.apache.poi.ss.usermodel.Cell grossMarginCell = dataRow.createCell(colNum++);
                grossMarginCell.setCellValue(ratio.getGrossMarginPct() != null ? ratio.getGrossMarginPct().doubleValue() : 0.0);
                grossMarginCell.setCellStyle(percentStyle);

                org.apache.poi.ss.usermodel.Cell netMarginCell = dataRow.createCell(colNum++);
                netMarginCell.setCellValue(ratio.getNetMarginPct() != null ? ratio.getNetMarginPct().doubleValue() : 0.0);
                netMarginCell.setCellStyle(percentStyle);

                org.apache.poi.ss.usermodel.Cell currentRatioCell = dataRow.createCell(colNum++);
                currentRatioCell.setCellValue(ratio.getCurrentRatio() != null ? ratio.getCurrentRatio().doubleValue() : 0.0);
                currentRatioCell.setCellStyle(numberStyle);

                org.apache.poi.ss.usermodel.Cell quickRatioCell = dataRow.createCell(colNum++);
                quickRatioCell.setCellValue(ratio.getQuickRatio() != null ? ratio.getQuickRatio().doubleValue() : 0.0);
                quickRatioCell.setCellStyle(numberStyle);

                dataRow.createCell(colNum++).setCellValue(ratio.getDsoDays() != null ? ratio.getDsoDays() : 0);
                dataRow.createCell(colNum++).setCellValue(ratio.getDioDays() != null ? ratio.getDioDays() : 0);
                dataRow.createCell(colNum++).setCellValue(ratio.getDpoDays() != null ? ratio.getDpoDays() : 0);

                org.apache.poi.ss.usermodel.Cell debtEquityCell = dataRow.createCell(colNum++);
                debtEquityCell.setCellValue(ratio.getDebtToEquity() != null ? ratio.getDebtToEquity().doubleValue() : 0.0);
                debtEquityCell.setCellStyle(numberStyle);

                org.apache.poi.ss.usermodel.Cell debtRatioCell = dataRow.createCell(colNum++);
                debtRatioCell.setCellValue(ratio.getDebtRatioPct() != null ? ratio.getDebtRatioPct().doubleValue() : 0.0);
                debtRatioCell.setCellStyle(percentStyle);

                dataRow.createCell(colNum++).setCellValue(ratio.getCashConversionCycle() != null ? ratio.getCashConversionCycle() : 0);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            log.info("Export Excel de l'historique des ratios terminé - {} octets", baos.size());
            return baos.toByteArray();
        }
    }

    /**
     * Exporte le grand livre en CSV
     */
    public byte[] exportGeneralLedgerToCsv(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export du grand livre en CSV pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        List<com.predykt.accounting.domain.entity.GeneralLedger> entries =
            reportService.getGeneralLedgerRepository().findByCompanyAndEntryDateBetween(company, startDate, endDate);

        StringBuilder csv = new StringBuilder();

        // En-tête CSV
        csv.append("Date,Référence,Code Journal,Compte,Libellé,Débit,Crédit,Description,Verrouillé\n");

        // Données
        for (com.predykt.accounting.domain.entity.GeneralLedger entry : entries) {
            csv.append(entry.getEntryDate().format(DATE_FORMATTER)).append(",");
            csv.append(escapeCsv(entry.getReference())).append(",");
            csv.append(escapeCsv(entry.getJournalCode())).append(",");
            csv.append(entry.getAccount() != null ? entry.getAccount().getAccountNumber() : "").append(",");
            csv.append(entry.getAccount() != null ? escapeCsv(entry.getAccount().getAccountName()) : "").append(",");
            csv.append(formatAmount(entry.getDebitAmount())).append(",");
            csv.append(formatAmount(entry.getCreditAmount())).append(",");
            csv.append(escapeCsv(entry.getDescription())).append(",");
            csv.append(entry.getIsLocked() ? "Oui" : "Non").append("\n");
        }

        byte[] csvBytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        log.info("Export CSV du grand livre terminé - {} lignes, {} octets", entries.size(), csvBytes.length);

        return csvBytes;
    }

    /**
     * Exporte le grand livre en Excel
     */
    public byte[] exportGeneralLedgerToExcel(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export du grand livre en Excel pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        List<com.predykt.accounting.domain.entity.GeneralLedger> entries =
            reportService.getGeneralLedgerRepository().findByCompanyAndEntryDateBetween(company, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Grand Livre");

            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle dateStyle = workbook.createCellStyle();
            DataFormat dateFormat = workbook.createDataFormat();
            dateStyle.setDataFormat(dateFormat.getFormat("dd/mm/yyyy"));

            int rowNum = 0;

            // Titre
            Row titleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("GRAND LIVRE");
            titleCell.setCellStyle(titleStyle);

            // Entreprise
            Row companyRow = sheet.createRow(rowNum++);
            companyRow.createCell(0).setCellValue(company.getName());

            // Période
            Row periodRow = sheet.createRow(rowNum++);
            periodRow.createCell(0).setCellValue("Du " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER));

            rowNum++; // Ligne vide

            // En-têtes de colonnes
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {
                "Date", "Référence", "Code Journal", "Compte", "Libellé",
                "Débit", "Crédit", "Description", "Verrouillé"
            };

            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Données
            for (com.predykt.accounting.domain.entity.GeneralLedger entry : entries) {
                Row dataRow = sheet.createRow(rowNum++);
                int colNum = 0;

                // Date
                org.apache.poi.ss.usermodel.Cell dateCell = dataRow.createCell(colNum++);
                dateCell.setCellValue(entry.getEntryDate().toString());

                // Référence
                dataRow.createCell(colNum++).setCellValue(entry.getReference() != null ? entry.getReference() : "");

                // Code Journal
                dataRow.createCell(colNum++).setCellValue(entry.getJournalCode() != null ? entry.getJournalCode() : "");

                // Compte
                dataRow.createCell(colNum++).setCellValue(
                    entry.getAccount() != null ? entry.getAccount().getAccountNumber() : ""
                );

                // Libellé
                dataRow.createCell(colNum++).setCellValue(
                    entry.getAccount() != null ? entry.getAccount().getAccountName() : ""
                );

                // Débit
                org.apache.poi.ss.usermodel.Cell debitCell = dataRow.createCell(colNum++);
                debitCell.setCellValue(entry.getDebitAmount() != null ? entry.getDebitAmount().doubleValue() : 0.0);
                debitCell.setCellStyle(currencyStyle);

                // Crédit
                org.apache.poi.ss.usermodel.Cell creditCell = dataRow.createCell(colNum++);
                creditCell.setCellValue(entry.getCreditAmount() != null ? entry.getCreditAmount().doubleValue() : 0.0);
                creditCell.setCellStyle(currencyStyle);

                // Description
                dataRow.createCell(colNum++).setCellValue(entry.getDescription() != null ? entry.getDescription() : "");

                // Verrouillé
                dataRow.createCell(colNum++).setCellValue(entry.getIsLocked() ? "Oui" : "Non");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            log.info("Export Excel du grand livre terminé - {} lignes, {} octets", entries.size(), baos.size());
            return baos.toByteArray();
        }
    }

    /**
     * Échappe les valeurs CSV (guillemets et virgules)
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Exporte l'état de rapprochement bancaire en PDF (conforme OHADA)
     */
    public byte[] exportBankReconciliationToPdf(
        com.predykt.accounting.domain.entity.BankReconciliation reconciliation
    ) throws IOException {
        log.info("Export de l'état de rapprochement {} en PDF", reconciliation.getId());

        Company company = reconciliation.getCompany();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // En-tête
        document.add(new Paragraph("ÉTAT DE RAPPROCHEMENT BANCAIRE")
            .setFontSize(18)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(company.getName())
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Au " + reconciliation.getReconciliationDate().format(DATE_FORMATTER))
            .setFontSize(12)
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Compte: " + reconciliation.getBankAccountNumber() +
                                   (reconciliation.getBankName() != null ? " - " + reconciliation.getBankName() : ""))
            .setFontSize(11)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20));

        // SECTION A: Solde selon relevé bancaire
        document.add(new Paragraph("A) SOLDE SELON RELEVÉ BANCAIRE")
            .setFontSize(13)
            .setBold()
            .setMarginTop(15));

        Table sectionA = new Table(UnitValue.createPercentArray(new float[]{4, 2}))
            .useAllAvailableWidth();

        addTableRow(sectionA, "Solde selon relevé bancaire", formatAmount(reconciliation.getBankStatementBalance()));
        addTableRow(sectionA, "(+) Chèques émis non encaissés", formatAmount(reconciliation.getChequesIssuedNotCashed()));
        addTableRow(sectionA, "(-) Dépôts/virements en transit", formatAmount(reconciliation.getDepositsInTransit()));
        addTableRow(sectionA, "(+/-) Erreurs bancaires", formatAmount(reconciliation.getBankErrors()));
        addTableTotal(sectionA, "SOLDE BANCAIRE RECTIFIÉ (A)", formatAmount(reconciliation.getAdjustedBankBalance()));

        document.add(sectionA);

        // SECTION B: Solde selon livre
        document.add(new Paragraph("B) SOLDE SELON LIVRE COMPTABLE")
            .setFontSize(13)
            .setBold()
            .setMarginTop(20));

        Table sectionB = new Table(UnitValue.createPercentArray(new float[]{4, 2}))
            .useAllAvailableWidth();

        addTableRow(sectionB, "Solde selon livre (compte " +
            (reconciliation.getGlAccountNumber() != null ? reconciliation.getGlAccountNumber() : "52X") + ")",
            formatAmount(reconciliation.getBookBalance()));
        addTableRow(sectionB, "(+) Virements reçus non comptabilisés", formatAmount(reconciliation.getCreditsNotRecorded()));
        addTableRow(sectionB, "(-) Prélèvements non comptabilisés", formatAmount(reconciliation.getDebitsNotRecorded()));
        addTableRow(sectionB, "(-) Frais bancaires non enregistrés", formatAmount(reconciliation.getBankFeesNotRecorded()));
        addTableRow(sectionB, "(+/-) Erreurs comptables", formatAmount(reconciliation.getBookErrors()));
        addTableTotal(sectionB, "SOLDE LIVRE RECTIFIÉ (B)", formatAmount(reconciliation.getAdjustedBookBalance()));

        document.add(sectionB);

        // SECTION C: Écart
        document.add(new Paragraph("C) ÉCART")
            .setFontSize(13)
            .setBold()
            .setMarginTop(20));

        Table sectionC = new Table(UnitValue.createPercentArray(new float[]{4, 2}))
            .useAllAvailableWidth();

        addTableTotal(sectionC, "ÉCART (A - B)", formatAmount(reconciliation.getDifference()));

        com.itextpdf.layout.element.Cell statusCell = new com.itextpdf.layout.element.Cell();
        statusCell.add(new Paragraph(reconciliation.getIsBalanced() ?
            "✓ RAPPROCHEMENT ÉQUILIBRÉ" : "⚠ RAPPROCHEMENT NON ÉQUILIBRÉ")
            .setBold()
            .setFontColor(reconciliation.getIsBalanced() ?
                ColorConstants.GREEN : ColorConstants.RED));
        sectionC.addCell(statusCell);
        sectionC.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("")));

        document.add(sectionC);

        // Opérations en suspens (détail)
        if (!reconciliation.getPendingItems().isEmpty()) {
            document.add(new Paragraph("DÉTAIL DES OPÉRATIONS EN SUSPENS")
                .setFontSize(13)
                .setBold()
                .setMarginTop(20));

            Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{2, 3, 2, 1}))
                .useAllAvailableWidth();

            addTableHeader(itemsTable, "Date", "Description", "Type", "Montant");

            for (com.predykt.accounting.domain.entity.BankReconciliationItem item : reconciliation.getPendingItems()) {
                itemsTable.addCell(new com.itextpdf.layout.element.Cell().add(
                    new Paragraph(item.getTransactionDate().format(DATE_FORMATTER))));
                itemsTable.addCell(new com.itextpdf.layout.element.Cell().add(
                    new Paragraph(item.getDescription() != null ? item.getDescription() : "")));
                itemsTable.addCell(new com.itextpdf.layout.element.Cell().add(
                    new Paragraph(item.getItemType().getDisplayName())));
                itemsTable.addCell(new com.itextpdf.layout.element.Cell().add(
                    new Paragraph(formatAmount(item.getAmount()))).setTextAlignment(TextAlignment.RIGHT));
            }

            document.add(itemsTable);
        }

        // Pied de page avec signatures
        document.add(new Paragraph("\n\nPréparé par: " +
            (reconciliation.getPreparedBy() != null ? reconciliation.getPreparedBy() : "_______________"))
            .setFontSize(10)
            .setMarginTop(30));

        if (reconciliation.getApprovedBy() != null) {
            document.add(new Paragraph("Approuvé par: " + reconciliation.getApprovedBy())
                .setFontSize(10));
        }

        document.add(new Paragraph("Date d'édition: " + LocalDate.now().format(DATE_FORMATTER))
            .setFontSize(9)
            .setTextAlignment(TextAlignment.RIGHT)
            .setMarginTop(10));

        document.close();

        log.info("Export PDF de l'état de rapprochement terminé - {} octets", baos.size());
        return baos.toByteArray();
    }

    /**
     * Exporte l'état de rapprochement bancaire en Excel
     */
    public byte[] exportBankReconciliationToExcel(
        com.predykt.accounting.domain.entity.BankReconciliation reconciliation
    ) throws IOException {
        log.info("Export de l'état de rapprochement {} en Excel", reconciliation.getId());

        Company company = reconciliation.getCompany();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Rapprochement Bancaire");

            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            int rowNum = 0;

            // Titre
            Row titleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("ÉTAT DE RAPPROCHEMENT BANCAIRE");
            titleCell.setCellStyle(titleStyle);

            // Entreprise
            Row companyRow = sheet.createRow(rowNum++);
            companyRow.createCell(0).setCellValue(company.getName());

            // Date
            Row dateRow = sheet.createRow(rowNum++);
            dateRow.createCell(0).setCellValue("Au " + reconciliation.getReconciliationDate().format(DATE_FORMATTER));

            // Compte
            Row accountRow = sheet.createRow(rowNum++);
            accountRow.createCell(0).setCellValue("Compte: " + reconciliation.getBankAccountNumber() +
                (reconciliation.getBankName() != null ? " - " + reconciliation.getBankName() : ""));

            rowNum++; // Ligne vide

            // SECTION A
            Row sectionAHeader = sheet.createRow(rowNum++);
            sectionAHeader.createCell(0).setCellValue("A) SOLDE SELON RELEVÉ BANCAIRE");
            sectionAHeader.getCell(0).setCellStyle(headerStyle);

            addExcelRow(sheet, rowNum++, "Solde selon relevé bancaire", reconciliation.getBankStatementBalance(), currencyStyle);
            addExcelRow(sheet, rowNum++, "(+) Chèques émis non encaissés", reconciliation.getChequesIssuedNotCashed(), currencyStyle);
            addExcelRow(sheet, rowNum++, "(-) Dépôts/virements en transit", reconciliation.getDepositsInTransit(), currencyStyle);
            addExcelRow(sheet, rowNum++, "(+/-) Erreurs bancaires", reconciliation.getBankErrors(), currencyStyle);
            addExcelRow(sheet, rowNum++, "SOLDE BANCAIRE RECTIFIÉ (A)", reconciliation.getAdjustedBankBalance(), totalStyle);

            rowNum++; // Ligne vide

            // SECTION B
            Row sectionBHeader = sheet.createRow(rowNum++);
            sectionBHeader.createCell(0).setCellValue("B) SOLDE SELON LIVRE COMPTABLE");
            sectionBHeader.getCell(0).setCellStyle(headerStyle);

            addExcelRow(sheet, rowNum++, "Solde selon livre (compte " +
                (reconciliation.getGlAccountNumber() != null ? reconciliation.getGlAccountNumber() : "52X") + ")",
                reconciliation.getBookBalance(), currencyStyle);
            addExcelRow(sheet, rowNum++, "(+) Virements reçus non comptabilisés", reconciliation.getCreditsNotRecorded(), currencyStyle);
            addExcelRow(sheet, rowNum++, "(-) Prélèvements non comptabilisés", reconciliation.getDebitsNotRecorded(), currencyStyle);
            addExcelRow(sheet, rowNum++, "(-) Frais bancaires non enregistrés", reconciliation.getBankFeesNotRecorded(), currencyStyle);
            addExcelRow(sheet, rowNum++, "(+/-) Erreurs comptables", reconciliation.getBookErrors(), currencyStyle);
            addExcelRow(sheet, rowNum++, "SOLDE LIVRE RECTIFIÉ (B)", reconciliation.getAdjustedBookBalance(), totalStyle);

            rowNum++; // Ligne vide

            // SECTION C
            Row sectionCHeader = sheet.createRow(rowNum++);
            sectionCHeader.createCell(0).setCellValue("C) ÉCART");
            sectionCHeader.getCell(0).setCellStyle(headerStyle);

            addExcelRow(sheet, rowNum++, "ÉCART (A - B)", reconciliation.getDifference(), totalStyle);

            Row statusRow = sheet.createRow(rowNum++);
            statusRow.createCell(0).setCellValue(reconciliation.getIsBalanced() ?
                "✓ RAPPROCHEMENT ÉQUILIBRÉ" : "⚠ RAPPROCHEMENT NON ÉQUILIBRÉ");

            // Auto-size columns
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            log.info("Export Excel de l'état de rapprochement terminé - {} octets", baos.size());
            return baos.toByteArray();
        }
    }

    // ==================== BALANCE DE VÉRIFICATION ====================

    /**
     * Exporte la balance de vérification en PDF
     */
    public byte[] exportTrialBalanceToPdf(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export de la balance de vérification en PDF pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        List<GeneralLedgerService.TrialBalanceEntry> trialBalance =
            generalLedgerService.getTrialBalance(companyId, startDate, endDate);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // En-tête
        document.add(new Paragraph("BALANCE DE VÉRIFICATION")
            .setFontSize(18)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(company.getName())
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Du " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER))
            .setFontSize(12)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20));

        // Table principale
        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 4, 2, 2, 2, 2}))
            .useAllAvailableWidth();

        addTableHeader(table, "Compte", "Libellé", "Débit", "Crédit", "Solde Débit", "Solde Crédit");

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal totalBalanceDebit = BigDecimal.ZERO;
        BigDecimal totalBalanceCredit = BigDecimal.ZERO;

        for (GeneralLedgerService.TrialBalanceEntry entry : trialBalance) {
            BigDecimal balanceDebit = BigDecimal.ZERO;
            BigDecimal balanceCredit = BigDecimal.ZERO;

            BigDecimal balance = entry.totalDebit().subtract(entry.totalCredit());
            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                balanceDebit = balance;
            } else {
                balanceCredit = balance.abs();
            }

            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(entry.accountNumber()).setFontSize(9)));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(entry.accountName()).setFontSize(9)));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(formatAmount(entry.totalDebit())).setFontSize(9))
                .setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(formatAmount(entry.totalCredit())).setFontSize(9))
                .setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(formatAmount(balanceDebit)).setFontSize(9))
                .setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(formatAmount(balanceCredit)).setFontSize(9))
                .setTextAlignment(TextAlignment.RIGHT));

            totalDebit = totalDebit.add(entry.totalDebit());
            totalCredit = totalCredit.add(entry.totalCredit());
            totalBalanceDebit = totalBalanceDebit.add(balanceDebit);
            totalBalanceCredit = totalBalanceCredit.add(balanceCredit);
        }

        // Ligne de totaux
        com.itextpdf.layout.element.Cell totalLabelCell = new com.itextpdf.layout.element.Cell(1, 2)
            .add(new Paragraph("TOTAUX").setBold())
            .setBackgroundColor(ColorConstants.LIGHT_GRAY);
        table.addCell(totalLabelCell);

        addTableCell(table, formatAmount(totalDebit), true);
        addTableCell(table, formatAmount(totalCredit), true);
        addTableCell(table, formatAmount(totalBalanceDebit), true);
        addTableCell(table, formatAmount(totalBalanceCredit), true);

        document.add(table);

        // Vérification équilibre
        boolean isBalanced = totalDebit.compareTo(totalCredit) == 0 &&
                           totalBalanceDebit.compareTo(totalBalanceCredit) == 0;

        document.add(new Paragraph(isBalanced ? "\n✓ Balance équilibrée" : "\n⚠ Balance déséquilibrée")
            .setFontSize(12)
            .setBold()
            .setFontColor(isBalanced ? ColorConstants.GREEN : ColorConstants.RED)
            .setMarginTop(10));

        // Pied de page
        document.add(new Paragraph("Document généré le " + LocalDate.now().format(DATE_FORMATTER))
            .setFontSize(10)
            .setTextAlignment(TextAlignment.RIGHT)
            .setMarginTop(20));

        document.close();

        log.info("Export PDF de la balance de vérification terminé - {} octets", baos.size());
        return baos.toByteArray();
    }

    /**
     * Exporte la balance de vérification en Excel
     */
    public byte[] exportTrialBalanceToExcel(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export de la balance de vérification en Excel pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        List<GeneralLedgerService.TrialBalanceEntry> trialBalance =
            generalLedgerService.getTrialBalance(companyId, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Balance de Vérification");

            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            int rowNum = 0;

            // Titre
            Row titleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BALANCE DE VÉRIFICATION");
            titleCell.setCellStyle(titleStyle);

            // Entreprise
            Row companyRow = sheet.createRow(rowNum++);
            companyRow.createCell(0).setCellValue(company.getName());

            // Période
            Row periodRow = sheet.createRow(rowNum++);
            periodRow.createCell(0).setCellValue("Du " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER));

            rowNum++; // Ligne vide

            // En-têtes de colonnes
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Compte", "Libellé", "Débit", "Crédit", "Solde Débit", "Solde Crédit"};

            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;
            BigDecimal totalBalanceDebit = BigDecimal.ZERO;
            BigDecimal totalBalanceCredit = BigDecimal.ZERO;

            // Données
            for (GeneralLedgerService.TrialBalanceEntry entry : trialBalance) {
                Row dataRow = sheet.createRow(rowNum++);
                int colNum = 0;

                BigDecimal balanceDebit = BigDecimal.ZERO;
                BigDecimal balanceCredit = BigDecimal.ZERO;

                BigDecimal balance = entry.totalDebit().subtract(entry.totalCredit());
                if (balance.compareTo(BigDecimal.ZERO) > 0) {
                    balanceDebit = balance;
                } else {
                    balanceCredit = balance.abs();
                }

                dataRow.createCell(colNum++).setCellValue(entry.accountNumber());
                dataRow.createCell(colNum++).setCellValue(entry.accountName());

                org.apache.poi.ss.usermodel.Cell debitCell = dataRow.createCell(colNum++);
                debitCell.setCellValue(entry.totalDebit().doubleValue());
                debitCell.setCellStyle(currencyStyle);

                org.apache.poi.ss.usermodel.Cell creditCell = dataRow.createCell(colNum++);
                creditCell.setCellValue(entry.totalCredit().doubleValue());
                creditCell.setCellStyle(currencyStyle);

                org.apache.poi.ss.usermodel.Cell balDebitCell = dataRow.createCell(colNum++);
                balDebitCell.setCellValue(balanceDebit.doubleValue());
                balDebitCell.setCellStyle(currencyStyle);

                org.apache.poi.ss.usermodel.Cell balCreditCell = dataRow.createCell(colNum++);
                balCreditCell.setCellValue(balanceCredit.doubleValue());
                balCreditCell.setCellStyle(currencyStyle);

                totalDebit = totalDebit.add(entry.totalDebit());
                totalCredit = totalCredit.add(entry.totalCredit());
                totalBalanceDebit = totalBalanceDebit.add(balanceDebit);
                totalBalanceCredit = totalBalanceCredit.add(balanceCredit);
            }

            // Ligne de totaux
            Row totalRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell totalLabelCell = totalRow.createCell(0);
            totalLabelCell.setCellValue("TOTAUX");
            totalLabelCell.setCellStyle(totalStyle);

            totalRow.createCell(1).setCellStyle(totalStyle);

            org.apache.poi.ss.usermodel.Cell totalDebitCell = totalRow.createCell(2);
            totalDebitCell.setCellValue(totalDebit.doubleValue());
            totalDebitCell.setCellStyle(totalStyle);

            org.apache.poi.ss.usermodel.Cell totalCreditCell = totalRow.createCell(3);
            totalCreditCell.setCellValue(totalCredit.doubleValue());
            totalCreditCell.setCellStyle(totalStyle);

            org.apache.poi.ss.usermodel.Cell totalBalDebitCell = totalRow.createCell(4);
            totalBalDebitCell.setCellValue(totalBalanceDebit.doubleValue());
            totalBalDebitCell.setCellStyle(totalStyle);

            org.apache.poi.ss.usermodel.Cell totalBalCreditCell = totalRow.createCell(5);
            totalBalCreditCell.setCellValue(totalBalanceCredit.doubleValue());
            totalBalCreditCell.setCellStyle(totalStyle);

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            log.info("Export Excel de la balance de vérification terminé - {} octets", baos.size());
            return baos.toByteArray();
        }
    }

    // ==================== GRAND LIVRE (PDF) ====================

    /**
     * Exporte le grand livre complet en PDF
     */
    public byte[] exportGeneralLedgerToPdf(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export du grand livre complet en PDF pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        List<com.predykt.accounting.domain.entity.GeneralLedger> entries =
            reportService.getGeneralLedgerRepository().findByCompanyAndEntryDateBetween(company, startDate, endDate);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // En-tête
        document.add(new Paragraph("GRAND LIVRE")
            .setFontSize(18)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(company.getName())
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Du " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER))
            .setFontSize(12)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20));

        // Table
        Table table = new Table(UnitValue.createPercentArray(new float[]{1.5f, 2, 1.5f, 1.5f, 3, 1.5f, 1.5f}))
            .useAllAvailableWidth();

        addTableHeader(table, "Date", "Réf", "Journal", "Compte", "Libellé", "Débit", "Crédit");

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (com.predykt.accounting.domain.entity.GeneralLedger entry : entries) {
            table.addCell(new com.itextpdf.layout.element.Cell().add(
                new Paragraph(entry.getEntryDate().format(DATE_FORMATTER)).setFontSize(8)));
            table.addCell(new com.itextpdf.layout.element.Cell().add(
                new Paragraph(entry.getReference() != null ? entry.getReference() : "").setFontSize(8)));
            table.addCell(new com.itextpdf.layout.element.Cell().add(
                new Paragraph(entry.getJournalCode() != null ? entry.getJournalCode() : "").setFontSize(8)));
            table.addCell(new com.itextpdf.layout.element.Cell().add(
                new Paragraph(entry.getAccount() != null ? entry.getAccount().getAccountNumber() : "").setFontSize(8)));
            table.addCell(new com.itextpdf.layout.element.Cell().add(
                new Paragraph(entry.getDescription() != null ? entry.getDescription() : "").setFontSize(8)));
            table.addCell(new com.itextpdf.layout.element.Cell().add(
                new Paragraph(formatAmount(entry.getDebitAmount())).setFontSize(8))
                .setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new com.itextpdf.layout.element.Cell().add(
                new Paragraph(formatAmount(entry.getCreditAmount())).setFontSize(8))
                .setTextAlignment(TextAlignment.RIGHT));

            totalDebit = totalDebit.add(entry.getDebitAmount());
            totalCredit = totalCredit.add(entry.getCreditAmount());
        }

        // Ligne de totaux
        com.itextpdf.layout.element.Cell totalLabelCell = new com.itextpdf.layout.element.Cell(1, 5)
            .add(new Paragraph("TOTAUX").setBold())
            .setBackgroundColor(ColorConstants.LIGHT_GRAY);
        table.addCell(totalLabelCell);

        addTableCell(table, formatAmount(totalDebit), true);
        addTableCell(table, formatAmount(totalCredit), true);

        document.add(table);

        // Statistiques
        document.add(new Paragraph("\nNombre d'écritures: " + entries.size())
            .setFontSize(10)
            .setMarginTop(10));

        boolean isBalanced = totalDebit.compareTo(totalCredit) == 0;
        document.add(new Paragraph(isBalanced ? "✓ Grand livre équilibré" : "⚠ Grand livre déséquilibré")
            .setFontSize(10)
            .setBold()
            .setFontColor(isBalanced ? ColorConstants.GREEN : ColorConstants.RED));

        // Pied de page
        document.add(new Paragraph("Document généré le " + LocalDate.now().format(DATE_FORMATTER))
            .setFontSize(10)
            .setTextAlignment(TextAlignment.RIGHT)
            .setMarginTop(20));

        document.close();

        log.info("Export PDF du grand livre terminé - {} écritures, {} octets", entries.size(), baos.size());
        return baos.toByteArray();
    }

    /**
     * Ajouter une cellule avec style (helper)
     */
    private void addTableCell(Table table, String value, boolean isBold) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
            .add(new Paragraph(value).setFontSize(9));
        if (isBold) {
            cell.setBold();
            cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        }
        cell.setTextAlignment(TextAlignment.RIGHT);
        table.addCell(cell);
    }

    // ==================== TAFIRE (PRIORITÉ 2) ====================

    /**
     * Exporte le TAFIRE en PDF (format OHADA)
     */
    public byte[] exportTAFIREToPdf(Long companyId, Integer fiscalYear) throws IOException {
        log.info("Export du TAFIRE en PDF pour l'entreprise {} - exercice {}", companyId, fiscalYear);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        com.predykt.accounting.dto.response.TAFIREResponse tafire = tafireService.generateTAFIRE(companyId, fiscalYear);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // En-tête
        document.add(new Paragraph("TABLEAU FINANCIER DES RESSOURCES ET EMPLOIS (TAFIRE)")
            .setFontSize(16)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(company.getName())
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Exercice " + fiscalYear)
            .setFontSize(12)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20));

        // I. RESSOURCES STABLES
        document.add(new Paragraph("I. RESSOURCES STABLES")
            .setFontSize(13)
            .setBold()
            .setMarginTop(10));

        Table ressourcesTable = new Table(UnitValue.createPercentArray(new float[]{4, 2}))
            .useAllAvailableWidth();

        addTableHeader(ressourcesTable, "Description", "Montant (" + tafire.getCurrency() + ")");

        // A. Ressources internes
        addTableRow(ressourcesTable, "A. Ressources internes", "");
        addTableRow(ressourcesTable, "  Capacité d'autofinancement (CAF)",
            formatAmount(tafire.getRessourcesStables().getCaf().getCafTotal()));
        addTableRow(ressourcesTable, "  Cessions d'immobilisations",
            formatAmount(tafire.getRessourcesStables().getCessionsImmobilisations()));

        // B. Ressources externes
        addTableRow(ressourcesTable, "B. Ressources externes", "");
        addTableRow(ressourcesTable, "  Augmentation de capital",
            formatAmount(tafire.getRessourcesStables().getAugmentationCapital()));
        addTableRow(ressourcesTable, "  Emprunts à long terme",
            formatAmount(tafire.getRessourcesStables().getEmpruntsLongTerme()));
        addTableRow(ressourcesTable, "  Subventions d'investissement",
            formatAmount(tafire.getRessourcesStables().getSubventionsInvestissement()));

        addTableTotal(ressourcesTable, "TOTAL RESSOURCES STABLES",
            formatAmount(tafire.getRessourcesStables().getTotalRessourcesStables()));

        document.add(ressourcesTable);

        // II. EMPLOIS STABLES
        document.add(new Paragraph("II. EMPLOIS STABLES")
            .setFontSize(13)
            .setBold()
            .setMarginTop(20));

        Table emploisTable = new Table(UnitValue.createPercentArray(new float[]{4, 2}))
            .useAllAvailableWidth();

        addTableHeader(emploisTable, "Description", "Montant (" + tafire.getCurrency() + ")");

        addTableRow(emploisTable, "Acquisitions immobilisations incorporelles",
            formatAmount(tafire.getEmploisStables().getAcquisitionsImmobilisationsIncorporelles()));
        addTableRow(emploisTable, "Acquisitions immobilisations corporelles",
            formatAmount(tafire.getEmploisStables().getAcquisitionsImmobilisationsCorporelles()));
        addTableRow(emploisTable, "Acquisitions immobilisations financières",
            formatAmount(tafire.getEmploisStables().getAcquisitionsImmobilisationsFinancieres()));
        addTableRow(emploisTable, "Remboursements emprunts long terme",
            formatAmount(tafire.getEmploisStables().getRemboursementsEmpruntsLongTerme()));
        addTableRow(emploisTable, "Dividendes versés",
            formatAmount(tafire.getEmploisStables().getDividendesVerses()));

        addTableTotal(emploisTable, "TOTAL EMPLOIS STABLES",
            formatAmount(tafire.getEmploisStables().getTotalEmploisStables()));

        document.add(emploisTable);

        // III. VARIATION FRNG
        document.add(new Paragraph("III. VARIATION DU FONDS DE ROULEMENT NET GLOBAL")
            .setFontSize(13)
            .setBold()
            .setMarginTop(20));

        Table frngTable = new Table(UnitValue.createPercentArray(new float[]{4, 2}))
            .useAllAvailableWidth();

        addTableTotal(frngTable, "VARIATION FRNG (Ressources - Emplois)",
            formatAmount(tafire.getVariationFRNG()));

        document.add(frngTable);

        // IV. VARIATION BFR
        document.add(new Paragraph("IV. VARIATION DU BESOIN EN FONDS DE ROULEMENT")
            .setFontSize(13)
            .setBold()
            .setMarginTop(20));

        Table bfrTable = new Table(UnitValue.createPercentArray(new float[]{4, 2}))
            .useAllAvailableWidth();

        addTableHeader(bfrTable, "Description", "Montant (" + tafire.getCurrency() + ")");

        addTableRow(bfrTable, "BFR exercice N",
            formatAmount(tafire.getVariationBFR().getBfrExerciceN()));
        addTableRow(bfrTable, "BFR exercice N-1",
            formatAmount(tafire.getVariationBFR().getBfrExerciceN1()));

        addTableTotal(bfrTable, "VARIATION BFR (N - N-1)",
            formatAmount(tafire.getVariationBFR().getVariationBFR()));

        document.add(bfrTable);

        // V. VARIATION TRÉSORERIE
        document.add(new Paragraph("V. VARIATION DE LA TRÉSORERIE")
            .setFontSize(13)
            .setBold()
            .setMarginTop(20));

        Table tresoTable = new Table(UnitValue.createPercentArray(new float[]{4, 2}))
            .useAllAvailableWidth();

        addTableHeader(tresoTable, "Description", "Montant (" + tafire.getCurrency() + ")");

        addTableRow(tresoTable, "Variation FRNG",
            formatAmount(tafire.getVariationTresorerie().getVariationFRNG()));
        addTableRow(tresoTable, "Variation BFR",
            formatAmount(tafire.getVariationTresorerie().getVariationBFR()));

        addTableTotal(tresoTable, "VARIATION TRÉSORERIE (FRNG - BFR)",
            formatAmount(tafire.getVariationTresorerie().getVariationTresorerie()));

        addTableRow(tresoTable, "", "");
        addTableRow(tresoTable, "Trésorerie début exercice",
            formatAmount(tafire.getVariationTresorerie().getTresorerieDebut()));
        addTableRow(tresoTable, "Trésorerie fin exercice",
            formatAmount(tafire.getVariationTresorerie().getTresorerieFin()));

        document.add(tresoTable);

        // Vérification
        if (tafire.getIsBalanced()) {
            document.add(new Paragraph("\n✓ TAFIRE vérifié et équilibré")
                .setFontSize(11)
                .setBold()
                .setFontColor(ColorConstants.GREEN));
        } else {
            document.add(new Paragraph("\n⚠ ATTENTION: Écart de cohérence détecté")
                .setFontSize(11)
                .setBold()
                .setFontColor(ColorConstants.RED));
        }

        // Analyse automatique
        if (tafire.getAnalysisComment() != null && !tafire.getAnalysisComment().isEmpty()) {
            document.add(new Paragraph("\nANALYSE")
                .setFontSize(12)
                .setBold()
                .setMarginTop(15));
            document.add(new Paragraph(tafire.getAnalysisComment())
                .setFontSize(10));
        }

        // Pied de page
        document.add(new Paragraph("\nDocument généré le " + LocalDate.now().format(DATE_FORMATTER))
            .setFontSize(9)
            .setTextAlignment(TextAlignment.RIGHT)
            .setMarginTop(20));

        document.add(new Paragraph("Rapport conforme OHADA")
            .setFontSize(9)
            .setTextAlignment(TextAlignment.RIGHT));

        document.close();

        log.info("Export PDF du TAFIRE terminé - {} octets", baos.size());
        return baos.toByteArray();
    }

    /**
     * Exporte le TAFIRE en Excel
     */
    public byte[] exportTAFIREToExcel(Long companyId, Integer fiscalYear) throws IOException {
        log.info("Export du TAFIRE en Excel pour l'entreprise {} - exercice {}", companyId, fiscalYear);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        com.predykt.accounting.dto.response.TAFIREResponse tafire = tafireService.generateTAFIRE(companyId, fiscalYear);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TAFIRE " + fiscalYear);

            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            int rowNum = 0;

            // Titre
            Row titleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("TABLEAU FINANCIER DES RESSOURCES ET EMPLOIS (TAFIRE)");
            titleCell.setCellStyle(titleStyle);

            // Entreprise
            Row companyRow = sheet.createRow(rowNum++);
            companyRow.createCell(0).setCellValue(company.getName());

            // Exercice
            Row yearRow = sheet.createRow(rowNum++);
            yearRow.createCell(0).setCellValue("Exercice " + fiscalYear);

            rowNum++; // Ligne vide

            // I. RESSOURCES STABLES
            Row ressourcesHeader = sheet.createRow(rowNum++);
            ressourcesHeader.createCell(0).setCellValue("I. RESSOURCES STABLES");
            ressourcesHeader.getCell(0).setCellStyle(headerStyle);

            Row ressourcesColHeader = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell rcol1 = ressourcesColHeader.createCell(0);
            rcol1.setCellValue("Description");
            rcol1.setCellStyle(headerStyle);
            org.apache.poi.ss.usermodel.Cell rcol2 = ressourcesColHeader.createCell(1);
            rcol2.setCellValue("Montant (" + tafire.getCurrency() + ")");
            rcol2.setCellStyle(headerStyle);

            addExcelRow(sheet, rowNum++, "A. Ressources internes", BigDecimal.ZERO, currencyStyle);
            addExcelRow(sheet, rowNum++, "  Capacité d'autofinancement (CAF)",
                tafire.getRessourcesStables().getCaf().getCafTotal(), currencyStyle);
            addExcelRow(sheet, rowNum++, "  Cessions d'immobilisations",
                tafire.getRessourcesStables().getCessionsImmobilisations(), currencyStyle);
            addExcelRow(sheet, rowNum++, "B. Ressources externes", BigDecimal.ZERO, currencyStyle);
            addExcelRow(sheet, rowNum++, "  Augmentation de capital",
                tafire.getRessourcesStables().getAugmentationCapital(), currencyStyle);
            addExcelRow(sheet, rowNum++, "  Emprunts à long terme",
                tafire.getRessourcesStables().getEmpruntsLongTerme(), currencyStyle);
            addExcelRow(sheet, rowNum++, "  Subventions d'investissement",
                tafire.getRessourcesStables().getSubventionsInvestissement(), currencyStyle);
            addExcelRow(sheet, rowNum++, "TOTAL RESSOURCES STABLES",
                tafire.getRessourcesStables().getTotalRessourcesStables(), totalStyle);

            rowNum++; // Ligne vide

            // II. EMPLOIS STABLES
            Row emploisHeader = sheet.createRow(rowNum++);
            emploisHeader.createCell(0).setCellValue("II. EMPLOIS STABLES");
            emploisHeader.getCell(0).setCellStyle(headerStyle);

            Row emploisColHeader = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell ecol1 = emploisColHeader.createCell(0);
            ecol1.setCellValue("Description");
            ecol1.setCellStyle(headerStyle);
            org.apache.poi.ss.usermodel.Cell ecol2 = emploisColHeader.createCell(1);
            ecol2.setCellValue("Montant (" + tafire.getCurrency() + ")");
            ecol2.setCellStyle(headerStyle);

            addExcelRow(sheet, rowNum++, "Acquisitions immobilisations incorporelles",
                tafire.getEmploisStables().getAcquisitionsImmobilisationsIncorporelles(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Acquisitions immobilisations corporelles",
                tafire.getEmploisStables().getAcquisitionsImmobilisationsCorporelles(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Acquisitions immobilisations financières",
                tafire.getEmploisStables().getAcquisitionsImmobilisationsFinancieres(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Remboursements emprunts long terme",
                tafire.getEmploisStables().getRemboursementsEmpruntsLongTerme(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Dividendes versés",
                tafire.getEmploisStables().getDividendesVerses(), currencyStyle);
            addExcelRow(sheet, rowNum++, "TOTAL EMPLOIS STABLES",
                tafire.getEmploisStables().getTotalEmploisStables(), totalStyle);

            rowNum++; // Ligne vide

            // III. VARIATION FRNG
            Row frngHeader = sheet.createRow(rowNum++);
            frngHeader.createCell(0).setCellValue("III. VARIATION FRNG");
            frngHeader.getCell(0).setCellStyle(headerStyle);

            addExcelRow(sheet, rowNum++, "VARIATION FRNG (Ressources - Emplois)",
                tafire.getVariationFRNG(), totalStyle);

            rowNum++; // Ligne vide

            // IV. VARIATION BFR
            Row bfrHeader = sheet.createRow(rowNum++);
            bfrHeader.createCell(0).setCellValue("IV. VARIATION BFR");
            bfrHeader.getCell(0).setCellStyle(headerStyle);

            addExcelRow(sheet, rowNum++, "BFR exercice N",
                tafire.getVariationBFR().getBfrExerciceN(), currencyStyle);
            addExcelRow(sheet, rowNum++, "BFR exercice N-1",
                tafire.getVariationBFR().getBfrExerciceN1(), currencyStyle);
            addExcelRow(sheet, rowNum++, "VARIATION BFR (N - N-1)",
                tafire.getVariationBFR().getVariationBFR(), totalStyle);

            rowNum++; // Ligne vide

            // V. VARIATION TRÉSORERIE
            Row tresoHeader = sheet.createRow(rowNum++);
            tresoHeader.createCell(0).setCellValue("V. VARIATION TRÉSORERIE");
            tresoHeader.getCell(0).setCellStyle(headerStyle);

            addExcelRow(sheet, rowNum++, "Variation FRNG",
                tafire.getVariationTresorerie().getVariationFRNG(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Variation BFR",
                tafire.getVariationTresorerie().getVariationBFR(), currencyStyle);
            addExcelRow(sheet, rowNum++, "VARIATION TRÉSORERIE (FRNG - BFR)",
                tafire.getVariationTresorerie().getVariationTresorerie(), totalStyle);

            rowNum++; // Ligne vide
            addExcelRow(sheet, rowNum++, "Trésorerie début exercice",
                tafire.getVariationTresorerie().getTresorerieDebut(), currencyStyle);
            addExcelRow(sheet, rowNum++, "Trésorerie fin exercice",
                tafire.getVariationTresorerie().getTresorerieFin(), currencyStyle);

            // Auto-size columns
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            log.info("Export Excel du TAFIRE terminé - {} octets", baos.size());
            return baos.toByteArray();
        }
    }

    // ==================== JOURNAUX AUXILIAIRES (PRIORITÉ 2) ====================

    /**
     * Exporte le journal des ventes (VE) en PDF
     */
    public byte[] exportSalesJournalToPdf(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export du journal des ventes en PDF pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        com.predykt.accounting.dto.response.AuxiliaryJournalResponse journal =
            auxiliaryJournalsService.getSalesJournal(companyId, startDate, endDate);

        return exportAuxiliaryJournalToPdf(company, journal, "JOURNAL DES VENTES (VE)");
    }

    /**
     * Exporte le journal des achats (AC) en PDF
     */
    public byte[] exportPurchasesJournalToPdf(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export du journal des achats en PDF pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        com.predykt.accounting.dto.response.AuxiliaryJournalResponse journal =
            auxiliaryJournalsService.getPurchasesJournal(companyId, startDate, endDate);

        return exportAuxiliaryJournalToPdf(company, journal, "JOURNAL DES ACHATS (AC)");
    }

    /**
     * Exporte le journal de banque (BQ) en PDF
     */
    public byte[] exportBankJournalToPdf(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export du journal de banque en PDF pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        com.predykt.accounting.dto.response.AuxiliaryJournalResponse journal =
            auxiliaryJournalsService.getBankJournal(companyId, startDate, endDate);

        return exportAuxiliaryJournalToPdf(company, journal, "JOURNAL DE BANQUE (BQ)");
    }

    /**
     * Exporte le journal de caisse (CA) en PDF
     */
    public byte[] exportCashJournalToPdf(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export du journal de caisse en PDF pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        com.predykt.accounting.dto.response.AuxiliaryJournalResponse journal =
            auxiliaryJournalsService.getCashJournal(companyId, startDate, endDate);

        return exportAuxiliaryJournalToPdf(company, journal, "JOURNAL DE CAISSE (CA)");
    }

    /**
     * Exporte le journal des opérations diverses (OD) en PDF
     */
    public byte[] exportGeneralJournalToPdf(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export du journal des opérations diverses en PDF pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        com.predykt.accounting.dto.response.AuxiliaryJournalResponse journal =
            auxiliaryJournalsService.getGeneralJournal(companyId, startDate, endDate);

        return exportAuxiliaryJournalToPdf(company, journal, "JOURNAL DES OPÉRATIONS DIVERSES (OD)");
    }

    /**
     * Exporte le journal à nouveaux (AN) en PDF
     */
    public byte[] exportOpeningJournalToPdf(Long companyId, Integer fiscalYear) throws IOException {
        log.info("Export du journal à nouveaux en PDF pour l'entreprise {} - exercice {}",
            companyId, fiscalYear);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        com.predykt.accounting.dto.response.AuxiliaryJournalResponse journal =
            auxiliaryJournalsService.getOpeningJournal(companyId, fiscalYear);

        return exportAuxiliaryJournalToPdf(company, journal, "JOURNAL À NOUVEAUX (AN)");
    }

    /**
     * Méthode générique pour exporter un journal auxiliaire en PDF
     */
    private byte[] exportAuxiliaryJournalToPdf(Company company,
                                              com.predykt.accounting.dto.response.AuxiliaryJournalResponse journal,
                                              String title) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // En-tête
        document.add(new Paragraph(title)
            .setFontSize(16)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(company.getName())
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER));

        String period = journal.getStartDate() != null && journal.getEndDate() != null
            ? "Du " + journal.getStartDate().format(DATE_FORMATTER) + " au " + journal.getEndDate().format(DATE_FORMATTER)
            : "Exercice complet";

        document.add(new Paragraph(period)
            .setFontSize(12)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20));

        // Table des écritures
        Table table = new Table(UnitValue.createPercentArray(new float[]{1.5f, 2, 1.5f, 3, 1.5f, 1.5f}))
            .useAllAvailableWidth();

        addTableHeader(table, "Date", "N° Pièce", "Compte", "Libellé", "Débit", "Crédit");

        for (com.predykt.accounting.dto.response.AuxiliaryJournalResponse.JournalEntry entry : journal.getEntries()) {
            table.addCell(new com.itextpdf.layout.element.Cell().add(
                new Paragraph(entry.getEntryDate().format(DATE_FORMATTER)).setFontSize(8)));
            table.addCell(new com.itextpdf.layout.element.Cell().add(
                new Paragraph(entry.getPieceNumber() != null ? entry.getPieceNumber() : "").setFontSize(8)));
            table.addCell(new com.itextpdf.layout.element.Cell().add(
                new Paragraph(entry.getAccountNumber() != null ? entry.getAccountNumber() : "").setFontSize(8)));
            table.addCell(new com.itextpdf.layout.element.Cell().add(
                new Paragraph(entry.getDescription() != null ? entry.getDescription() : "").setFontSize(8)));
            table.addCell(new com.itextpdf.layout.element.Cell().add(
                new Paragraph(formatAmount(entry.getDebitAmount())).setFontSize(8))
                .setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new com.itextpdf.layout.element.Cell().add(
                new Paragraph(formatAmount(entry.getCreditAmount())).setFontSize(8))
                .setTextAlignment(TextAlignment.RIGHT));
        }

        // Ligne de totaux
        com.itextpdf.layout.element.Cell totalLabelCell = new com.itextpdf.layout.element.Cell(1, 4)
            .add(new Paragraph("TOTAUX").setBold())
            .setBackgroundColor(ColorConstants.LIGHT_GRAY);
        table.addCell(totalLabelCell);

        addTableCell(table, formatAmount(journal.getTotalDebit()), true);
        addTableCell(table, formatAmount(journal.getTotalCredit()), true);

        document.add(table);

        // Statistiques
        document.add(new Paragraph("\nSTATISTIQUES")
            .setFontSize(12)
            .setBold()
            .setMarginTop(15));

        document.add(new Paragraph("Nombre d'écritures: " + journal.getNumberOfEntries())
            .setFontSize(10));

        if (journal.getStatistics() != null) {
            com.predykt.accounting.dto.response.AuxiliaryJournalResponse.JournalStatistics stats = journal.getStatistics();

            if (stats.getTotalSalesTTC() != null) {
                document.add(new Paragraph("Total ventes TTC: " + formatAmount(stats.getTotalSalesTTC()) + " " + journal.getCurrency())
                    .setFontSize(10));
                document.add(new Paragraph("TVA collectée: " + formatAmount(stats.getTotalVATCollected()) + " " + journal.getCurrency())
                    .setFontSize(10));
            }

            if (stats.getTotalPurchasesTTC() != null) {
                document.add(new Paragraph("Total achats TTC: " + formatAmount(stats.getTotalPurchasesTTC()) + " " + journal.getCurrency())
                    .setFontSize(10));
                document.add(new Paragraph("TVA déductible: " + formatAmount(stats.getTotalVATDeductible()) + " " + journal.getCurrency())
                    .setFontSize(10));
            }

            if (stats.getNetCashFlow() != null) {
                document.add(new Paragraph("Flux net: " + formatAmount(stats.getNetCashFlow()) + " " + journal.getCurrency())
                    .setFontSize(10));
                document.add(new Paragraph("Solde d'ouverture: " + formatAmount(stats.getOpeningBalance()) + " " + journal.getCurrency())
                    .setFontSize(10));
                document.add(new Paragraph("Solde de clôture: " + formatAmount(stats.getClosingBalance()) + " " + journal.getCurrency())
                    .setFontSize(10));
            }
        }

        // Vérification équilibre
        if (journal.getIsBalanced() != null) {
            document.add(new Paragraph(journal.getIsBalanced() ? "\n✓ Journal équilibré" : "\n⚠ Journal déséquilibré")
                .setFontSize(11)
                .setBold()
                .setFontColor(journal.getIsBalanced() ? ColorConstants.GREEN : ColorConstants.RED)
                .setMarginTop(10));
        }

        // Pied de page
        document.add(new Paragraph("\nDocument généré le " + LocalDate.now().format(DATE_FORMATTER))
            .setFontSize(9)
            .setTextAlignment(TextAlignment.RIGHT)
            .setMarginTop(20));

        document.add(new Paragraph("Journal conforme OHADA")
            .setFontSize(9)
            .setTextAlignment(TextAlignment.RIGHT));

        document.close();

        log.info("Export PDF du journal {} terminé - {} écritures, {} octets",
            journal.getJournalCode(), journal.getNumberOfEntries(), baos.size());

        return baos.toByteArray();
    }

    /**
     * Exporte le journal des ventes (VE) en Excel
     */
    public byte[] exportSalesJournalToExcel(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        com.predykt.accounting.dto.response.AuxiliaryJournalResponse journal =
            auxiliaryJournalsService.getSalesJournal(companyId, startDate, endDate);
        return exportAuxiliaryJournalToExcel(companyId, journal, "Journal Ventes");
    }

    /**
     * Exporte le journal des achats (AC) en Excel
     */
    public byte[] exportPurchasesJournalToExcel(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        com.predykt.accounting.dto.response.AuxiliaryJournalResponse journal =
            auxiliaryJournalsService.getPurchasesJournal(companyId, startDate, endDate);
        return exportAuxiliaryJournalToExcel(companyId, journal, "Journal Achats");
    }

    /**
     * Exporte le journal de banque (BQ) en Excel
     */
    public byte[] exportBankJournalToExcel(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        com.predykt.accounting.dto.response.AuxiliaryJournalResponse journal =
            auxiliaryJournalsService.getBankJournal(companyId, startDate, endDate);
        return exportAuxiliaryJournalToExcel(companyId, journal, "Journal Banque");
    }

    /**
     * Exporte le journal de caisse (CA) en Excel
     */
    public byte[] exportCashJournalToExcel(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        com.predykt.accounting.dto.response.AuxiliaryJournalResponse journal =
            auxiliaryJournalsService.getCashJournal(companyId, startDate, endDate);
        return exportAuxiliaryJournalToExcel(companyId, journal, "Journal Caisse");
    }

    /**
     * Exporte le journal des opérations diverses (OD) en Excel
     */
    public byte[] exportGeneralJournalToExcel(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        com.predykt.accounting.dto.response.AuxiliaryJournalResponse journal =
            auxiliaryJournalsService.getGeneralJournal(companyId, startDate, endDate);
        return exportAuxiliaryJournalToExcel(companyId, journal, "Journal Opé. Div.");
    }

    /**
     * Exporte le journal à nouveaux (AN) en Excel
     */
    public byte[] exportOpeningJournalToExcel(Long companyId, Integer fiscalYear) throws IOException {
        com.predykt.accounting.dto.response.AuxiliaryJournalResponse journal =
            auxiliaryJournalsService.getOpeningJournal(companyId, fiscalYear);
        return exportAuxiliaryJournalToExcel(companyId, journal, "Journal À Nouveaux");
    }

    /**
     * Méthode générique pour exporter un journal auxiliaire en Excel
     */
    private byte[] exportAuxiliaryJournalToExcel(Long companyId,
                                                 com.predykt.accounting.dto.response.AuxiliaryJournalResponse journal,
                                                 String sheetName) throws IOException {

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            int rowNum = 0;

            // Titre
            Row titleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(journal.getJournalName());
            titleCell.setCellStyle(titleStyle);

            // Entreprise
            Row companyRow = sheet.createRow(rowNum++);
            companyRow.createCell(0).setCellValue(company.getName());

            // Période
            if (journal.getStartDate() != null && journal.getEndDate() != null) {
                Row periodRow = sheet.createRow(rowNum++);
                periodRow.createCell(0).setCellValue("Du " + journal.getStartDate().format(DATE_FORMATTER) +
                    " au " + journal.getEndDate().format(DATE_FORMATTER));
            }

            rowNum++; // Ligne vide

            // En-têtes de colonnes
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Date", "N° Pièce", "Compte", "Libellé", "Débit", "Crédit"};

            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Données
            for (com.predykt.accounting.dto.response.AuxiliaryJournalResponse.JournalEntry entry : journal.getEntries()) {
                Row dataRow = sheet.createRow(rowNum++);
                int colNum = 0;

                dataRow.createCell(colNum++).setCellValue(entry.getEntryDate().format(DATE_FORMATTER));
                dataRow.createCell(colNum++).setCellValue(entry.getPieceNumber() != null ? entry.getPieceNumber() : "");
                dataRow.createCell(colNum++).setCellValue(entry.getAccountNumber() != null ? entry.getAccountNumber() : "");
                dataRow.createCell(colNum++).setCellValue(entry.getDescription() != null ? entry.getDescription() : "");

                org.apache.poi.ss.usermodel.Cell debitCell = dataRow.createCell(colNum++);
                debitCell.setCellValue(entry.getDebitAmount() != null ? entry.getDebitAmount().doubleValue() : 0.0);
                debitCell.setCellStyle(currencyStyle);

                org.apache.poi.ss.usermodel.Cell creditCell = dataRow.createCell(colNum++);
                creditCell.setCellValue(entry.getCreditAmount() != null ? entry.getCreditAmount().doubleValue() : 0.0);
                creditCell.setCellStyle(currencyStyle);
            }

            // Ligne de totaux
            Row totalRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell totalLabelCell = totalRow.createCell(0);
            totalLabelCell.setCellValue("TOTAUX");
            totalLabelCell.setCellStyle(totalStyle);

            totalRow.createCell(1).setCellStyle(totalStyle);
            totalRow.createCell(2).setCellStyle(totalStyle);
            totalRow.createCell(3).setCellStyle(totalStyle);

            org.apache.poi.ss.usermodel.Cell totalDebitCell = totalRow.createCell(4);
            totalDebitCell.setCellValue(journal.getTotalDebit() != null ? journal.getTotalDebit().doubleValue() : 0.0);
            totalDebitCell.setCellStyle(totalStyle);

            org.apache.poi.ss.usermodel.Cell totalCreditCell = totalRow.createCell(5);
            totalCreditCell.setCellValue(journal.getTotalCredit() != null ? journal.getTotalCredit().doubleValue() : 0.0);
            totalCreditCell.setCellStyle(totalStyle);

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            log.info("Export Excel du journal {} terminé - {} écritures, {} octets",
                journal.getJournalCode(), journal.getNumberOfEntries(), baos.size());

            return baos.toByteArray();
        }
    }

    // ============================================================
    // EXPORTS NOTES ANNEXES
    // ============================================================

    /**
     * Exporte les Notes Annexes en PDF
     */
    public byte[] exportNotesAnnexesToPdf(Long companyId, Integer fiscalYear) throws IOException {
        log.info("Export des Notes Annexes en PDF pour l'entreprise {} - exercice {}", companyId, fiscalYear);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        NotesAnnexesResponse notes = notesAnnexesService.generateNotesAnnexes(companyId, fiscalYear);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // En-tête principal
        document.add(new Paragraph("NOTES ANNEXES AUX ÉTATS FINANCIERS")
            .setFontSize(18)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(notes.getCompanyName())
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Exercice clos le 31/12/" + fiscalYear)
            .setFontSize(12)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20));

        // NOTE 1: Principes et méthodes comptables
        addNoteSectionHeader(document, "NOTE 1", "PRINCIPES ET MÉTHODES COMPTABLES");
        document.add(new Paragraph("Référentiel comptable: " + notes.getNote1().getReferentielComptable())
            .setFontSize(10).setMarginLeft(20));
        document.add(new Paragraph("Méthodes d'évaluation: " + notes.getNote1().getMethodesEvaluation())
            .setFontSize(10).setMarginLeft(20));
        document.add(new Paragraph("Méthodes d'amortissement: " + notes.getNote1().getMethodesAmortissement())
            .setFontSize(10).setMarginLeft(20));
        document.add(new Paragraph("Méthodes de valorisation des stocks: " + notes.getNote1().getMethodesStocks())
            .setFontSize(10).setMarginLeft(20).setMarginBottom(10));

        // NOTE 2: Immobilisations
        addNoteSectionHeader(document, "NOTE 2", "IMMOBILISATIONS CORPORELLES ET INCORPORELLES");
        if (notes.getNote2() != null) {
            if (notes.getNote2().getImmobilisationsCorporelles() != null) {
                var corp = notes.getNote2().getImmobilisationsCorporelles();
                document.add(new Paragraph("Immobilisations corporelles - " + corp.getCategorie())
                    .setFontSize(10).setMarginLeft(20));
                document.add(new Paragraph("Valeur nette: " + formatAmount(corp.getValeurNetteFin()))
                    .setFontSize(9).setMarginLeft(30));
            }
            if (notes.getNote2().getCommentaire() != null) {
                document.add(new Paragraph(notes.getNote2().getCommentaire())
                    .setFontSize(9).setMarginLeft(20).setMarginBottom(10));
            }
        }

        // NOTE 3: Immobilisations financières
        addNoteSectionHeader(document, "NOTE 3", "IMMOBILISATIONS FINANCIÈRES");
        if (notes.getNote3() != null) {
            document.add(new Paragraph("Total immobilisations financières: " + formatAmount(notes.getNote3().getTotal()))
                .setFontSize(10).setMarginLeft(20).setMarginBottom(10));
        }

        // NOTE 4: Stocks
        addNoteSectionHeader(document, "NOTE 4", "STOCKS");
        if (notes.getNote4() != null) {
            document.add(new Paragraph("Méthode d'évaluation: " + notes.getNote4().getMethodeEvaluation())
                .setFontSize(10).setMarginLeft(20));
            document.add(new Paragraph("Total stocks (début): " + formatAmount(notes.getNote4().getTotalStocksDebut()))
                .setFontSize(10).setMarginLeft(20));
            document.add(new Paragraph("Total stocks (fin): " + formatAmount(notes.getNote4().getTotalStocksFin()))
                .setFontSize(10).setMarginLeft(20));
            document.add(new Paragraph("Variation: " + formatAmount(notes.getNote4().getVariationStocks()))
                .setFontSize(10).setMarginLeft(20).setMarginBottom(10));
        }

        // NOTE 5: Créances et dettes
        addNoteSectionHeader(document, "NOTE 5", "CRÉANCES ET DETTES");
        if (notes.getNote5() != null) {
            if (notes.getNote5().getCreances() != null) {
                document.add(new Paragraph("Créances clients: " + formatAmount(notes.getNote5().getCreances().getCreancesClients()))
                    .setFontSize(10).setMarginLeft(20));
                document.add(new Paragraph("Total créances: " + formatAmount(notes.getNote5().getCreances().getTotal()))
                    .setFontSize(10).setMarginLeft(20));
            }
            if (notes.getNote5().getDettes() != null) {
                document.add(new Paragraph("Dettes fournisseurs: " + formatAmount(notes.getNote5().getDettes().getDettesFournisseurs()))
                    .setFontSize(10).setMarginLeft(20));
                document.add(new Paragraph("Total dettes: " + formatAmount(notes.getNote5().getDettes().getTotal()))
                    .setFontSize(10).setMarginLeft(20));
            }
            document.add(new Paragraph("Provisions créances douteuses: " + formatAmount(notes.getNote5().getProvisionsCreancesDouteuses()))
                .setFontSize(10).setMarginLeft(20).setMarginBottom(10));
        }

        // NOTE 6: Capitaux propres
        addNoteSectionHeader(document, "NOTE 6", "CAPITAUX PROPRES");
        if (notes.getNote6() != null) {
            document.add(new Paragraph("Capital début exercice: " + formatAmount(notes.getNote6().getCapitalDebut()))
                .setFontSize(10).setMarginLeft(20));
            document.add(new Paragraph("Capital fin exercice: " + formatAmount(notes.getNote6().getCapitalFin()))
                .setFontSize(10).setMarginLeft(20));
            if (notes.getNote6().getTableauVariation() != null) {
                var variation = notes.getNote6().getTableauVariation();
                document.add(new Paragraph("Capital social: " + formatAmount(variation.getCapitalSocial()))
                    .setFontSize(9).setMarginLeft(30));
                document.add(new Paragraph("Résultat exercice: " + formatAmount(variation.getResultatExercice()))
                    .setFontSize(9).setMarginLeft(30));
            }
            document.add(new Paragraph("").setMarginBottom(10));
        }

        // NOTE 7: Emprunts et dettes financières
        addNoteSectionHeader(document, "NOTE 7", "EMPRUNTS ET DETTES FINANCIÈRES");
        if (notes.getNote7() != null) {
            document.add(new Paragraph("Total emprunts LT: " + formatAmount(notes.getNote7().getTotalEmpruntsLongTerme()))
                .setFontSize(10).setMarginLeft(20));
            document.add(new Paragraph("Total emprunts CT: " + formatAmount(notes.getNote7().getTotalEmpruntsCourtTerme()))
                .setFontSize(10).setMarginLeft(20).setMarginBottom(10));
        }

        // NOTE 8: Autres passifs
        addNoteSectionHeader(document, "NOTE 8", "AUTRES PASSIFS");
        if (notes.getNote8() != null) {
            document.add(new Paragraph("Provisions risques: " + formatAmount(notes.getNote8().getProvisionsRisques()))
                .setFontSize(10).setMarginLeft(20));
            document.add(new Paragraph("Provisions charges: " + formatAmount(notes.getNote8().getProvisionsCharges()))
                .setFontSize(10).setMarginLeft(20));
            document.add(new Paragraph("Produits constatés d'avance: " + formatAmount(notes.getNote8().getProduitsConstatesAvance()))
                .setFontSize(10).setMarginLeft(20));
            document.add(new Paragraph("Total: " + formatAmount(notes.getNote8().getTotal()))
                .setFontSize(10).setMarginLeft(20).setMarginBottom(10));
        }

        // NOTE 9: Produits et charges
        addNoteSectionHeader(document, "NOTE 9", "PRODUITS ET CHARGES");
        if (notes.getNote9() != null) {
            if (notes.getNote9().getProduits() != null) {
                document.add(new Paragraph("Total produits: " + formatAmount(notes.getNote9().getProduits().getTotal()))
                    .setFontSize(10).setMarginLeft(20));
            }
            if (notes.getNote9().getCharges() != null) {
                document.add(new Paragraph("Total charges: " + formatAmount(notes.getNote9().getCharges().getTotal()))
                    .setFontSize(10).setMarginLeft(20));
                document.add(new Paragraph("Charges personnel: " + formatAmount(notes.getNote9().getCharges().getChargesPersonnel()))
                    .setFontSize(9).setMarginLeft(30));
            }
            document.add(new Paragraph("").setMarginBottom(10));
        }

        // NOTE 10: Impôts et taxes
        addNoteSectionHeader(document, "NOTE 10", "IMPÔTS ET TAXES");
        if (notes.getNote10() != null) {
            if (notes.getNote10().getImposSurBenefices() != null) {
                document.add(new Paragraph("Impôt dû: " + formatAmount(notes.getNote10().getImposSurBenefices().getImpotDu()))
                    .setFontSize(10).setMarginLeft(20));
            }
            if (notes.getNote10().getTva() != null) {
                document.add(new Paragraph("TVA collectée: " + formatAmount(notes.getNote10().getTva().getTvaCollectee()))
                    .setFontSize(10).setMarginLeft(20));
                document.add(new Paragraph("TVA déductible: " + formatAmount(notes.getNote10().getTva().getTvaDeductible()))
                    .setFontSize(10).setMarginLeft(20));
            }
            document.add(new Paragraph("Total impôts et taxes: " + formatAmount(notes.getNote10().getTotalImpotsEtTaxes()))
                .setFontSize(10).setMarginLeft(20).setMarginBottom(10));
        }

        // NOTE 11: Engagements hors bilan
        addNoteSectionHeader(document, "NOTE 11", "ENGAGEMENTS HORS BILAN");
        if (notes.getNote11() != null) {
            String commentaire = notes.getNote11().getCommentaire() != null ?
                notes.getNote11().getCommentaire() : "Aucun engagement significatif hors bilan.";
            document.add(new Paragraph(commentaire)
                .setFontSize(10).setMarginLeft(20).setMarginBottom(10));
        }

        // NOTE 12: Événements postérieurs
        addNoteSectionHeader(document, "NOTE 12", "ÉVÉNEMENTS POSTÉRIEURS À LA CLÔTURE");
        if (notes.getNote12() != null) {
            String commentaire = notes.getNote12().getCommentaireGeneral() != null ?
                notes.getNote12().getCommentaireGeneral() : "Aucun événement significatif postérieur à la clôture.";
            document.add(new Paragraph(commentaire)
                .setFontSize(10).setMarginLeft(20).setMarginBottom(10));
        }

        document.close();

        log.info("Export PDF des Notes Annexes terminé - {} octets", baos.size());
        return baos.toByteArray();
    }

    /**
     * Exporte les Notes Annexes en Excel
     */
    public byte[] exportNotesAnnexesToExcel(Long companyId, Integer fiscalYear) throws IOException {
        log.info("Export des Notes Annexes en Excel pour l'entreprise {} - exercice {}", companyId, fiscalYear);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        NotesAnnexesResponse notes = notesAnnexesService.generateNotesAnnexes(companyId, fiscalYear);

        try (Workbook workbook = new XSSFWorkbook()) {
            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            // Feuille 1: Résumé
            Sheet summarySheet = workbook.createSheet("Résumé");
            int rowNum = 0;

            // En-tête
            Row titleRow = summarySheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("NOTES ANNEXES AUX ÉTATS FINANCIERS - Exercice " + fiscalYear);
            titleCell.setCellStyle(headerStyle);

            rowNum++;
            Row companyRow = summarySheet.createRow(rowNum++);
            companyRow.createCell(0).setCellValue("Entreprise:");
            companyRow.createCell(1).setCellValue(notes.getCompanyName());

            rowNum++;

            // Tableau des 12 notes
            Row headerRow = summarySheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("Note");
            headerRow.createCell(1).setCellValue("Titre");
            headerRow.getCell(0).setCellStyle(headerStyle);
            headerRow.getCell(1).setCellStyle(headerStyle);

            String[] noteTitles = {
                "Principes et méthodes comptables",
                "Immobilisations corporelles et incorporelles",
                "Immobilisations financières",
                "Stocks",
                "Créances et dettes",
                "Capitaux propres",
                "Emprunts et dettes financières",
                "Autres passifs",
                "Produits et charges",
                "Impôts et taxes",
                "Engagements hors bilan",
                "Événements postérieurs à la clôture"
            };

            for (int i = 0; i < noteTitles.length; i++) {
                Row noteRow = summarySheet.createRow(rowNum++);
                noteRow.createCell(0).setCellValue("Note " + (i + 1));
                noteRow.createCell(1).setCellValue(noteTitles[i]);
            }

            // Feuille 2: Détails (simplifiée pour Notes Annexes complexes)
            Sheet detailSheet = workbook.createSheet("Détails");
            int detailRow = 0;

            Row detailTitle = detailSheet.createRow(detailRow++);
            detailTitle.createCell(0).setCellValue("DÉTAILS SÉLECTIONNÉS DES NOTES ANNEXES");
            detailTitle.getCell(0).setCellStyle(headerStyle);

            detailRow++;

            // Note 4: Stocks
            if (notes.getNote4() != null) {
                Row stockLabel = detailSheet.createRow(detailRow++);
                stockLabel.createCell(0).setCellValue("STOCKS:");
                stockLabel.getCell(0).setCellStyle(headerStyle);

                Row stockRow = detailSheet.createRow(detailRow++);
                stockRow.createCell(0).setCellValue("Méthode évaluation:");
                stockRow.createCell(1).setCellValue(notes.getNote4().getMethodeEvaluation());

                Row stockTotalRow = detailSheet.createRow(detailRow++);
                stockTotalRow.createCell(0).setCellValue("Total stocks (fin):");
                Cell stockCell = stockTotalRow.createCell(1);
                stockCell.setCellValue(notes.getNote4().getTotalStocksFin() != null ?
                    notes.getNote4().getTotalStocksFin().doubleValue() : 0);
                stockCell.setCellStyle(currencyStyle);

                detailRow++;
            }

            // Note 10: Impôts
            if (notes.getNote10() != null) {
                Row impotLabel = detailSheet.createRow(detailRow++);
                impotLabel.createCell(0).setCellValue("IMPÔTS ET TAXES:");
                impotLabel.getCell(0).setCellStyle(headerStyle);

                Row impotRow = detailSheet.createRow(detailRow++);
                impotRow.createCell(0).setCellValue("Total impôts et taxes:");
                Cell impotCell = impotRow.createCell(1);
                impotCell.setCellValue(notes.getNote10().getTotalImpotsEtTaxes() != null ?
                    notes.getNote10().getTotalImpotsEtTaxes().doubleValue() : 0);
                impotCell.setCellStyle(currencyStyle);
            }

            detailSheet.autoSizeColumn(0);
            detailSheet.autoSizeColumn(1);

            // Auto-size colonnes feuille résumé
            for (int i = 0; i < 2; i++) {
                summarySheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            log.info("Export Excel des Notes Annexes terminé - {} octets", baos.size());
            return baos.toByteArray();
        }
    }

    // ============================================================
    // EXPORTS GRANDS LIVRES AUXILIAIRES
    // ============================================================

    /**
     * Exporte le Grand Livre Auxiliaire CLIENTS en PDF
     */
    public byte[] exportCustomersSubledgerToPdf(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export du GL Auxiliaire Clients en PDF pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        return exportSubledgerToPdf(companyId, startDate, endDate, "CLIENTS");
    }

    /**
     * Exporte le Grand Livre Auxiliaire FOURNISSEURS en PDF
     */
    public byte[] exportSuppliersSubledgerToPdf(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export du GL Auxiliaire Fournisseurs en PDF pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        return exportSubledgerToPdf(companyId, startDate, endDate, "FOURNISSEURS");
    }

    /**
     * Méthode générique pour exporter un grand livre auxiliaire en PDF
     */
    private byte[] exportSubledgerToPdf(Long companyId, LocalDate startDate, LocalDate endDate, String type) throws IOException {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        SubledgerResponse subledger = type.equals("CLIENTS") ?
            subledgerService.getCustomersSubledger(companyId, startDate, endDate) :
            subledgerService.getSuppliersSubledger(companyId, startDate, endDate);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // En-tête
        document.add(new Paragraph("GRAND LIVRE AUXILIAIRE " + type)
            .setFontSize(18)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(company.getName())
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Période: " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER))
            .setFontSize(12)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20));

        // Statistiques globales
        document.add(new Paragraph("STATISTIQUES GLOBALES")
            .setFontSize(12)
            .setBold()
            .setMarginBottom(5));

        document.add(new Paragraph("Nombre de tiers: " + (subledger.getNombreTiers() != null ? subledger.getNombreTiers() : 0))
            .setFontSize(10).setMarginLeft(20));
        document.add(new Paragraph("Solde total: " + formatAmount(subledger.getTotalSoldeCloture()))
            .setFontSize(10).setMarginLeft(20));
        document.add(new Paragraph("Nombre d'écritures: " + (subledger.getNombreEcritures() != null ? subledger.getNombreEcritures() : 0))
            .setFontSize(10).setMarginLeft(20).setMarginBottom(15));

        // Détail par tiers (limité aux 20 premiers pour le PDF)
        document.add(new Paragraph("DÉTAIL PAR TIERS (Top 20)")
            .setFontSize(12)
            .setBold()
            .setMarginBottom(5));

        if (subledger.getTiersDetails() != null && !subledger.getTiersDetails().isEmpty()) {
            int count = 0;
            for (SubledgerResponse.TiersDetail tiers : subledger.getTiersDetails()) {
                if (count++ >= 20) break;

                document.add(new Paragraph(tiers.getTiersName() + " (" + tiers.getAccountNumber() + ")")
                    .setFontSize(11)
                    .setBold()
                    .setMarginTop(10));

                document.add(new Paragraph("Solde: " + formatAmount(tiers.getSoldeCloture()) +
                    " | Écritures: " + tiers.getNombreEcritures())
                    .setFontSize(9)
                    .setMarginLeft(20)
                    .setMarginBottom(5));
            }
        }

        document.close();

        log.info("Export PDF du GL Auxiliaire {} terminé - {} octets", type, baos.size());
        return baos.toByteArray();
    }

    /**
     * Exporte le Grand Livre Auxiliaire CLIENTS en Excel
     */
    public byte[] exportCustomersSubledgerToExcel(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export du GL Auxiliaire Clients en Excel pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        return exportSubledgerToExcel(companyId, startDate, endDate, "CLIENTS");
    }

    /**
     * Exporte le Grand Livre Auxiliaire FOURNISSEURS en Excel
     */
    public byte[] exportSuppliersSubledgerToExcel(Long companyId, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Export du GL Auxiliaire Fournisseurs en Excel pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        return exportSubledgerToExcel(companyId, startDate, endDate, "FOURNISSEURS");
    }

    /**
     * Méthode générique pour exporter un grand livre auxiliaire en Excel
     */
    private byte[] exportSubledgerToExcel(Long companyId, LocalDate startDate, LocalDate endDate, String type) throws IOException {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        SubledgerResponse subledger = type.equals("CLIENTS") ?
            subledgerService.getCustomersSubledger(companyId, startDate, endDate) :
            subledgerService.getSuppliersSubledger(companyId, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            // Feuille 1: Résumé
            Sheet summarySheet = workbook.createSheet("Résumé");
            int rowNum = 0;

            // En-tête
            Row titleRow = summarySheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("GRAND LIVRE AUXILIAIRE " + type);
            titleCell.setCellStyle(headerStyle);

            rowNum++;
            Row companyRow = summarySheet.createRow(rowNum++);
            companyRow.createCell(0).setCellValue("Entreprise:");
            companyRow.createCell(1).setCellValue(company.getName());

            Row periodRow = summarySheet.createRow(rowNum++);
            periodRow.createCell(0).setCellValue("Période:");
            periodRow.createCell(1).setCellValue(startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER));

            rowNum++;

            // Statistiques
            Row statRow1 = summarySheet.createRow(rowNum++);
            statRow1.createCell(0).setCellValue("Nombre de tiers:");
            statRow1.createCell(1).setCellValue(subledger.getNombreTiers() != null ? subledger.getNombreTiers() : 0);

            Row statRow2 = summarySheet.createRow(rowNum++);
            statRow2.createCell(0).setCellValue("Solde total:");
            Cell soldeCell = statRow2.createCell(1);
            soldeCell.setCellValue(subledger.getTotalSoldeCloture() != null ?
                subledger.getTotalSoldeCloture().doubleValue() : 0);
            soldeCell.setCellStyle(currencyStyle);

            Row statRow3 = summarySheet.createRow(rowNum++);
            statRow3.createCell(0).setCellValue("Nombre d'écritures:");
            statRow3.createCell(1).setCellValue(subledger.getNombreEcritures() != null ? subledger.getNombreEcritures() : 0);

            rowNum += 2;

            // Tableau des tiers
            Row headerRow = summarySheet.createRow(rowNum++);
            String[] headers = {"Compte", "Nom", "Solde", "Nb écritures", "Catégorie risque"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            if (subledger.getTiersDetails() != null) {
                for (SubledgerResponse.TiersDetail tiers : subledger.getTiersDetails()) {
                    Row dataRow = summarySheet.createRow(rowNum++);
                    dataRow.createCell(0).setCellValue(tiers.getAccountNumber());
                    dataRow.createCell(1).setCellValue(tiers.getTiersName());

                    Cell soldeCellRow = dataRow.createCell(2);
                    soldeCellRow.setCellValue(tiers.getSoldeCloture() != null ? tiers.getSoldeCloture().doubleValue() : 0);
                    soldeCellRow.setCellStyle(currencyStyle);

                    dataRow.createCell(3).setCellValue(tiers.getNombreEcritures() != null ? tiers.getNombreEcritures() : 0);

                    if (tiers.getAnalyse() != null) {
                        dataRow.createCell(4).setCellValue(tiers.getAnalyse().getCategorieRisque() != null ?
                            tiers.getAnalyse().getCategorieRisque() : "");
                    }
                }
            }

            // Auto-size colonnes
            for (int i = 0; i < headers.length; i++) {
                summarySheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            log.info("Export Excel du GL Auxiliaire {} terminé - {} octets", type, baos.size());
            return baos.toByteArray();
        }
    }

    // ============================================================
    // MÉTHODES UTILITAIRES POUR NOTES ANNEXES
    // ============================================================

    /**
     * Ajoute un en-tête de section pour les notes annexes en PDF
     */
    private void addNoteSectionHeader(Document document, String noteNumber, String noteTitle) {
        document.add(new Paragraph(noteNumber + " - " + noteTitle)
            .setFontSize(12)
            .setBold()
            .setMarginTop(15)
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            .setPadding(5));
    }
}
