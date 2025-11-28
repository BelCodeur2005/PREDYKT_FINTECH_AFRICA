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
}
