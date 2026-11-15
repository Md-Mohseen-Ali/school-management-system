package com.schoolmanagement.modules.identity.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "guardian_profiles")
public class GuardianProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "alternate_phone", length = 25)
    private String alternatePhone;

    protected GuardianProfile() {
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAlternatePhone() {
        return alternatePhone;
    }
}