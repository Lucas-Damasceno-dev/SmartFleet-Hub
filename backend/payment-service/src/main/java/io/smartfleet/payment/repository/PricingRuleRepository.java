package io.smartfleet.payment.repository;

import io.smartfleet.payment.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for PricingRule entity.
 */
@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    Optional<PricingRule> findByVehicleTypeIdAndActiveTrue(Integer vehicleTypeId);
    
    Optional<PricingRule> findFirstByActiveTrue();
}
