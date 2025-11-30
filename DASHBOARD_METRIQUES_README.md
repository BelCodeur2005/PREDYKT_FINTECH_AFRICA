# 📊 Dashboard de Métriques - Système de Matching Bancaire Intelligent

## 🎯 Vue d'ensemble

Le **Dashboard de Métriques** est un système d'analyse et de monitoring complet qui permet de mesurer la performance du système de matching automatique bancaire et la productivité des équipes comptables.

### Objectifs principaux

1. **Suivi de la qualité** : Mesurer la précision du matching automatique
2. **Amélioration continue** : Identifier les points faibles et axes d'amélioration
3. **Optimisation des paramètres** : Ajuster les seuils de confiance si nécessaire
4. **Management** : Suivre la productivité et qualité du travail des comptables
5. **Audit & Conformité** : Traçabilité des décisions pour conformité OHADA

---

## 👥 Qui peut accéder au Dashboard ?

### Niveaux d'accès

| Rôle | Accès | Métriques visibles |
|------|-------|-------------------|
| **ADMIN** | ✅ Complet | Toutes les métriques (système + utilisateurs) |
| **MANAGER** | ✅ Complet | Toutes les métriques (système + utilisateurs) |
| **ACCOUNTANT** | ⚠️ Limité | Uniquement ses propres métriques (TODO: endpoint `/me`) |
| **VIEWER** | ❌ Aucun | Pas d'accès aux métriques |

> **Note :** Dans la version actuelle, la sécurité est DÉSACTIVÉE (MVP). En production, activer `@PreAuthorize` dans le contrôleur.

---

## 🛠️ Endpoints disponibles

### Base URL
```
/api/v1/companies/{companyId}/reconciliations/metrics
```

### 1️⃣ Métriques globales de performance
```http
GET /performance?startDate=2024-01-01&endDate=2024-01-31
```

**Retourne :**
- Métriques globales (taux de précision, analyses, temps moyen)
- Distribution par niveau de confiance (EXCELLENT/GOOD/FAIR/LOW)
- Performance par type de suggestion
- Top 10 raisons de rejet avec actions suggérées
- Performance par volume de transactions
- Évolution temporelle (si période > 1 mois)
- Recommandations d'amélioration automatiques

**Exemple de réponse :**
```json
{
  "success": true,
  "message": "📊 Analyse complétée : 1247 analyses, 91.3% de précision, 5 recommandations",
  "data": {
    "startDate": "2024-01-01",
    "endDate": "2024-01-31",
    "globalMetrics": {
      "totalAnalyses": 1247,
      "totalTransactionsAnalyzed": 12845,
      "totalSuggestionsGenerated": 3894,
      "totalSuggestionsApplied": 3556,
      "totalSuggestionsRejected": 278,
      "totalSuggestionsPending": 60,
      "overallPrecisionRate": 91.32,
      "averageConfidenceScore": 87.45,
      "averageAnalysisTimeSeconds": 2.8,
      "medianAnalysisTimeSeconds": 1.9,
      "p95AnalysisTimeSeconds": 8.4,
      "monthOverMonthChange": 4.2
    },
    "confidenceLevelBreakdown": [
      {
        "confidenceLevel": "EXCELLENT",
        "scoreRange": "95-100%",
        "count": 2140,
        "applied": 2098,
        "rejected": 32,
        "applicationRate": 97.9,
        "percentage": 55.0
      },
      {
        "confidenceLevel": "GOOD",
        "scoreRange": "80-94%",
        "count": 1234,
        "applied": 1123,
        "rejected": 98,
        "applicationRate": 91.0,
        "percentage": 31.7
      }
    ],
    "topRejectionReasons": [
      {
        "reason": "Montant incorrect",
        "count": 87,
        "percentage": 31.3,
        "suggestedAction": "Revoir la configuration amount-tolerance",
        "priority": "HIGH"
      },
      {
        "reason": "Date trop éloignée",
        "count": 56,
        "percentage": 20.1,
        "suggestedAction": "Augmenter date-thresholds.fair-match-days",
        "priority": "MEDIUM"
      }
    ],
    "recommendations": [
      "✅ EXCELLENT PRECISION (91.32%) : You can enable auto-application for suggestions > 95% confidence",
      "⚠️ TYPE 'Crédit non identifié' has low application rate (43.6%) : Improve detection criteria or lower confidence",
      "🔧 HIGH REJECTION RATE for 'Montant incorrect' (31.3%) : Review amount-tolerance configuration"
    ]
  }
}
```

---

### 2️⃣ Distribution par niveau de confiance
```http
GET /confidence-breakdown?startDate=2024-01-01&endDate=2024-01-31
```

**Utilité :** Valider les seuils de confiance et décider d'activer l'auto-approbation.

