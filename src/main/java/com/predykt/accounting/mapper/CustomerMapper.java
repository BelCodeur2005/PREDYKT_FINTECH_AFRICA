package com.predykt.accounting.mapper;

import com.predykt.accounting.domain.entity.Customer;
import com.predykt.accounting.dto.request.CustomerCreateRequest;
import com.predykt.accounting.dto.request.CustomerUpdateRequest;
import com.predykt.accounting.dto.response.CustomerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper pour les clients
 */
@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "hasNiu", ignore = true)
    @Mapping(target = "auxiliaryAccount", ignore = true)
    Customer toEntity(CustomerCreateRequest request);

    @Mapping(target = "auxiliaryAccountNumber", expression = "java(customer.getAuxiliaryAccountNumber())")
    @Mapping(target = "hasValidNiu", expression = "java(customer.hasValidNiu())")
    @Mapping(target = "isExportCustomer", expression = "java(customer.isExportCustomer())")
    CustomerResponse toResponse(Customer customer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "hasNiu", ignore = true)
    @Mapping(target = "auxiliaryAccount", ignore = true)
    void updateEntityFromRequest(CustomerUpdateRequest request, @MappingTarget Customer customer);
}
