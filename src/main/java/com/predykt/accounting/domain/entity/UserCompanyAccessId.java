package com.predykt.accounting.domain.entity;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clé composite pour UserCompanyAccess
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserCompanyAccessId implements Serializable {

    private Long user;
    private Long company;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserCompanyAccessId that = (UserCompanyAccessId) o;
        return Objects.equals(user, that.user) && Objects.equals(company, that.company);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, company);
    }
}
