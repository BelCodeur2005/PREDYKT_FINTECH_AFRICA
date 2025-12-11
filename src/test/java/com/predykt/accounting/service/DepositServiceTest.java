package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.*;
import com.predykt.accounting.domain.enums.InvoiceStatus;
import com.predykt.accounting.dto.request.DepositApplyRequest;
import com.predykt.accounting.dto.request.DepositCreateRequest;
import com.predykt.accounting.dto.request.DepositUpdateRequest;
import com.predykt.accounting.dto.response.DepositResponse;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.exception.ValidationException;
import com.predykt.accounting.mapper.DepositMapper;
import com.predykt.accounting.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour DepositService
 *
 * Couvre:
 * - Création d'acompte avec génération automatique de numéro
 * - Imputation sur facture avec validation OHADA
 * - Annulation d'imputation
 * - Calcul automatique TVA (19.25%)
 * - Génération d'écritures comptables
 * - Gestion des erreurs (client invalide, acompte déjà imputé, etc.)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DepositService - Tests unitaires")
class DepositServiceTest {

    @Mock
    private DepositRepository depositRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private GeneralLedgerService generalLedgerService;

    @Mock
    private DepositMapper depositMapper;

    @InjectMocks
    private DepositService depositService;

    private Company testCompany;
    private Customer testCustomer;
    private Invoice testInvoice;
    private Deposit testDeposit;
    private DepositCreateRequest createRequest;
    private DepositResponse depositResponse;

    @BeforeEach
    void setUp() {
        // Initialisation société
        testCompany = Company.builder()
            .id(1L)
            .name("Test Company SARL")
            .build();

        // Initialisation client
        testCustomer = Customer.builder()
            .id(1L)
            .company(testCompany)
            .name("Client Test")
            .niuNumber("P123456789M")
            .hasNiu(true)
            .build();

        // Initialisation facture
        testInvoice = Invoice.builder()
            .id(1L)
            .company(testCompany)
            .customer(testCustomer)
            .invoiceNumber("FV-2025-0001")
            .totalHt(new BigDecimal("1000000"))
            .vatAmount(new BigDecimal("192500"))
            .totalTtc(new BigDecimal("1192500"))
            .amountPaid(BigDecimal.ZERO)
            .amountDue(new BigDecimal("1192500"))
            .status(InvoiceStatus.SENT)
            .build();

        // Initialisation acompte
        testDeposit = Deposit.builder()
            .id(1L)
            .company(testCompany)
            .customer(testCustomer)
            .depositNumber("RA-2025-000001")
            .depositDate(LocalDate.of(2025, 1, 15))
            .amountHt(new BigDecimal("100000"))
            .vatRate(new BigDecimal("19.25"))
            .vatAmount(new BigDecimal("19250"))
            .amountTtc(new BigDecimal("119250"))
            .isApplied(false)
            .build();

        // Initialisation requête création
        createRequest = DepositCreateRequest.builder()
            .depositDate(LocalDate.of(2025, 1, 15))
            .amountHt(new BigDecimal("100000"))
            .vatRate(new BigDecimal("19.25"))
            .customerId(1L)
            .description("Acompte test")
            .build();

        // Initialisation réponse
        depositResponse = DepositResponse.builder()
            .id(1L)
            .depositNumber("RA-2025-000001")
            .amountHt(new BigDecimal("100000"))
            .vatRate(new BigDecimal("19.25"))
            .vatAmount(new BigDecimal("19250"))
            .amountTtc(new BigDecimal("119250"))
            .isApplied(false)
            .canBeApplied(true)
            .build();
    }

    // ==================== TESTS CRÉATION ====================

    @Test
    @DisplayName("Créer un acompte avec succès - Calcul automatique TVA")
    void testCreateDeposit_Success() {
        // Given
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(depositMapper.toEntity(createRequest)).thenReturn(testDeposit);
        when(depositRepository.findByCompanyAndDepositDateBetweenOrderByDepositDateDesc(
            eq(testCompany), any(), any()
        )).thenReturn(new ArrayList<>());
        when(depositRepository.existsByDepositNumberAndCompany(anyString(), eq(testCompany)))
            .thenReturn(false);
        when(depositRepository.save(any(Deposit.class))).thenReturn(testDeposit);
        when(depositMapper.toResponse(testDeposit)).thenReturn(depositResponse);

        // When
        DepositResponse result = depositService.createDeposit(1L, createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDepositNumber()).startsWith("RA-2025-");
        assertThat(result.getAmountTtc()).isEqualByComparingTo(new BigDecimal("119250"));
        assertThat(result.getVatAmount()).isEqualByComparingTo(new BigDecimal("19250"));

        verify(depositRepository).save(any(Deposit.class));
        verify(generalLedgerService).recordJournalEntry(eq(1L), any());
    }

