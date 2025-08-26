package middleware.model;

/**
 * Tenant subscription plans.
 * 
 * FREE: Free tier with limited usage
 * SUBSCRIPTION: Fixed monthly subscription
 * TOKEN_BILLED: Pay-per-token usage
 */
public enum TenantPlan {
    FREE,
    SUBSCRIPTION,
    TOKEN_BILLED
}
