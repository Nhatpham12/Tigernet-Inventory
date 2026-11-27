package com.tigernet.backend.support;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.catalog.entity.ProductGroupEntity;
import com.tigernet.backend.partner.entity.CustomerEntity;
import com.tigernet.backend.partner.entity.PartyEntity;
import com.tigernet.backend.partner.entity.SupplierEntity;
import com.tigernet.backend.partner.enums.PartyType;
import com.tigernet.backend.user.entity.RoleEntity;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.EntityManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dữ liệu mẫu cho integration test chạy trên MySQL thật (test bọc @Transactional nên tự rollback).
 * Mỗi bản ghi gắn {@code tag} riêng (VD "it_" + nanoTime) để không đụng dữ liệu có sẵn.
 */
public record TestData(EntityManager em, String tag) {

    public static TestData of(EntityManager em, String prefix) {
        return new TestData(em, prefix + System.nanoTime());
    }

    public WarehouseEntity warehouse(String suffix) {
        WarehouseEntity w = new WarehouseEntity();
        String code = (tag + suffix);
        w.setWarehouseCode(code.substring(Math.max(0, code.length() - 12)));
        w.setWarehouseName(tag + suffix);
        w.setAddress("test");
        w.setIsActive(true);
        w.setCreatedAt(LocalDateTime.now());
        em.persist(w);
        return w;
    }

    public ProductEntity product(String suffix, boolean serialized) {
        ProductGroupEntity group = new ProductGroupEntity();
        group.setGroupName(tag + suffix);
        em.persist(group);
        ProductEntity p = new ProductEntity();
        p.setProductName(tag + suffix);
        p.setGroup(group);
        p.setUnit("cái");
        p.setDefaultPriceIn(new BigDecimal("100"));
        p.setDefaultPriceOut(new BigDecimal("150"));
        p.setIsSerialized(serialized);
        p.setMinStockLevel(0);
        em.persist(p);
        return p;
    }

    public PartyEntity party(String suffix) {
        PartyEntity party = new PartyEntity();
        party.setPartyType(PartyType.ORGANIZATION);
        party.setFullName(tag + suffix);
        em.persist(party);
        return party;
    }

    public SupplierEntity supplier(String suffix) {
        SupplierEntity s = new SupplierEntity();
        s.setParty(party(suffix));
        em.persist(s);
        return s;
    }

    public CustomerEntity customer(String suffix) {
        CustomerEntity c = new CustomerEntity();
        c.setParty(party(suffix));
        em.persist(c);
        return c;
    }

    /** tạo user với vai trò cho trước và đặt làm người đang gọi API (token giả có userId + scope) */
    public UserEntity loginAs(String roleName) {
        RoleEntity role = em.createQuery("select r from RoleEntity r where r.roleName = :n", RoleEntity.class)
                .setParameter("n", roleName).getSingleResult();
        UserEntity user = new UserEntity();
        user.setUsername((tag + roleName).toLowerCase());
        user.setFullName(tag);
        user.setPasswordHash("x");
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(role);
        em.persist(user);
        em.flush();
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS512")
                .claim("userId", user.getUserId()).claim("scope", roleName).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("SCOPE_" + roleName))));
        return user;
    }
}
