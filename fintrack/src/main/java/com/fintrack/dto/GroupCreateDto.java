package com.fintrack.dto;

import jakarta.validation.constraints.*;

public class GroupCreateDto {

    @NotBlank @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private String currency;

    public GroupCreateDto() {}

    public String getName()        { return name; }
    public String getDescription() { return description; }
    public String getCurrency()    { return currency; }

    public void setName(String v)        { this.name = v; }
    public void setDescription(String v) { this.description = v; }
    public void setCurrency(String v)    { this.currency = v; }
}
