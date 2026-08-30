package com.servicekit.data.repository;

import com.servicekit.common.exception.NotFoundException;
import com.servicekit.data.TestApplication;
import com.servicekit.data.entity.TestProductEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ContextConfiguration(classes = TestApplication.class)
class BaseRepositoryTest {

    @Autowired
    private TestProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    private TestProductEntity product1;
    private TestProductEntity product2;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        product1 = productRepository.save(new TestProductEntity("Laptop", 1200.0));
        product2 = productRepository.save(new TestProductEntity("Mouse", 25.0));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Kiểm tra UUID: ID được sinh tự động dạng UUID")
    void testUuidGeneration() {
        TestProductEntity product = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(product.getId()).isNotNull();
        assertThat(product.getId()).isInstanceOf(UUID.class);
    }

    @Test
    @DisplayName("Kiểm tra Audit Lifecycle: Tự động gán createdAt, updatedAt khi tạo mới")
    void testAuditLifecycle_OnCreate() {
        TestProductEntity product = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(product.getCreatedAt()).isNotNull();
        assertThat(product.getUpdatedAt()).isNotNull();
        assertThat(product.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("Kiểm tra Soft Delete: deleteById() tự động chuyển is_deleted = true và ẩn khỏi findById")
    void testDeleteById_ShouldSoftDelete() {
        UUID id = product1.getId();
        productRepository.deleteById(id);
        entityManager.flush();
        entityManager.clear();

        Optional<TestProductEntity> found = productRepository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Kiểm tra Soft Delete: deleteAll(entities) tự động xóa mềm toàn bộ")
    void testDeleteAllEntities_ShouldSoftDelete() {
        List<TestProductEntity> all = productRepository.findAll();
        productRepository.deleteAll(all);
        entityManager.flush();
        entityManager.clear();

        assertThat(productRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Kiểm tra Soft Delete: deleteAllById(ids) tự động xóa mềm toàn bộ")
    void testDeleteAllById_ShouldSoftDelete() {
        productRepository.deleteAllById(List.of(product1.getId(), product2.getId()));
        entityManager.flush();
        entityManager.clear();

        assertThat(productRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Kiểm tra Bulk Soft Delete: softDeleteAllByIds(ids)")
    void testSoftDeleteAllByIds() {
        productRepository.softDeleteAllByIds(List.of(product1.getId(), product2.getId()));
        entityManager.flush();
        entityManager.clear();

        assertThat(productRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Kiểm tra Restore: restoreById() phục hồi lại bản ghi đã bị xóa mềm")
    void testRestoreById() {
        UUID id = product1.getId();
        productRepository.deleteById(id);
        entityManager.flush();
        entityManager.clear();
        assertThat(productRepository.findById(id)).isEmpty();

        productRepository.restoreById(id);
        entityManager.flush();
        entityManager.clear();

        Optional<TestProductEntity> restored = productRepository.findById(id);
        assertThat(restored).isPresent();
        assertThat(restored.get().getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("Kiểm tra findByIdOrThrow: Ném NotFoundException khi không tìm thấy ID")
    void testFindByIdOrThrow_ThrowsNotFoundException() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> productRepository.findByIdOrThrow(randomId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(randomId.toString());
    }
}
