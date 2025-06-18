package com.mobility.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

/**
 * DTO « Change password ».
 */
@Builder
public record PasswordChangeRequest(

        @Size(min = 8, max = 72)
        String oldPassword,

        @Size(min = 8, max = 72)
        String newPassword
) {}
