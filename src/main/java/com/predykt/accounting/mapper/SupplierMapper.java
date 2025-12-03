package com.predykt.accounting.mapper;

import com.predykt.accounting.domain.entity.Supplier;
import com.predykt.accounting.dto.request.SupplierCreateRequest;
import com.predykt.accounting.dto.request.SupplierUpdateRequest;
import com.predykt.accounting.dto.response.SupplierResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper pour les fournisseurs
 */
@Mapper(componentModel = "spring")
public interface SupplierMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "hasNiu", ignore = true)
    Supplier toEntity(SupplierCreateRequest request);

    @Mapping(target = "applicableAirRate", expression = "java(supplier.getApplicableAirRate())")
    @Mapping(target = "requiresAlert", expression = "java(!supplier.hasValidNiu())")
    @Mapping(target = "alertMessage", expression = "java(supplier.hasValidNiu() ? null : \"⚠️ NIU manquant - Taux AIR majoré à 5,5%\")")
    SupplierResponse toResponse(Supplier supplier);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "hasNiu", ignore = true)
    void updateEntityFromRequest(SupplierUpdateRequest request, @MappingTarget Supplier supplier);
}
