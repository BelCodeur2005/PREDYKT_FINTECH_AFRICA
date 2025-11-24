
// ============================================
// UserUpdateRequest.java
// ============================================
package com.predykt.accounting.dto.request;

import lombok.Data;

import java.util.Set;

@Data
public class UserUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private Boolean isActive;
    private Set<Integer> roleIds;
}