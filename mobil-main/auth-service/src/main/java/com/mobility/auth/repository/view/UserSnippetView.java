// ───────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/auth/repository/view/UserSnippetView.java
// ───────────────────────────────────────────────────────────
package com.mobility.auth.repository.view;

/**
 * Charge uniquement les champs utiles à l’écran « match ».
 * La vue est mappée directement sur le résultat SQL
 * → pas de surcharge mémoire.
 */
public interface UserSnippetView {
    Long   getId();
    String getFirstName();
    String getLastName();
    byte[] getProfilePicture();
    String getProfilePictureMimeType();
    Double getRating();
}
