package com.predykt.accounting.mapper;

import com.predykt.accounting.domain.entity.FixedAsset;
import com.predykt.accounting.dto.request.FixedAssetCreateRequest;
import com.predykt.accounting.dto.request.FixedAssetUpdateRequest;
import com.predykt.accounting.dto.response.FixedAssetResponse;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper MapStruct pour la conversion entre entités FixedAsset et DTOs
 * Génération automatique de l'implémentation au moment de la compilation
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface FixedAssetMapper {

    /**
     * Convertir une requête de création en entité
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "totalCost", ignore = true) // Calculé par @PrePersist
    @Mapping(target = "depreciationRate", ignore = true) // Calculé par @PrePersist
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "isFullyDepreciated", constant = "false")
    @Mapping(target = "disposalDate", ignore = true)
    @Mapping(target = "disposalAmount", ignore = true)
    @Mapping(target = "disposalReason", ignore = true)
    FixedAsset toEntity(FixedAssetCreateRequest request);

    /**
     * Mettre à jour une entité existante avec les valeurs de la requête
     * Seuls les champs non-null de la requête sont appliqués
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "assetNumber", ignore = true) // Non modifiable
    @Mapping(target = "category", ignore = true) // Non modifiable
    @Mapping(target = "accountNumber", ignore = true) // Non modifiable
    @Mapping(target = "acquisitionDate", ignore = true) // Non modifiable
    @Mapping(target = "acquisitionCost", ignore = true) // Non modifiable
    @Mapping(target = "depreciationMethod", ignore = true) // Non modifiable après création
    @Mapping(target = "totalCost", ignore = true) // Recalculé par @PreUpdate
    @Mapping(target = "depreciationRate", ignore = true) // Recalculé par @PreUpdate
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "isFullyDepreciated", ignore = true)
    @Mapping(target = "disposalDate", ignore = true)
    @Mapping(target = "disposalAmount", ignore = true)
    @Mapping(target = "disposalReason", ignore = true)
    void updateEntity(@MappingTarget FixedAsset entity, FixedAssetUpdateRequest request);

    /**
     * Convertir une entité en réponse simple
     */
    @Mapping(target = "categoryName", source = "category.displayName")
    @Mapping(target = "depreciationMethodName", source = "depreciationMethod.displayName")
    @Mapping(target = "isDisposed", expression = "java(entity.isDisposed())")
    @Mapping(target = "currentAccumulatedDepreciation", ignore = true) // Calculé dans le service
    @Mapping(target = "currentNetBookValue", ignore = true) // Calculé dans le service
    @Mapping(target = "ageInYears", ignore = true) // Calculé dans le service
    @Mapping(target = "ageInMonths", ignore = true) // Calculé dans le service
    @Mapping(target = "depreciationProgress", ignore = true) // Calculé dans le service
    @Mapping(target = "disposalGainLoss", ignore = true) // Calculé dans le service
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "statusLabel", ignore = true) // Calculé dans le service
    @Mapping(target = "statusIcon", ignore = true) // Calculé dans le service
    @Mapping(target = "needsRenewal", ignore = true) // Calculé dans le service
    FixedAssetResponse toResponse(FixedAsset entity);

    /**
     * Convertir une liste d'entités en liste de réponses
     */
    List<FixedAssetResponse> toResponseList(List<FixedAsset> entities);
}
