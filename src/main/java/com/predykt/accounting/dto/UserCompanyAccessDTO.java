package com.predykt.accounting.dto;

import com.predykt.accounting.domain.enums.AccessLevel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour UserCompanyAccess (MODE CABINET)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCompanyAccessDTO {

    @NotNull(message = "L'ID de l'utilisateur est obligatoire")
    private Long userId;

    private String userEmail;

    private String userFullName;

    @NotNull(message = "L'ID de l'entreprise est obligatoire")
    private Long companyId;

    private String companyName;

    @NotNull(message = "Le niveau d'accès est obligatoire")
    private AccessLevel accessLevel;

    private Boolean canWrite;

    private Boolean isAdmin;

    private LocalDateTime grantedAt;

    private String grantedBy;
}
