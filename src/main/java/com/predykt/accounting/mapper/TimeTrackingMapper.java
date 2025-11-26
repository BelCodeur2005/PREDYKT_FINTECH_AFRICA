package com.predykt.accounting.mapper;

import com.predykt.accounting.domain.entity.TimeTracking;
import com.predykt.accounting.domain.entity.User;
import com.predykt.accounting.dto.TimeTrackingDTO;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper MapStruct pour TimeTracking ↔ TimeTrackingDTO
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TimeTrackingMapper {

    /**
     * Convertit TimeTracking → TimeTrackingDTO
     */
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user", target = "userFullName", qualifiedByName = "getUserFullName")
    @Mapping(source = "company.id", target = "companyId")
    @Mapping(source = "company.name", target = "companyName")
    @Mapping(target = "durationHours", expression = "java(timeTracking.getDurationHours())")
    @Mapping(target = "billableAmount", expression = "java(timeTracking.calculateBillableAmount())")
    @Mapping(target = "canBeBilled", expression = "java(timeTracking.canBeBilled())")
    TimeTrackingDTO toDTO(TimeTracking timeTracking);

    /**
     * Convertit TimeTrackingDTO → TimeTracking
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    TimeTracking toEntity(TimeTrackingDTO dto);

    /**
     * Convertit une liste
     */
    List<TimeTrackingDTO> toDTOList(List<TimeTracking> timeTrackings);

    /**
     * Met à jour une entité existante depuis un DTO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromDTO(TimeTrackingDTO dto, @MappingTarget TimeTracking timeTracking);

    /**
     * Méthode personnalisée pour obtenir le nom complet de l'utilisateur
     */
    @Named("getUserFullName")
    default String getUserFullName(User user) {
        if (user == null) return null;
        return user.getFullName();
    }
}
