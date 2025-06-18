package com.mobility.auth.dto;

import lombok.*;

import java.util.List;

@Getter @AllArgsConstructor @Builder
public class KycDocumentPage {
    private final List<UrlDocument> documents;
}
