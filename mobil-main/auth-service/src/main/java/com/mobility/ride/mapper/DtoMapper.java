// ───────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/mapper/DtoMapper.java
// ───────────────────────────────────────────────────────────
package com.mobility.ride.mapper;

import com.mobility.auth.repository.view.UserSnippetView;
import com.mobility.ride.dto.DriverSnippet;
import com.mobility.ride.dto.RiderSnippet;

/** Conversion manuelle – une seule ligne, inutile de sortir MapStruct. */
public final class DtoMapper {

    private DtoMapper() {}

    public static DriverSnippet toDriverSnippet(UserSnippetView v) {
        return new DriverSnippet(
                v.getId(), v.getFirstName(), v.getLastName(),
                v.getProfilePicture(), v.getProfilePictureMimeType(),
                v.getRating());
    }

    public static RiderSnippet toRiderSnippet(UserSnippetView v) {
        return new RiderSnippet(
                v.getId(), v.getFirstName(), v.getLastName(),
                v.getProfilePicture(), v.getProfilePictureMimeType());
    }
}
