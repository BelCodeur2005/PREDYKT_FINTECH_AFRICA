package com.predykt.accounting.mapper;

import com.predykt.accounting.domain.entity.CabinetInvoice;
import com.predykt.accounting.dto.CabinetInvoiceDTO;
import org.mapstruct.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Mapper MapStruct pour CabinetInvoice ↔ CabinetInvoiceDTO
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CabinetInvoiceMapper {

    /**
     * Convertit CabinetInvoice → CabinetInvoiceDTO
     */
    @Mapping(source = "cabinet.id", target = "cabinetId")
    @Mapping(source = "cabinet.name", target = "cabinetName")
    @Mapping(target = "isOverdue", expression = "java(invoice.isOverdue())")
    @Mapping(target = "daysUntilDue", expression = "java(calculateDaysUntilDue(invoice))")
    @Mapping(target = "daysOverdue", expression = "java(calculateDaysOverdue(invoice))")
    CabinetInvoiceDTO toDTO(CabinetInvoice invoice);

    /**
     * Convertit CabinetInvoiceDTO → CabinetInvoice
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cabinet", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    CabinetInvoice toEntity(CabinetInvoiceDTO dto);

    /**
     * Convertit une liste
     */
    List<CabinetInvoiceDTO> toDTOList(List<CabinetInvoice> invoices);

    /**
     * Met à jour une entité existante depuis un DTO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cabinet", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "paymentMethod", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDTO(CabinetInvoiceDTO dto, @MappingTarget CabinetInvoice invoice);

    /**
     * Calcule le nombre de jours jusqu'à l'échéance
     */
    default Long calculateDaysUntilDue(CabinetInvoice invoice) {
        if (invoice == null || invoice.getDueDate() == null) return null;
        LocalDate today = LocalDate.now();
        if (today.isAfter(invoice.getDueDate())) return 0L;
        return ChronoUnit.DAYS.between(today, invoice.getDueDate());
    }

    /**
     * Calcule le nombre de jours de retard
     */
    default Long calculateDaysOverdue(CabinetInvoice invoice) {
        if (invoice == null || !invoice.isOverdue()) return 0L;
        return ChronoUnit.DAYS.between(invoice.getDueDate(), LocalDate.now());
    }
}
