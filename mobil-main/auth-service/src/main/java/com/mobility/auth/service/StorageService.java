package com.mobility.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

/**
 * Service de stockage abstrait (ex : S3, GCS, Azure Blob…).
 * Ici en stub local, à remplacer par l’intégration réelle.
 */
@Service
public class StorageService {

    /**
     * Chemin de base local (pour stub). Peut être un bucket S3 en prod.
     */
    @Value("${app.storage.base-path:storage}")
    private String basePath;

    @PostConstruct
    public void init() {
        // Ici on pourrait créer le répertoire local, ou initialiser le client S3, etc.
        System.out.println("StorageService initialisé avec basePath=" + basePath);
    }

    /**
     * Stocke un document (bytes) et renvoie la clé interne (UUID).
     * @param data       contenu binaire du fichier
     * @param contentType type MIME
     * @return clé pour récupérer ce document
     */
    public String storeDocument(byte[] data, String contentType) {
        // TODO : écrire dans S3/GCS/Azure
        // Stub : on génère juste un UUID comme "clé"
        return "doc/" + UUID.randomUUID();
    }

    /**
     * Génère une URL présignée pour accéder au document stocké.
     * @param key clé renvoyée par storeDocument()
     * @return URL accessible en GET
     */
    public String getPresignedUrl(String key) {
        // TODO : signer l’URL S3 ou GCS
        // Stub : on retourne un URL simulé
        return "https://static.example.com/" + key + "?expires=" + Duration.ofMinutes(15).toSeconds();
    }

    /**
     * Stocke une photo de profil (bytes) et renvoie la clé interne.
     * @param userId identifiant externe de l’utilisateur
     * @param data   contenu binaire de l’image
     * @return clé pour récupérer la photo
     */
    public String storeProfilePicture(String userId, byte[] data) {
        // TODO : même mécanisme que storeDocument, dans un dossier /profile-pics/
        return "profile/" + userId + "/" + UUID.randomUUID();
    }
}
