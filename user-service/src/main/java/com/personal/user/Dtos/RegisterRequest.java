package com.personal.user.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterRequest{

    @NotBlank
    String username;
    
    @NotBlank
    @Email
    String email;

    @NotBlank
    String password;

    public RegisterRequest(String username, String email, String password) {
        this.username = username.toLowerCase();
        this.email = email.toLowerCase();
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username.toLowerCase();
    }

    public void setEmail(String email) {
        this.email = email.toLowerCase();
    }
}
