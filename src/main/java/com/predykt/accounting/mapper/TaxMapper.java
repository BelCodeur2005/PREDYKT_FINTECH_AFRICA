package com.predykt.accounting.mapper;

import com.predykt.accounting.domain.entity.TaxCalculation;
import com.predykt.accounting.domain.entity.TaxConfiguration;
import com.predykt.accounting.dto.response.TaxAlertResponse;
import com.predykt.accounting.dto.response.TaxConfigurationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper pour les taxes
 */
@Mapper(componentModel = "spring")
public interface TaxMapper {

    @Mapping(target = "taxType", expression = "java(config.getTaxType().name())")
    TaxConfigurationResponse toConfigurationResponse(TaxConfiguration config);

    @Mapping(target = "taxType", expression = "java(calc.getTaxType().name())")
    @Mapping(target = "supplierName", expression = "java(calc.getSupplier() != null ? calc.getSupplier().getName() : null)")
    @Mapping(target = "niuNumber", expression = "java(calc.getSupplier() != null ? calc.getSupplier().getNiuNumber() : null)")
    @Mapping(target = "penaltyCost", expression = "java(calc.calculatePenaltyCost())")
    @Mapping(target = "severity", expression = "java(calc.hasPenaltyRate() ? \"HIGH\" : \"MEDIUM\")")
    @Mapping(target = "actionRequired", expression = "java(calc.hasPenaltyRate() ? \"Régulariser NIU fournisseur\" : \"Vérifier calcul\")")
    TaxAlertResponse toAlertResponse(TaxCalculation calc);
}
