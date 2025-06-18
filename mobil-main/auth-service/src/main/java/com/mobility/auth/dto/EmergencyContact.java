package com.mobility.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter @AllArgsConstructor @Builder
public class EmergencyContact {
    @NotBlank @Size(max = 60)
    private final String name;

    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$")
    private final String phone;
}
