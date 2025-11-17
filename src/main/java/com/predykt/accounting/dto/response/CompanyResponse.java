// CompanyResponse.java
package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {
    private Long id;
    private String name;
    private String taxId;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private String currency;
    private String accountingStandard;
    private Boolean isActive;
    private Boolean isVatRegistered;
    private LocalDateTime createdAt;
}