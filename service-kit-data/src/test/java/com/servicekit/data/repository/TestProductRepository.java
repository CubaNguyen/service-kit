package com.servicekit.data.repository;

import com.servicekit.data.entity.TestProductEntity;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TestProductRepository extends BaseRepository<TestProductEntity, UUID> {
}
