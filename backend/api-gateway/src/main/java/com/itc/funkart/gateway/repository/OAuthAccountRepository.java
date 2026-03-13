package com.itc.funkart.gateway.repository;

import com.itc.funkart.gateway.entity.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderId(String provider, String providerId);
}
