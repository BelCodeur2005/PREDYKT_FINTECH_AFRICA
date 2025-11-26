package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Cabinet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour l'entité Cabinet (MODE CABINET)
 */
@Repository
public interface CabinetRepository extends JpaRepository<Cabinet, Long> {

    /**
     * Trouve un cabinet par son nom
     */
    Optional<Cabinet> findByName(String name);

    /**
     * Trouve un cabinet par son code
     */
    Optional<Cabinet> findByCode(String code);

    /**
     * Vérifie si un cabinet existe par nom
     */
    boolean existsByName(String name);

    /**
     * Vérifie si un cabinet existe par code
     */
    boolean existsByCode(String code);

    /**
     * Compte le nombre d'entreprises d'un cabinet
     */
    @Query("SELECT COUNT(c) FROM Company c WHERE c.cabinet.id = :cabinetId")
    Long countCompaniesByCabinetId(@Param("cabinetId") Long cabinetId);

    /**
     * Compte le nombre d'utilisateurs d'un cabinet
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.cabinet.id = :cabinetId")
    Long countUsersByCabinetId(@Param("cabinetId") Long cabinetId);

    /**
     * Vérifie si un cabinet a atteint sa limite d'entreprises
     */
    @Query("SELECT CASE WHEN COUNT(c) >= cab.maxCompanies THEN true ELSE false END " +
           "FROM Company c JOIN c.cabinet cab WHERE cab.id = :cabinetId")
    boolean hasReachedCompanyLimit(@Param("cabinetId") Long cabinetId);

    /**
     * Vérifie si un cabinet a atteint sa limite d'utilisateurs
     */
    @Query("SELECT CASE WHEN COUNT(u) >= cab.maxUsers THEN true ELSE false END " +
           "FROM User u JOIN u.cabinet cab WHERE cab.id = :cabinetId")
    boolean hasReachedUserLimit(@Param("cabinetId") Long cabinetId);
}
