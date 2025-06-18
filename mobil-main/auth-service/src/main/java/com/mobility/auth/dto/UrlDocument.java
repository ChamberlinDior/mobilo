package com.mobility.auth.dto;

import lombok.*;

@Getter @AllArgsConstructor @Builder
public class UrlDocument {
    private final String type;
    private final String url;
}
