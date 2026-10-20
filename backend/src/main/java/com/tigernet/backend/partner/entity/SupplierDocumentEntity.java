package com.tigernet.backend.partner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(name = "Supplier_Documents")
public class SupplierDocumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Supplier_ID", nullable = false)
    private SupplierEntity supplier;

    @Column(name = "Doc_Type", length = 100)
    private String docType;

    @Column(name = "File_Path", nullable = false)
    private String filePath; // object key trên object storage

    @Column(name = "Uploaded_At", nullable = false)
    private LocalDateTime uploadedAt;
}
