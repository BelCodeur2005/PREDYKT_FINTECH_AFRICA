package com.predykt.accounting.mapper;

import com.predykt.accounting.domain.entity.Deposit;
import com.predykt.accounting.dto.request.DepositCreateRequest;
import com.predykt.accounting.dto.request.DepositUpdateRequest;
import com.predykt.accounting.dto.response.DepositResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper MapStruct pour les acomptes (Deposits).
 *
 * Conversions automatiques:
 * - DepositCreateRequest → Deposit (entity)
 * - Deposit (entity) → DepositResponse
 * - DepositUpdateRequest + Deposit → Deposit (mise à jour)
 *
 * MapStruct génère automatiquement l'implémentation à la compilation.
 * L'ordre de traitement Lombok → MapStruct est crucial (voir pom.xml).
 *
 * @author PREDYKT System Optimizer
 * @since Phase 3 - Conformité OHADA Avancée
 */
@Mapper(componentModel = "spring")
public interface DepositMapper {

    /**
     * Convertit une requête de création en entité Deposit.
     *
     * Champs ignorés (gérés par le service):
     * - id: auto-généré
     * - depositNumber: généré par DepositService
     * - company: injecté par DepositService
     * - customer: résolu par DepositService via customerId
     * - invoice: NULL à la création
     * - payment: résolu par DepositService via paymentId
     * - vatAmount, amountTtc: calculés par @PrePersist
     * - isApplied: toujours FALSE à la création
     * - appliedAt, appliedBy: NULL à la création
     * - depositReceiptUrl: généré après création
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "depositNumber", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "payment", ignore = true)
    @Mapping(target = "vatAmount", ignore = true)
    @Mapping(target = "amountTtc", ignore = true)
    @Mapping(target = "isApplied", ignore = true)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "appliedBy", ignore = true)
    @Mapping(target = "depositReceiptUrl", ignore = true)
    Deposit toEntity(DepositCreateRequest request);

    /**
     * Convertit une entité Deposit en DTO de réponse.
     *
     * Mappings explicites pour les relations:
     * - company.id → companyId
     * - company.name → companyName
     * - customer.id → customerId
     * - customer.name → customerName
     * - invoice.id → invoiceId
     * - invoice.invoiceNumber → invoiceNumber
     * - payment.id → paymentId
     * - payment.paymentNumber → paymentNumber
     *
     * Mappings calculés (méthodes de l'entité):
     * - availableAmount: deposit.getAvailableAmount()
     * - canBeApplied: deposit.canBeApplied()
     */
    @Mapping(target = "companyId", source = "company.id")
    @Mapping(target = "companyName", source = "company.name")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "invoiceNumber", source = "invoice.invoiceNumber")
    @Mapping(target = "paymentId", source = "payment.id")
    @Mapping(target = "paymentNumber", source = "payment.paymentNumber")
    @Mapping(target = "availableAmount", expression = "java(deposit.getAvailableAmount())")
    @Mapping(target = "canBeApplied", expression = "java(deposit.canBeApplied())")
    DepositResponse toResponse(Deposit deposit);

    /**
     * Met à jour une entité Deposit existante avec les données d'une requête de modification.
     *
     * Champs modifiables:
     * - depositDate
     * - description
     * - customerOrderReference
     * - notes
     *
     * Champs ignorés (non modifiables après création):
     * - id, depositNumber: immuables
     * - company: immuable
     * - customer: résolu par DepositService via customerId
     * - invoice, payment: gérés par d'autres méthodes
     * - amountHt, vatRate, vatAmount, amountTtc: immuables (intégrité comptable)
     * - isApplied, appliedAt, appliedBy: gérés par applyToInvoice()
     * - depositReceiptUrl: généré automatiquement
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "depositNumber", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "payment", ignore = true)
    @Mapping(target = "amountHt", ignore = true)
    @Mapping(target = "vatRate", ignore = true)
    @Mapping(target = "vatAmount", ignore = true)
    @Mapping(target = "amountTtc", ignore = true)
    @Mapping(target = "isApplied", ignore = true)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "appliedBy", ignore = true)
    @Mapping(target = "depositReceiptUrl", ignore = true)
    void updateEntityFromRequest(DepositUpdateRequest request, @MappingTarget Deposit deposit);
}
