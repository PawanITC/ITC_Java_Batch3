package com.itc.funkart.gateway.service;

import com.itc.funkart.gateway.entity.OAuthAccount;
import com.itc.funkart.gateway.repository.OAuthAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class OAuthAccountService {

    private final OAuthAccountRepository oauthAccountRepository;

    public OAuthAccountService(OAuthAccountRepository oauthAccountRepository) {
        this.oauthAccountRepository = oauthAccountRepository;
    }

    /**
     * Find an OAuthAccount by provider and providerId
     *
     * @param provider   e.g., "github", "google"
     * @param providerId OAuth provider unique ID
     * @return Optional of OAuthAccount
     */
    public Optional<OAuthAccount> findByProviderAndProviderId(String provider, String providerId) {
        return oauthAccountRepository.findByProviderAndProviderId(provider, providerId);
    }

    /**
     * Create a new OAuthAccount for a given userId
     *
     * @param userId     ID from user-service
     * @param provider   e.g., "github", "google"
     * @param providerId provider unique ID
     * @return persisted OAuthAccount
     */
    public OAuthAccount createAccount(Long userId, String provider, String providerId) {
        OAuthAccount account = new OAuthAccount();
        account.setUserId(userId); // Assuming OAuthAccount entity now has userId field
        account.setProvider(provider);
        account.setProviderId(providerId);

        return oauthAccountRepository.save(account);
    }

    /**
     * Find or create an OAuth account
     *
     * @param userId     user-service ID
     * @param provider
     * @param providerId
     * @return OAuthAccount entity
     */
    public OAuthAccount findOrCreate(Long userId, String provider, String providerId) {
        return findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> createAccount(userId, provider, providerId));
    }
}