    @Test
    @DisplayName("Créer un acompte - Société non trouvée")
    void testCreateDeposit_CompanyNotFound() {
        // Given
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> depositService.createDeposit(99L, createRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Société non trouvée");

        verify(depositRepository, never()).save(any());
    }

    @Test
    @DisplayName("Créer un acompte - Client non trouvé")
    void testCreateDeposit_CustomerNotFound() {
        // Given
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> depositService.createDeposit(1L, createRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Client non trouvé");

        verify(depositRepository, never()).save(any());
    }

    @Test
    @DisplayName("Créer un acompte - Client n'appartient pas à la société")
    void testCreateDeposit_CustomerNotBelongsToCompany() {
        // Given
        Company otherCompany = Company.builder().id(2L).name("Other Company").build();
        testCustomer.setCompany(otherCompany);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        // When/Then
        assertThatThrownBy(() -> depositService.createDeposit(1L, createRequest))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("n'appartient pas à cette société");

        verify(depositRepository, never()).save(any());
    }

    // ==================== TESTS IMPUTATION ====================

    @Test
    @DisplayName("Imputer un acompte sur une facture avec succès")
    void testApplyDepositToInvoice_Success() {
        // Given
        DepositApplyRequest applyRequest = DepositApplyRequest.builder()
            .invoiceId(1L)
            .build();

        when(depositRepository.findById(1L)).thenReturn(Optional.of(testDeposit));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(depositRepository.save(any(Deposit.class))).thenReturn(testDeposit);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);
        when(depositMapper.toResponse(testDeposit)).thenReturn(depositResponse);

        // When
        DepositResponse result = depositService.applyDepositToInvoice(1L, 1L, applyRequest);

        // Then
        assertThat(result).isNotNull();

        // Vérifier que l'acompte a été marqué comme imputé
        ArgumentCaptor<Deposit> depositCaptor = ArgumentCaptor.forClass(Deposit.class);
        verify(depositRepository).save(depositCaptor.capture());
        Deposit savedDeposit = depositCaptor.getValue();
        assertThat(savedDeposit.getIsApplied()).isTrue();
        assertThat(savedDeposit.getInvoice()).isEqualTo(testInvoice);

        // Vérifier que la facture a été mise à jour
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice savedInvoice = invoiceCaptor.getValue();
        assertThat(savedInvoice.getAmountPaid()).isEqualByComparingTo(new BigDecimal("119250"));

        // Vérifier que l'écriture comptable a été générée
        verify(generalLedgerService).recordJournalEntry(eq(1L), any());
    }

    @Test
    @DisplayName("Imputer un acompte - Acompte non trouvé")
    void testApplyDepositToInvoice_DepositNotFound() {
        // Given
        DepositApplyRequest applyRequest = DepositApplyRequest.builder()
            .invoiceId(1L)
            .build();

        when(depositRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> depositService.applyDepositToInvoice(1L, 99L, applyRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Acompte non trouvé");
    }

    @Test
    @DisplayName("Imputer un acompte - Facture non trouvée")
    void testApplyDepositToInvoice_InvoiceNotFound() {
        // Given
        DepositApplyRequest applyRequest = DepositApplyRequest.builder()
            .invoiceId(99L)
            .build();

        when(depositRepository.findById(1L)).thenReturn(Optional.of(testDeposit));
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> depositService.applyDepositToInvoice(1L, 1L, applyRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Facture non trouvée");
    }

    @Test
    @DisplayName("Imputer un acompte - Client différent entre acompte et facture")
    void testApplyDepositToInvoice_CustomerMismatch() {
        // Given
        Customer otherCustomer = Customer.builder()
            .id(2L)
            .company(testCompany)
            .name("Other Customer")
            .build();
        testInvoice.setCustomer(otherCustomer);

        DepositApplyRequest applyRequest = DepositApplyRequest.builder()
            .invoiceId(1L)
            .build();

        when(depositRepository.findById(1L)).thenReturn(Optional.of(testDeposit));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));

        // When/Then
        assertThatThrownBy(() -> depositService.applyDepositToInvoice(1L, 1L, applyRequest))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("client");
    }

    @Test
    @DisplayName("Imputer un acompte - Acompte déjà imputé")
    void testApplyDepositToInvoice_AlreadyApplied() {
        // Given
        testDeposit.setIsApplied(true);
        testDeposit.setInvoice(testInvoice);

        DepositApplyRequest applyRequest = DepositApplyRequest.builder()
            .invoiceId(1L)
            .build();

        when(depositRepository.findById(1L)).thenReturn(Optional.of(testDeposit));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));

        // When/Then
        assertThatThrownBy(() -> depositService.applyDepositToInvoice(1L, 1L, applyRequest))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("déjà imputé");
    }

