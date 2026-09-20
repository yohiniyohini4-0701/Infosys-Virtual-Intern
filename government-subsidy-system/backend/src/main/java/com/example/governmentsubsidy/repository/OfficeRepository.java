package com.example.governmentsubsidy.repository;

import com.example.governmentsubsidy.entity.Office;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeRepository extends JpaRepository<Office, Long> {
    boolean existsByName(String name);
}
