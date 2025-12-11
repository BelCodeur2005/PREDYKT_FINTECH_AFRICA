package com.predykt.accounting.mapper;

import com.predykt.accounting.domain.entity.DepositApplication;
import com.predykt.accounting.dto.response.DepositApplicationResponse;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper MapStruct pour les entités DepositApplication.
 *
 * Convertit entre:
 * - DepositApplication (entité JPA) ↔ DepositApplicationResponse (DTO API)
 *
 * @author PREDYKT Accounting Team
 * @version 2.0 (Phase 2 - Imputation Partielle)
 * @since 2025-12-11
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface DepositApplicationMapper {

    /**
     * Convertit une entité DepositApplication en DTO Response.
     *
     * @param application L'entité source
     * @return Le DTO de réponse
     */
    @Mapping(target = "depositId", source = "deposit.id")
    @Mapping(target = "depositNumber", source = "deposit.depositNumber")
    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "invoiceNumber", source = "invoice.invoiceNumber")
    @Mapping(target = "companyId", source = "company.id")
    @Mapping(target = "percentageOfDeposit", expression = "java(application.getPercentageOfDeposit())")
    @Mapping(target = "percentageOfInvoice", expression = "java(application.getPercentageOfInvoice())")
    @Mapping(target = "description", expression = "java(application.getDescription())")
    DepositApplicationResponse toResponse(DepositApplication application);

    /**
     * Convertit une liste d'entités en liste de DTOs Response.
     *
     * @param applications Liste d'entités
     * @return Liste de DTOs
     */
    List<DepositApplicationResponse> toResponseList(List<DepositApplication> applications);
}
