package com.predykt.accounting.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.Customer;
import com.predykt.accounting.domain.entity.Deposit;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.DepositRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Service de génération de PDF pour les reçus d'acompte (Phase 2).
 *
 * Génère des reçus d'acompte professionnels conformes OHADA avec:
 * - En-tête entreprise avec logo
 * - Informations client
 * - Détails montants (HT, TVA 19.25%, TTC)
 * - Numéro de reçu (RA-YYYY-NNNNNN)
 * - Mentions légales OHADA
 *
 * Utilise iText 7 pour la génération PDF.
 *
 * @author PREDYKT Accounting Team
 * @version 2.0 (Phase 2)
 * @since 2025-12-11
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PDFGenerationService {

    private final DepositRepository depositRepository;
    private final CompanyRepository companyRepository;

    // Couleurs corporate PREDYKT
    private static final DeviceRgb COLOR_PRIMARY = new DeviceRgb(41, 98, 255); // Bleu
    private static final DeviceRgb COLOR_SECONDARY = new DeviceRgb(100, 116, 139); // Gris
    private static final DeviceRgb COLOR_SUCCESS = new DeviceRgb(34, 197, 94); // Vert
    private static final DeviceRgb COLOR_BACKGROUND = new DeviceRgb(248, 250, 252); // Gris clair

    // Formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(Locale.FRANCE);

    static {
        CURRENCY_FORMATTER.setCurrency(java.util.Currency.getInstance("XAF"));
        CURRENCY_FORMATTER.setMinimumFractionDigits(0);
        CURRENCY_FORMATTER.setMaximumFractionDigits(2);
    }

    /**
     * Génère un PDF de reçu d'acompte.
     *
     * @param companyId ID de l'entreprise
     * @param depositId ID de l'acompte
     * @return Bytes du PDF généré
     * @throws IOException En cas d'erreur de génération PDF
     */
    public byte[] generateDepositReceiptPdf(Long companyId, Long depositId) throws IOException {
        log.info("📄 Génération PDF reçu d'acompte {} pour entreprise {}", depositId, companyId);

        // Récupérer les données
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise", "id", companyId));

        Deposit deposit = depositRepository.findById(depositId)
            .orElseThrow(() -> new ResourceNotFoundException("Acompte", "id", depositId));

        // Vérifier que l'acompte appartient à l'entreprise
        if (!deposit.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Cet acompte n'appartient pas à l'entreprise spécifiée");
        }

        // Générer le PDF
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Fonts
            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Ajouter le contenu
            addHeader(document, company, fontBold, fontRegular);
            addTitle(document, deposit, fontBold);
            addCustomerInfo(document, deposit, fontRegular, fontBold);
            addAmountDetails(document, deposit, fontRegular, fontBold);
            addFooter(document, fontRegular);

            // Fermer le document
            document.close();

            log.info("✅ PDF généré avec succès: {} octets", baos.size());
            return baos.toByteArray();
        }
    }

    /**
     * Ajoute l'en-tête du document avec informations entreprise.
     */
    private void addHeader(Document document, Company company, PdfFont fontBold, PdfFont fontRegular) {
        // Table en-tête (2 colonnes: logo + infos)
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
            .useAllAvailableWidth()
            .setMarginBottom(30);

        // Colonne 1: Logo (ou nom de l'entreprise)
        Cell logoCell = new Cell()
            .add(new Paragraph(company.getName())
                .setFont(fontBold)
                .setFontSize(20)
                .setFontColor(COLOR_PRIMARY))
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.LEFT);

        headerTable.addCell(logoCell);

        // Colonne 2: Informations entreprise
        StringBuilder companyInfo = new StringBuilder();
        companyInfo.append(company.getName()).append("\n");

        if (company.getAddress() != null) {
            companyInfo.append(company.getAddress()).append("\n");
        }

        if (company.getPhone() != null) {
            companyInfo.append("Tél: ").append(company.getPhone()).append("\n");
        }

        if (company.getEmail() != null) {
            companyInfo.append("Email: ").append(company.getEmail()).append("\n");
        }

        Cell infoCell = new Cell()
            .add(new Paragraph(companyInfo.toString())
                .setFont(fontRegular)
                .setFontSize(10)
                .setFontColor(COLOR_SECONDARY))
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.RIGHT);

        headerTable.addCell(infoCell);
        document.add(headerTable);

        // Ligne de séparation
        document.add(new Paragraph()
            .setBorderBottom(new SolidBorder(COLOR_PRIMARY, 2))
            .setMarginBottom(20));
    }

    /**
     * Ajoute le titre du document.
     */
    private void addTitle(Document document, Deposit deposit, PdfFont fontBold) {
        // Titre principal
        document.add(new Paragraph("REÇU D'ACOMPTE")
            .setFont(fontBold)
            .setFontSize(24)
            .setFontColor(COLOR_PRIMARY)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(10));

        // Numéro de reçu
        document.add(new Paragraph(deposit.getDepositNumber())
            .setFont(fontBold)
            .setFontSize(14)
            .setFontColor(COLOR_SECONDARY)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(30));
    }

    /**
     * Ajoute les informations client et date.
     */
    private void addCustomerInfo(Document document, Deposit deposit, PdfFont fontRegular, PdfFont fontBold) {
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
            .useAllAvailableWidth()
            .setMarginBottom(30);

        // Colonne gauche: Client
        Cell clientCell = new Cell()
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(COLOR_BACKGROUND)
            .setPadding(15);

        clientCell.add(new Paragraph("CLIENT")
            .setFont(fontBold)
            .setFontSize(12)
            .setFontColor(COLOR_PRIMARY)
            .setMarginBottom(10));

        Customer customer = deposit.getCustomer();
        if (customer != null) {
            clientCell.add(new Paragraph(customer.getName())
                .setFont(fontBold)
                .setFontSize(14)
                .setMarginBottom(5));

            if (customer.getAddress() != null) {
                clientCell.add(new Paragraph(customer.getAddress())
                    .setFont(fontRegular)
                    .setFontSize(10)
                    .setFontColor(COLOR_SECONDARY));
            }
        } else {
            clientCell.add(new Paragraph("Client non spécifié")
                .setFont(fontRegular)
                .setFontSize(12)
                .setFontColor(COLOR_SECONDARY));
        }

        infoTable.addCell(clientCell);

        // Colonne droite: Date
        Cell dateCell = new Cell()
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(COLOR_BACKGROUND)
            .setPadding(15);

        dateCell.add(new Paragraph("DATE DE RÉCEPTION")
            .setFont(fontBold)
            .setFontSize(12)
            .setFontColor(COLOR_PRIMARY)
            .setMarginBottom(10));

        dateCell.add(new Paragraph(deposit.getDepositDate().format(DATE_FORMATTER))
            .setFont(fontBold)
            .setFontSize(14));

        infoTable.addCell(dateCell);

        document.add(infoTable);
    }

    /**
     * Ajoute les détails des montants.
     */
    private void addAmountDetails(Document document, Deposit deposit, PdfFont fontRegular, PdfFont fontBold) {
        // Table des montants
        Table amountTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
            .useAllAvailableWidth()
            .setMarginBottom(30);

        // En-tête de table
        amountTable.addCell(createHeaderCell("DÉSIGNATION", fontBold));
        amountTable.addCell(createHeaderCell("MONTANT", fontBold));

        // Ligne 1: Montant HT
        amountTable.addCell(createCell("Montant Hors Taxes", fontRegular));
        amountTable.addCell(createAmountCell(deposit.getAmountHt(), fontRegular));

        // Ligne 2: TVA
        amountTable.addCell(createCell(
            String.format("TVA (%.2f%%)", deposit.getVatRate()),
            fontRegular));
        amountTable.addCell(createAmountCell(deposit.getVatAmount(), fontRegular));

        // Ligne séparation
        amountTable.addCell(new Cell()
            .setBorder(Border.NO_BORDER)
            .setBorderTop(new SolidBorder(COLOR_SECONDARY, 1))
            .setHeight(10));
        amountTable.addCell(new Cell()
            .setBorder(Border.NO_BORDER)
            .setBorderTop(new SolidBorder(COLOR_SECONDARY, 1))
            .setHeight(10));

        // Ligne 3: Total TTC (mise en évidence)
        Cell totalLabel = new Cell()
            .add(new Paragraph("TOTAL TTC")
                .setFont(fontBold)
                .setFontSize(14)
                .setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(COLOR_SUCCESS)
            .setPadding(10)
            .setBorder(Border.NO_BORDER);
        amountTable.addCell(totalLabel);

        Cell totalAmount = new Cell()
            .add(new Paragraph(formatCurrency(deposit.getAmountTtc()))
                .setFont(fontBold)
                .setFontSize(14)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT))
            .setBackgroundColor(COLOR_SUCCESS)
            .setPadding(10)
            .setBorder(Border.NO_BORDER);
        amountTable.addCell(totalAmount);

        document.add(amountTable);

        // Description si présente
        if (deposit.getDescription() != null && !deposit.getDescription().isBlank()) {
            document.add(new Paragraph("DESCRIPTION")
                .setFont(fontBold)
                .setFontSize(12)
                .setFontColor(COLOR_PRIMARY)
                .setMarginTop(20)
                .setMarginBottom(10));

            document.add(new Paragraph(deposit.getDescription())
                .setFont(fontRegular)
                .setFontSize(10)
                .setBackgroundColor(COLOR_BACKGROUND)
                .setPadding(15)
                .setMarginBottom(30));
        }
    }

    /**
     * Ajoute le pied de page avec mentions légales.
     */
    private void addFooter(Document document, PdfFont fontRegular) {
        // Ligne de séparation
        document.add(new Paragraph()
            .setBorderTop(new SolidBorder(COLOR_SECONDARY, 1))
            .setMarginTop(30)
            .setMarginBottom(20));

        // Mentions légales
        String legalMentions = "Reçu d'acompte conforme OHADA SYSCOHADA (Articles 276-279)\n" +
            "TVA exigible sur encaissement (CGI Cameroun Article 128)\n" +
            "Document généré automatiquement par PREDYKT Accounting System";

        document.add(new Paragraph(legalMentions)
            .setFont(fontRegular)
            .setFontSize(8)
            .setFontColor(COLOR_SECONDARY)
            .setTextAlignment(TextAlignment.CENTER)
            .setItalic());

        // Date de génération
        document.add(new Paragraph(
            String.format("Généré le %s", LocalDate.now().format(DATE_FORMATTER)))
            .setFont(fontRegular)
            .setFontSize(8)
            .setFontColor(COLOR_SECONDARY)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(10));
    }

    // =====================================================================
    // Méthodes utilitaires
    // =====================================================================

    private Cell createHeaderCell(String text, PdfFont font) {
        return new Cell()
            .add(new Paragraph(text).setFont(font).setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(COLOR_PRIMARY)
            .setPadding(10)
            .setBorder(Border.NO_BORDER);
    }

    private Cell createCell(String text, PdfFont font) {
        return new Cell()
            .add(new Paragraph(text).setFont(font))
            .setPadding(10)
            .setBorder(Border.NO_BORDER)
            .setBorderBottom(new SolidBorder(COLOR_BACKGROUND, 2));
    }

    private Cell createAmountCell(BigDecimal amount, PdfFont font) {
        return new Cell()
            .add(new Paragraph(formatCurrency(amount))
                .setFont(font)
                .setTextAlignment(TextAlignment.RIGHT))
            .setPadding(10)
            .setBorder(Border.NO_BORDER)
            .setBorderBottom(new SolidBorder(COLOR_BACKGROUND, 2));
    }

    private String formatCurrency(BigDecimal amount) {
        return CURRENCY_FORMATTER.format(amount) + " XAF";
    }
}
