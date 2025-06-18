package com.mobility.auth.dto;

/**
 * Transport de la photo de profil :
 * • data     = octets bruts de l'image (BLOB)
 * • mimeType = type MIME de l'image ("image/jpeg", "image/png", ...)
 */
public class ProfilePicture {

    private final byte[] data;
    private final String mimeType;

    public ProfilePicture(byte[] data, String mimeType) {
        this.data = data;
        this.mimeType = mimeType;
    }

    /** Octets bruts de l'image. Peut être null si pas de photo. */
    public byte[] getData() {
        return data;
    }

    /** Type MIME (Content-Type) de l'image. */
    public String getMimeType() {
        return mimeType;
    }
}
