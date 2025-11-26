package com.predykt.accounting.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour Cabinet (MODE CABINET)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetDTO {

    private Long id;

    @NotBlank(message = "Le nom du cabinet est obligatoire")
    @Size(max = 200, message = "Le nom ne peut pas dépasser 200 caractères")
    private String name;

    @Size(max = 50, message = "Le code ne peut pas dépasser 50 caractères")
    private String code;

    @Size(max = 500, message = "L'adresse ne peut pas dépasser 500 caractères")
    private String address;

    @Size(max = 20, message = "Le téléphone ne peut pas dépasser 20 caractères")
    private String phone;

    @Email(message = "Format email invalide")
    @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères")
    private String email;

    @NotNull(message = "Le nombre maximum d'entreprises est obligatoire")
    private Integer maxCompanies;

    @NotNull(message = "Le nombre maximum d'utilisateurs est obligatoire")
    private Integer maxUsers;

    @NotBlank(message = "Le plan est obligatoire")
    @Size(max = 50, message = "Le plan ne peut pas dépasser 50 caractères")
    private String plan;

    private Long currentCompaniesCount;

    private Long currentUsersCount;

    private Boolean hasReachedCompanyLimit;

    private Boolean hasReachedUserLimit;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
