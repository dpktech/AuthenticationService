package org.example.userauthenticationservice.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequestDto {
    private String email;
}
