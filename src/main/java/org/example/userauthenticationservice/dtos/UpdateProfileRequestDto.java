package org.example.userauthenticationservice.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequestDto {
    private Long userId;
    private String email;
    private String password;
}
