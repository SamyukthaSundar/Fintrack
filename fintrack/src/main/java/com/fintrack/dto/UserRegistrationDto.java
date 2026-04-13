package com.fintrack.dto;

import jakarta.validation.constraints.*;

public class UserRegistrationDto {

    @NotBlank @Size(min = 3, max = 50)
    private String username;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6, max = 100)
    private String password;

    @NotBlank @Size(max = 100)
    private String fullName;

    public UserRegistrationDto() {}

    public String getUsername()  { return username; }
    public String getEmail()     { return email; }
    public String getPassword()  { return password; }
    public String getFullName()  { return fullName; }

    public void setUsername(String v)  { this.username = v; }
    public void setEmail(String v)     { this.email = v; }
    public void setPassword(String v)  { this.password = v; }
    public void setFullName(String v)  { this.fullName = v; }
}