    @Test
    @DisplayName("Imputer un acompte - Montant acompte > Montant facture")
    void testApplyDepositToInvoice_AmountExceedsInvoice() {
        // Given
        testDeposit.setAmountTtc(new BigDecimal("2000000")); // Plus que la facture

        DepositApplyRequest applyRequest = DepositApplyRequest.builder()
            .invoiceId(1L)
            .build();

        when(depositRepository.findById(1L)).thenReturn(Optional.of(testDeposit));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));

        // When/Then
        assertThatThrownBy(() -> depositService.applyDepositToInvoice(1L, 1L, applyRequest))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dépasse le montant de la facture");
    }

    // ==================== TESTS ANNULATION IMPUTATION ====================

    @Test
    @DisplayName("Annuler imputation d'un acompte avec succès")
    void testUnapplyDeposit_Success() {
        // Given
        testDeposit.setIsApplied(true);
        testDeposit.setInvoice(testInvoice);
        testInvoice.setAmountPaid(new BigDecimal("119250"));
        testInvoice.setAmountDue(new BigDecimal("1073250"));

        when(depositRepository.findById(1L)).thenReturn(Optional.of(testDeposit));
        when(depositRepository.save(any(Deposit.class))).thenReturn(testDeposit);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);
        when(depositMapper.toResponse(testDeposit)).thenReturn(depositResponse);

        // When
        DepositResponse result = depositService.unapplyDeposit(1L, 1L);

        // Then
        assertThat(result).isNotNull();

        // Vérifier que l'acompte a été désimputé
        ArgumentCaptor<Deposit> depositCaptor = ArgumentCaptor.forClass(Deposit.class);
        verify(depositRepository).save(depositCaptor.capture());
        Deposit savedDeposit = depositCaptor.getValue();
        assertThat(savedDeposit.getIsApplied()).isFalse();
        assertThat(savedDeposit.getInvoice()).isNull();

        // Vérifier que la facture a été mise à jour
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice savedInvoice = invoiceCaptor.getValue();
        assertThat(savedInvoice.getAmountPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedInvoice.getAmountDue()).isEqualByComparingTo(new BigDecimal("1192500"));
    }

    @Test
    @DisplayName("Annuler imputation - Acompte non imputé")
    void testUnapplyDeposit_NotApplied() {
        // Given
        testDeposit.setIsApplied(false);

        when(depositRepository.findById(1L)).thenReturn(Optional.of(testDeposit));

        // When/Then
        assertThatThrownBy(() -> depositService.unapplyDeposit(1L, 1L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("n'est pas imputé");
    }

    // ==================== TESTS MODIFICATION ====================

    @Test
    @DisplayName("Modifier un acompte avec succès")
    void testUpdateDeposit_Success() {
        // Given
        DepositUpdateRequest updateRequest = DepositUpdateRequest.builder()
            .depositDate(LocalDate.of(2025, 1, 20))
            .description("Description mise à jour")
            .build();

        when(depositRepository.findById(1L)).thenReturn(Optional.of(testDeposit));
        when(depositRepository.save(any(Deposit.class))).thenReturn(testDeposit);
        when(depositMapper.toResponse(testDeposit)).thenReturn(depositResponse);

        // When
        DepositResponse result = depositService.updateDeposit(1L, 1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(depositMapper).updateEntityFromRequest(updateRequest, testDeposit);
        verify(depositRepository).save(testDeposit);
    }

    @Test
    @DisplayName("Modifier un acompte - Acompte déjà imputé")
    void testUpdateDeposit_AlreadyApplied() {
        // Given
        testDeposit.setIsApplied(true);
        testDeposit.setInvoice(testInvoice);

        DepositUpdateRequest updateRequest = DepositUpdateRequest.builder()
            .description("Nouvelle description")
            .build();

        when(depositRepository.findById(1L)).thenReturn(Optional.of(testDeposit));

        // When/Then
        assertThatThrownBy(() -> depositService.updateDeposit(1L, 1L, updateRequest))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("déjà imputé");

        verify(depositRepository, never()).save(any());
    }

    // ==================== TESTS STATISTIQUES ====================

    @Test
    @DisplayName("Calculer total acomptes disponibles pour société")
    void testGetTotalAvailableDeposits_Success() {
        // Given
        BigDecimal expectedTotal = new BigDecimal("500000");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
        when(depositRepository.sumAvailableDepositsByCompany(testCompany))
            .thenReturn(expectedTotal);

        // When
        BigDecimal result = depositService.getTotalAvailableDeposits(1L);

        // Then
        assertThat(result).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("Calculer total acomptes disponibles pour client")
    void testGetTotalAvailableDepositsForCustomer_Success() {
        // Given
        BigDecimal expectedTotal = new BigDecimal("250000");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(depositRepository.sumAvailableDepositsByCustomer(testCompany, testCustomer))
            .thenReturn(expectedTotal);

        // When
        BigDecimal result = depositService.getTotalAvailableDepositsForCustomer(1L, 1L);

        // Then
        assertThat(result).isEqualByComparingTo(expectedTotal);
    }
}
