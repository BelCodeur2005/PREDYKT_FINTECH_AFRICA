package com.predykt.accounting.service.ml;

import com.predykt.accounting.domain.entity.ml.MLModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import smile.classification.RandomForest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service de s\u00e9rialisation/d\u00e9s\u00e9rialisation des mod\u00e8les ML
 * G\u00e8re le stockage sur disque des mod\u00e8les Random Forest
 *
 * Format: Fichiers .model (Java Serialization)
 * Chemin: {baseDir}/{companyId}/model-{version}-{timestamp}.model
 *
 * @author PREDYKT ML Team
 */
@Service
@Slf4j
public class MLModelStorageService {

    @Value("${predykt.ml.models.base-dir:./ml-models}")
    private String baseDir;

    /**
     * Sauvegarde un mod\u00e8le Random Forest sur disque
     *
     * @param rf Mod\u00e8le Random Forest entra\u00een\u00e9
     * @param companyId ID de l'entreprise
     * @param version Version du mod\u00e8le
     * @return Chemin du fichier sauvegard\u00e9
     */
    public String saveModel(RandomForest rf, Long companyId, String version) {
        try {
            // Cr\u00e9er le r\u00e9pertoire si n\u00e9cessaire
            Path companyDir = Paths.get(baseDir, companyId.toString());
            Files.createDirectories(companyDir);

            // Nom du fichier avec timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String filename = String.format("model-%s-%s.model", version, timestamp);
            Path modelPath = companyDir.resolve(filename);

            // S\u00e9rialiser le mod\u00e8le
            try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(modelPath.toFile())))) {
                oos.writeObject(rf);
            }

            log.info("Mod\u00e8le ML sauvegard\u00e9: {} ({} bytes)",
                modelPath, Files.size(modelPath));

            return modelPath.toString();

        } catch (IOException e) {
            log.error("Erreur sauvegarde mod\u00e8le ML: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible de sauvegarder le mod\u00e8le ML", e);
        }
    }

    /**
     * Charge un mod\u00e8le Random Forest depuis le disque
     *
     * @param model Entit\u00e9 MLModel contenant le chemin
     * @return Mod\u00e8le Random Forest charg\u00e9, ou null si erreur
     */
    public RandomForest loadModel(MLModel model) {
        if (model.getModelPath() == null) {
            log.error("MLModel {} n'a pas de modelPath d\u00e9fini", model.getId());
            return null;
        }

        // V\u00e9rifier si d\u00e9j\u00e0 charg\u00e9 en m\u00e9moire
        if (model.getRandomForest() != null) {
            return model.getRandomForest();
        }

        return loadModelFromPath(model.getModelPath());
    }

    /**
     * Charge un mod\u00e8le depuis un chemin
     */
    public RandomForest loadModelFromPath(String modelPath) {
        try {
            Path path = Paths.get(modelPath);

            if (!Files.exists(path)) {
                log.error("Fichier mod\u00e8le introuvable: {}", modelPath);
                return null;
            }

            try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(path.toFile())))) {
                RandomForest rf = (RandomForest) ois.readObject();

                log.info("Mod\u00e8le ML charg\u00e9: {} ({} bytes)",
                    modelPath, Files.size(path));

                return rf;
            }

        } catch (IOException | ClassNotFoundException e) {
            log.error("Erreur chargement mod\u00e8le ML {}: {}", modelPath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Supprime un ancien mod\u00e8le du disque
     */
    public void deleteModel(String modelPath) {
        try {
            Path path = Paths.get(modelPath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("Mod\u00e8le ML supprim\u00e9: {}", modelPath);
            }
        } catch (IOException e) {
            log.warn("Impossible de supprimer le mod\u00e8le {}: {}", modelPath, e.getMessage());
        }
    }

    /**
     * Nettoie les anciens mod\u00e8les (garde les 5 derniers par entreprise)
     */
    public void cleanupOldModels(Long companyId) {
        try {
            Path companyDir = Paths.get(baseDir, companyId.toString());
            if (!Files.exists(companyDir)) return;

            // Lister tous les fichiers .model
            File[] modelFiles = companyDir.toFile().listFiles(
                (dir, name) -> name.endsWith(".model")
            );

            if (modelFiles == null || modelFiles.length <= 5) {
                return;  // Moins de 5 mod\u00e8les, on garde tout
            }

            // Trier par date de modification (plus ancien en premier)
            java.util.Arrays.sort(modelFiles,
                java.util.Comparator.comparingLong(File::lastModified));

            // Supprimer les plus anciens (garder les 5 derniers)
            int toDelete = modelFiles.length - 5;
            for (int i = 0; i < toDelete; i++) {
                Files.delete(modelFiles[i].toPath());
                log.info("Ancien mod\u00e8le supprim\u00e9: {}", modelFiles[i].getName());
            }

            log.info("Nettoyage termin\u00e9 pour company {}: {} mod\u00e8les supprim\u00e9s",
                companyId, toDelete);

        } catch (IOException e) {
            log.error("Erreur nettoyage mod\u00e8les company {}: {}", companyId, e.getMessage());
        }
    }

    /**
     * Exporte un mod\u00e8le vers un fichier ZIP (backup)
     */
    public String exportModelBackup(MLModel model, String exportDir) {
        try {
            Path exportPath = Paths.get(exportDir);
            Files.createDirectories(exportPath);

            String backupFilename = String.format("backup-model-%s-%s.model",
                model.getModelVersion(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            );

            Path backupPath = exportPath.resolve(backupFilename);

            // Copier le fichier mod\u00e8le
            Files.copy(Paths.get(model.getModelPath()), backupPath);

            log.info("Backup mod\u00e8le cr\u00e9\u00e9: {}", backupPath);
            return backupPath.toString();

        } catch (IOException e) {
            log.error("Erreur export backup mod\u00e8le: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible d'exporter le mod\u00e8le", e);
        }
    }

    /**
     * V\u00e9rifie l'int\u00e9grit\u00e9 d'un mod\u00e8le (peut \u00eatre charg\u00e9 ?)
     */
    public boolean verifyModelIntegrity(String modelPath) {
        try {
            RandomForest rf = loadModelFromPath(modelPath);
            return rf != null;
        } catch (Exception e) {
            log.error("Mod\u00e8le corrompu: {}", modelPath);
            return false;
        }
    }

    /**
     * Retourne la taille du mod\u00e8le en bytes
     */
    public long getModelSize(String modelPath) {
        try {
            Path path = Paths.get(modelPath);
            return Files.exists(path) ? Files.size(path) : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Liste tous les mod\u00e8les d'une entreprise
     */
    public java.util.List<String> listCompanyModels(Long companyId) {
        try {
            Path companyDir = Paths.get(baseDir, companyId.toString());
            if (!Files.exists(companyDir)) {
                return java.util.Collections.emptyList();
            }

            File[] modelFiles = companyDir.toFile().listFiles(
                (dir, name) -> name.endsWith(".model")
            );

            if (modelFiles == null) {
                return java.util.Collections.emptyList();
            }

            return java.util.Arrays.stream(modelFiles)
                .map(f -> f.getAbsolutePath())
                .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("Erreur listing mod\u00e8les company {}: {}", companyId, e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
}