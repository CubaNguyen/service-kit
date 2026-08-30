package com.servicekit.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "test_products")
public class TestProductEntity extends VersionedSoftDeletableEntity {

    private String name;
    private Double price;

    public TestProductEntity(String name, Double price) {
        this.name = name;
        this.price = price;
    }
}
