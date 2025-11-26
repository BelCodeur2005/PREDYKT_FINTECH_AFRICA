package com.predykt.accounting.domain.enums;

/**
 * Niveaux d'accès pour UserCompanyAccess (MODE CABINET)
 */
public enum AccessLevel {
    /**
     * Lecture seule
     */
    READ_ONLY,

    /**
     * Lecture et écriture
     */
    READ_WRITE,

    /**
     * Administrateur du dossier
     */
    ADMIN
}
