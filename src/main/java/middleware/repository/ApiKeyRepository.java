package middleware.repository;

import middleware.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ApiKey entity operations.
 * Provides data access methods for API key authentication.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    /**
     * Find an API key by its hash.
     *
     * @param keyHash The hashed API key
     * @return Optional containing the API key if found
     */
    Optional<ApiKey> findByKeyHash(String keyHash);

    /**
     * Find an API key by its hash and check if it's active.
     *
     * @param keyHash The hashed API key
     * @return Optional containing the active API key if found
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.keyHash = :keyHash AND ak.active = true")
    Optional<ApiKey> findByKeyHashAndActive(@Param("keyHash") String keyHash);

    /**
     * Find all API keys for a specific tenant.
     *
     * @param tenantId The tenant identifier
     * @return List of API keys for the tenant
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.tenant.tenantId = :tenantId")
    java.util.List<ApiKey> findByTenantId(@Param("tenantId") String tenantId);

    /**
     * Find all active API keys for a specific tenant.
     *
     * @param tenantId The tenant identifier
     * @return List of active API keys for the tenant
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.tenant.tenantId = :tenantId AND ak.active = true")
    java.util.List<ApiKey> findActiveByTenantId(@Param("tenantId") String tenantId);

    /**
     * Check if an API key exists by its hash.
     *
     * @param keyHash The hashed API key
     * @return true if API key exists, false otherwise
     */
    boolean existsByKeyHash(String keyHash);
}
