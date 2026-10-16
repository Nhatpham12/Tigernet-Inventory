package com.tigernet.backend.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "Roles")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // bỏ qua các field của Hibernate proxy khi trả JSON (role là LAZY)
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Role_Name", nullable = false, unique = true, length = 50)
    private String roleName;

    @Column(name = "Description")
    private String description;
}
