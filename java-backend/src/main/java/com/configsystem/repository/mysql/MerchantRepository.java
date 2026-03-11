package com.configsystem.repository.mysql;

import com.configsystem.entity.mysql.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the merchants table.
 *
 * findAll()    — used by seedAll() to scan all rows
 * existsById() — used by seedAll(); direct SQL execution now handled via EntityManager
 */
@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
}