**Exemple :**
Si le niveau EXCELLENT a un taux d'application > 98%, vous pouvez activer l'auto-approbation pour ces suggestions :
```yaml
predykt:
  reconciliation:
    matching:
      auto-approve-threshold: 95  # Activer l'auto-approbation
```

---

### 3️⃣ Top raisons de rejet
```http
GET /rejection-reasons?startDate=2024-01-01&endDate=2024-01-31&limit=10
```

**Utilité :** Identifier les faiblesses de l'algorithme et prioriser les corrections.

**Actions typiques selon les raisons :**

| Raison | Action recommandée | Configuration |
|--------|-------------------|---------------|
| "Montant incorrect" | Revoir tolérance de montant | `amount-tolerance.*` |
| "Date trop éloignée" | Augmenter seuil de date | `date-thresholds.fair-match-days` |
| "Description ne correspond pas" | Améliorer similarité textuelle | `text-similarity.threshold` |
| "Type de transaction incorrect" | Ajouter mots-clés | `heuristics.*-keywords` |

---

### 4️⃣ Performance par volume
```http
GET /volume-performance?startDate=2024-01-01&endDate=2024-01-31
```

**Utilité :** Détecter si les performances se dégradent au-delà d'un certain volume.

**Exemple de réponse :**
```json
[
  {
    "volumeRange": "< 50 tx",
    "analysesCount": 450,
    "averageTimeSeconds": 1.2,
    "maxTimeSeconds": 3.4,
    "p95TimeSeconds": 2.1,
    "averagePrecision": 93.5,
    "status": "OK"
  },
  {
    "volumeRange": "200-500 tx",
    "analysesCount": 78,
    "averageTimeSeconds": 12.5,
    "maxTimeSeconds": 45.2,
    "p95TimeSeconds": 38.7,
    "averagePrecision": 87.2,
    "status": "WARNING"
  },
  {
    "volumeRange": "> 500 tx",
    "analysesCount": 12,
    "averageTimeSeconds": 78.3,
    "maxTimeSeconds": 89.9,
    "p95TimeSeconds": 89.1,
    "averagePrecision": 81.5,
    "status": "CRITICAL"
  }
]
```

**Actions selon le statut :**
- **OK** : Rien à faire
- **WARNING** : Surveiller, envisager optimisation
- **CRITICAL** : Activer le mode haute performance :
  ```yaml
  performance:
    high-performance-mode: true  # Sacrifie précision pour vitesse
    max-candidates-for-multiple-matching: 20  # Réduire
  ```

---

### 5️⃣ Évolution temporelle (Time Series)
```http
GET /time-series?startDate=2024-01-01&endDate=2024-01-31
```

**Utilité :** Alimenter des graphiques de dashboard, détecter des tendances.

**Exemple de réponse :**
```json
[
  {
    "date": "2024-01-01",
    "suggestionsGenerated": 45,
    "suggestionsApplied": 42,
    "precisionRate": 93.3,
    "averageTimeSeconds": 2.1
  },
  {
    "date": "2024-01-02",
    "suggestionsGenerated": 67,
    "suggestionsApplied": 61,
    "precisionRate": 91.0,
    "averageTimeSeconds": 2.8
  }
]
```

---

### 6️⃣ Productivité des utilisateurs
```http
GET /user-productivity?startDate=2024-01-01&endDate=2024-01-31
```

⚠️ **IMPORTANT - Réservé ADMIN/MANAGER uniquement**

**Utilité :**
- Revue de performance individuelle et d'équipe
- Identifier les utilisateurs nécessitant formation
- Valoriser les meilleurs performers

**Exemple de réponse :**
```json
{
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "userMetrics": [
    {
      "userId": "user123",
      "userName": "Marie Dupont",
      "userEmail": "marie.dupont@example.com",
      "reconciliationsCompleted": 45,
      "transactionsProcessed": 1234,
      "suggestionsApplied": 567,
      "suggestionsRejected": 23,
      "applicationRate": 96.1,
      "averageTimePerReconciliation": 18.5,
      "precisionScore": 95.2,
      "performanceLevel": "EXCELLENT",
      "ranking": 1,
      "productivityIndex": 94.5
    },
    {
      "userId": "user456",
      "userName": "Jean Martin",
      "userEmail": "jean.martin@example.com",
      "reconciliationsCompleted": 38,
      "transactionsProcessed": 987,
      "suggestionsApplied": 432,
      "suggestionsRejected": 78,
      "applicationRate": 84.7,
      "averageTimePerReconciliation": 25.3,
      "precisionScore": 87.5,
      "performanceLevel": "GOOD",
      "ranking": 2,
      "productivityIndex": 78.3
    }
  ],
  "teamMetrics": {
    "totalUsers": 5,
    "activeUsers": 4,
    "totalReconciliations": 234,
    "totalTransactions": 8456,
    "averageApplicationRate": 89.5,
    "averageTimePerReconciliation": 22.1,
    "teamPrecisionScore": 90.8,
    "bestPerformer": "Marie Dupont",
    "bestPerformerScore": 94.5
  }
}
```

