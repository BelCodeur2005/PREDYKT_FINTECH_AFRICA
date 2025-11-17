package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Entity
@Table(name = "companies", indexes = {
    @Index(name = "idx_company_tax_id", columnList = "tax_id"),
    @Index(name = "idx_company_email", columnList = "email")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    @Column(nullable = false, length = 200)
    private String name;
    
    @Pattern(regexp = "^[A-Z0-9]{10,20}$", message = "Format du numéro fiscal invalide")
    @Column(name = "tax_id", unique = true, length = 50)
    private String taxId;  // Numéro de contribuable
    
    @Email(message = "Email invalide")
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(length = 20)
    private String phone;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    @Column(length = 10)
    private String postalCode;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 2, nullable = false)
    @Builder.Default
    private String country = "CM";  // ISO 3166-1 alpha-2
    
    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "XAF";  // ISO 4217
    
    @Column(name = "accounting_standard", length = 20)
    @Builder.Default
    private String accountingStandard = "OHADA";  // OHADA, IFRS, SYSCOHADA
    
    @Column(name = "fiscal_year_start", length = 5)
    @Builder.Default
    private String fiscalYearStart = "01-01";  // MM-DD
    
    @Column(name = "fiscal_year_end", length = 5)
    @Builder.Default
    private String fiscalYearEnd = "12-31";
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "vat_number", length = 50)
    private String vatNumber;  // Numéro TVA si assujetti
    
    @Column(name = "is_vat_registered")
    @Builder.Default
    private Boolean isVatRegistered = false;
}