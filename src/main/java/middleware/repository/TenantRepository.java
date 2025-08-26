package middleware.repository;

import middleware.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Tenant entity operations.
 * Provides data access methods for multi-tenant functionality.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {

    /**
     * Find a tenant by their unique identifier.
     *
     * @param tenantId The tenant identifier
     * @return Optional containing the tenant if found
     */
    Optional<Tenant> findByTenantId(String tenantId);

    /**
     * Check if a tenant exists by their identifier.
     *
     * @param tenantId The tenant identifier
     * @return true if tenant exists, false otherwise
     */
    boolean existsByTenantId(String tenantId);

    /**
     * Find a tenant by their API key hash.
     *
     * @param apiKeyHash The hashed API key
     * @return Optional containing the tenant if found
     */
    @Query("SELECT t FROM Tenant t JOIN ApiKey ak ON t.tenantId = ak.tenantId WHERE ak.keyHash = :apiKeyHash")
    Optional<Tenant> findByApiKeyHash(@Param("apiKeyHash") String apiKeyHash);

    /**
     * Find all active tenants.
     *
     * @return List of active tenants
     */
    @Query("SELECT t FROM Tenant t WHERE t.active = true")
    java.util.List<Tenant> findAllActive();
}
