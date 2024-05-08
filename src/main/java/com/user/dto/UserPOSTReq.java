package com.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPOSTReq extends UserDto {

    @NotBlank(message = "Email must not be empty")
    private String email;

    @NotBlank(message = "Auth0 Id must not be empty")
    private String auth0Id;
};