package org.example.parser.repository;

import org.example.parser.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByUserId(Long userId);
    void deleteByUserId(Long userId);
    Optional<Product> findByIdAndUserId(Long id, Long userId);
}
