package com.itc.funkart.user.repository;

import com.itc.funkart.user.entity.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderId(String provider, String providerId);
    Optional<OAuthAccount> findByUserEmail(String email);
}
