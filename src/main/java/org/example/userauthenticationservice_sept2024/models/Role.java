package org.example.userauthenticationservice_sept2024.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@JsonDeserialize(as = Role.class)
@Setter
@Getter
public class Role extends BaseModel {
    private String name;
}