**Niveaux de performance :**
- **EXCELLENT** : Index > 90
- **GOOD** : Index 75-90
- **AVERAGE** : Index 60-75
- **NEEDS_IMPROVEMENT** : Index < 60

---

### 7️⃣ Recommandations d'amélioration
```http
GET /recommendations?startDate=2024-01-01&endDate=2024-01-31
```

**Retourne :** Liste de recommandations actionnables générées automatiquement.

**Exemples de recommandations :**
```json
[
  "✅ EXCELLENT PRECISION (91.32%) : You can enable auto-application for suggestions > 95% confidence",
  "⚠️ TYPE 'Crédit non identifié' has low application rate (43.6%) : Improve detection criteria or lower confidence",
  "⚠️ HIGH REJECTION RATE (> 15%) : Review configuration or provide user training",
  "🔧 Configure heuristics.virement-keywords to improve transfer detection",
  "📚 User 'Jean Martin' has low application rate (67.3%) : Provide training on suggestion validation"
]
```

---

### 8️⃣ Résumé exécutif
```http
GET /executive-summary?startDate=2024-01-01&endDate=2024-01-31
```

**Utilité :** Rapport mensuel pour la direction (1 page, KPIs essentiels).

**Exemple de réponse :**
```json
{
  "period": "2024-01-01 → 2024-01-31",
  "totalAnalyses": 1247,
  "overallPrecisionRate": 91.32,
  "averageConfidenceScore": 87.45,
  "averageAnalysisTimeSeconds": 2.8,
  "monthOverMonthChange": 4.2,
  "topRecommendations": [
    "✅ EXCELLENT PRECISION (91.32%) : You can enable auto-application",
    "⚠️ TYPE 'Crédit non identifié' has low application rate (43.6%)",
    "🔧 HIGH REJECTION RATE for 'Montant incorrect' (31.3%)"
  ]
}
```

---

## 📈 Cas d'usage typiques

### 1. Revue mensuelle de performance

**Objectif :** Présenter les résultats du mois au responsable comptable.

**Workflow :**
1. Appeler `/executive-summary` pour le mois écoulé
2. Générer un graphique avec `/time-series` pour visualiser l'évolution
3. Analyser les `/rejection-reasons` pour identifier les axes d'amélioration
4. Partager les `/user-productivity` avec l'équipe (avec discrétion)

---

### 2. Optimisation des paramètres

**Objectif :** Ajuster la configuration pour améliorer la précision.

**Workflow :**
1. Appeler `/performance` pour voir le taux de précision global
2. Analyser `/confidence-breakdown` :
   - Si EXCELLENT a 98%+ d'application → Activer auto-approbation
   - Si FAIR a < 70% d'application → Augmenter le seuil à 75%
3. Analyser `/rejection-reasons` :
   - Si "Montant incorrect" est #1 → Ajuster `amount-tolerance`
   - Si "Date trop éloignée" est fréquent → Augmenter `date-thresholds`
4. Modifier `application.yaml` et redémarrer
5. Re-tester après 1 semaine

---

### 3. Détection de problèmes de performance

**Objectif :** Identifier pourquoi certains rapprochements sont lents.

**Workflow :**
1. Appeler `/volume-performance`
2. Identifier les tranches en statut WARNING ou CRITICAL
3. Si volume > 500 tx est CRITICAL :
   - Activer `high-performance-mode: true`
   - Réduire `max-candidates-for-multiple-matching: 20`
4. Re-tester et vérifier que le statut passe à OK

---

### 4. Formation des utilisateurs

**Objectif :** Identifier les comptables nécessitant un accompagnement.

**Workflow :**
1. Appeler `/user-productivity` pour le dernier trimestre
2. Identifier les utilisateurs avec :
   - `performanceLevel: NEEDS_IMPROVEMENT`
   - `applicationRate < 80%` (rejettent trop de bonnes suggestions)
   - `averageTimePerReconciliation > 30 min` (trop lents)
3. Organiser une formation personnalisée
4. Re-mesurer après 1 mois pour valider l'amélioration

---

## 🔧 Configuration recommandée

### Intervalles de mesure

| Période | Utilité |
|---------|---------|
| **Dernière semaine** | Détecter les problèmes récents |
| **Dernier mois** | Rapport mensuel standard |
| **Dernier trimestre** | Tendances à moyen terme |
| **Dernière année** | Vue stratégique, évolution annuelle |

