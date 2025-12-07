package com.predykt.accounting.domain.enums;

/**
 * Statut d'un modèle ML dans son cycle de vie
 *
 * @author PREDYKT ML Team
 */
public enum MLModelStatus {
    /**
     * Entraînement en cours
     */
    TRAINING,

    /**
     * Entraîné avec succès, prêt à être déployé
     */
    TRAINED,

    /**
     * Déployé en production (utilisé pour les prédictions)
     */
    DEPLOYED,

    /**
     * Dépréc ié (remplacé par une version plus récente)
     */
    DEPRECATED
}