package com.predykt.accounting.mapper;

import com.predykt.accounting.domain.entity.TenantMetadata;
import com.predykt.accounting.dto.TenantMetadataDTO;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper MapStruct pour TenantMetadata ↔ TenantMetadataDTO
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMetadataMapper {

    /**
     * Convertit TenantMetadata → TenantMetadataDTO
     */
    @Mapping(source = "company.id", target = "companyId")
    @Mapping(source = "company.name", target = "companyName")
    @Mapping(source = "cabinet.id", target = "cabinetId")
    @Mapping(source = "cabinet.name", target = "cabinetName")
    @Mapping(target = "isSharedMode", expression = "java(metadata.isSharedMode())")
    @Mapping(target = "isDedicatedMode", expression = "java(metadata.isDedicatedMode())")
    @Mapping(target = "isCabinetMode", expression = "java(metadata.isCabinetMode())")
    TenantMetadataDTO toDTO(TenantMetadata metadata);

    /**
     * Convertit TenantMetadataDTO → TenantMetadata
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "cabinet", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TenantMetadata toEntity(TenantMetadataDTO dto);

    /**
     * Convertit une liste
     */
    List<TenantMetadataDTO> toDTOList(List<TenantMetadata> metadataList);

    /**
     * Met à jour une entité existante depuis un DTO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "tenantMode", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "cabinet", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDTO(TenantMetadataDTO dto, @MappingTarget TenantMetadata metadata);
}