### Seuils d'alerte

| Métrique | Seuil OK | Seuil WARNING | Seuil CRITICAL |
|----------|----------|---------------|----------------|
| Taux de précision | ≥ 90% | 80-90% | < 80% |
| Taux de rejet | ≤ 10% | 10-20% | > 20% |
| Temps moyen (< 100 tx) | ≤ 5s | 5-10s | > 10s |
| Temps moyen (> 500 tx) | ≤ 60s | 60-90s | > 90s |

---

## 🎨 Intégration Frontend (Future)

### Graphiques recommandés

1. **Gauge Chart** : Taux de précision global (avec zones verte/orange/rouge)
2. **Line Chart** : Évolution du taux de précision (time series)
3. **Donut Chart** : Distribution par niveau de confiance
4. **Bar Chart** : Top 10 raisons de rejet
5. **Heatmap** : Performance par volume et par date
6. **Table** : Classement de productivité des utilisateurs

### Librairies suggérées
- **Chart.js** : Simple et efficace
- **Recharts** : Pour React
- **ApexCharts** : Graphiques interactifs avancés
- **D3.js** : Maximum de flexibilité

---

## 🔒 Sécurité et confidentialité

### Données sensibles

⚠️ Les métriques de productivité utilisateurs sont **SENSIBLES** :
- Ne jamais partager publiquement
- Limiter l'accès aux ADMIN/MANAGER uniquement
- Utiliser avec discrétion pour éviter pression excessive
- Préférer les métriques d'équipe agrégées

### Protection RGPD/GDPR

- Les données de productivité sont des **données personnelles**
- Informer les utilisateurs de la collecte de ces métriques
- Obtenir consentement si requis par votre juridiction
- Permettre aux utilisateurs d'accéder à leurs propres métriques
- Implémenter le droit à l'oubli (suppression sur demande)

---

## 🚀 Déploiement en production

### Checklist

- [ ] Activer la sécurité dans `SecurityConfig.java`
- [ ] Ajouter `@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")` sur `/user-productivity`
- [ ] Configurer les logs de monitoring (ELK, Prometheus, etc.)
- [ ] Créer des dashboards Grafana pour visualisation temps réel
- [ ] Configurer des alertes si précision < 80%
- [ ] Documenter les KPIs dans le wiki interne
- [ ] Former les managers à l'utilisation du dashboard
- [ ] Planifier une revue mensuelle des métriques

---

## 📚 Références

### Fichiers du projet

- **Controller** : `src/main/java/com/predykt/accounting/controller/MatchingMetricsController.java`
- **Service** : `src/main/java/com/predykt/accounting/service/MatchingMetricsService.java`
- **DTOs** :
  - `src/main/java/com/predykt/accounting/dto/response/MatchingMetricsResponse.java`
  - `src/main/java/com/predykt/accounting/dto/response/UserProductivityMetricsResponse.java`
- **Repository** : `src/main/java/com/predykt/accounting/repository/BankReconciliationSuggestionRepository.java`

### Documentation associée

- `RAPPROCHEMENT_BANCAIRE_INTELLIGENT.md` : Guide pour comptables
- `ANALYSE_ROBUSTESSE_MATCHING.md` : Analyse technique
- `CHANGELOG_MATCHING_V2.md` : Améliorations VERSION 2.0
- `CONFIGURATION_MATCHING_V2.yaml` : Configuration de référence

---

## 🆘 Support

### Questions fréquentes

**Q: Le taux de précision affiché est-il fiable ?**
R: Oui, il reflète le % de suggestions appliquées vs rejetées. Mais attention : un comptable peut rejeter une bonne suggestion (faux négatif) ou accepter une mauvaise (faux positif). Croiser avec les retours terrain.

**Q: Pourquoi certaines métriques sont à 0 ?**
R: Aucune donnée pour la période sélectionnée. Vérifier que des rapprochements ont été effectués avec matching automatique activé.

**Q: Comment interpréter le "productivityIndex" ?**
R: Formule : `(applicationRate * 0.4) + (precisionScore * 0.4) + (speedScore * 0.2)`. Score de 0-100, plus c'est élevé, mieux c'est.

**Q: Les métriques incluent-elles les suggestions auto-approuvées ?**
R: Oui, les suggestions auto-approuvées sont comptées comme APPLIED.

---

## 📞 Contact

Pour toute question ou suggestion d'amélioration du Dashboard :
- **Email** : support@predykt.com
- **GitHub Issues** : https://github.com/predykt/accounting-api/issues
- **Wiki** : https://wiki.predykt.com/dashboard-metriques

---

**Version :** 2.0
**Dernière mise à jour :** 2024-11-30
**Auteur :** Équipe PREDYKT
