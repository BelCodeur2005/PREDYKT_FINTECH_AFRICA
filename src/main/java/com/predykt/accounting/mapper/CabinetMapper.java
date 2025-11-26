package com.predykt.accounting.mapper;

import com.predykt.accounting.domain.entity.Cabinet;
import com.predykt.accounting.dto.CabinetDTO;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper MapStruct pour Cabinet ↔ CabinetDTO
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CabinetMapper {

    /**
     * Convertit Cabinet → CabinetDTO
     */
    @Mapping(target = "currentCompaniesCount", expression = "java(cabinet.getCompanies() != null ? (long) cabinet.getCompanies().size() : 0L)")
    @Mapping(target = "currentUsersCount", expression = "java(cabinet.getUsers() != null ? (long) cabinet.getUsers().size() : 0L)")
    @Mapping(target = "hasReachedCompanyLimit", expression = "java(cabinet.hasReachedCompanyLimit())")
    @Mapping(target = "hasReachedUserLimit", expression = "java(cabinet.hasReachedUserLimit())")
    CabinetDTO toDTO(Cabinet cabinet);

    /**
     * Convertit CabinetDTO → Cabinet
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "companies", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "invoices", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Cabinet toEntity(CabinetDTO dto);

    /**
     * Convertit une liste de Cabinet → List<CabinetDTO>
     */
    List<CabinetDTO> toDTOList(List<Cabinet> cabinets);

    /**
     * Met à jour une entité Cabinet existante depuis un DTO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "companies", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "invoices", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDTO(CabinetDTO dto, @MappingTarget Cabinet cabinet);
}
