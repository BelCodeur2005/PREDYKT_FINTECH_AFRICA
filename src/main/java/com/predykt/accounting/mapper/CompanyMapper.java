package com.predykt.accounting.mapper;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.dto.request.CompanyCreateRequest;
import com.predykt.accounting.dto.response.CompanyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CompanyMapper {
    
    Company toEntity(CompanyCreateRequest request);
    
    CompanyResponse toResponse(Company company);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(CompanyCreateRequest request, @MappingTarget Company company);
}