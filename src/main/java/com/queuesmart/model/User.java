package com.queuesmart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String username;
    private String email;
    private String password;
    private Role role;
    private boolean emailVerified;

    public enum Role {
        USER, ADMIN
    }
}
