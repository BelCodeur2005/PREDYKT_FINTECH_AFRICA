package com.predykt.accounting.mapper;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.User;
import com.predykt.accounting.domain.entity.UserCompanyAccess;
import com.predykt.accounting.dto.UserCompanyAccessDTO;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper MapStruct pour UserCompanyAccess ↔ UserCompanyAccessDTO
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserCompanyAccessMapper {

    /**
     * Convertit UserCompanyAccess → UserCompanyAccessDTO
     */
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "user", target = "userFullName", qualifiedByName = "getUserFullName")
    @Mapping(source = "company.id", target = "companyId")
    @Mapping(source = "company.name", target = "companyName")
    @Mapping(target = "canWrite", expression = "java(access.canWrite())")
    @Mapping(target = "isAdmin", expression = "java(access.isAdmin())")
    UserCompanyAccessDTO toDTO(UserCompanyAccess access);

    /**
     * Convertit UserCompanyAccessDTO → UserCompanyAccess
     */
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "grantedAt", ignore = true)
    @Mapping(target = "grantedBy", ignore = true)
    UserCompanyAccess toEntity(UserCompanyAccessDTO dto);

    /**
     * Convertit une liste
     */
    List<UserCompanyAccessDTO> toDTOList(List<UserCompanyAccess> accesses);

    /**
     * Méthode personnalisée pour obtenir le nom complet de l'utilisateur
     */
    @Named("getUserFullName")
    default String getUserFullName(User user) {
        if (user == null) return null;
        return user.getFullName();
    }
}
