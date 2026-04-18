package com.itc.funkart.user.service;

import com.itc.funkart.user.entity.OAuthAccount;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.repository.OAuthAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service responsible for managing external OAuth identity mappings.
 * Links local {@link com.itc.funkart.user.entity.User} identities with external providers
 * like GitHub or Google.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class OAuthAccountService {

    private final OAuthAccountRepository oauthAccountRepository;

    /**
     * Look up an OAuth mapping based on the provider name and their unique remote ID.
     * * @param provider   The name of the OAuth service (e.g., {@code "github"}).
     * @param providerId The unique identifier string returned by the provider.
     * @return An {@link Optional} containing the account if found.
     */
    public Optional<OAuthAccount> findByProviderAndProviderId(String provider, String providerId) {
        return oauthAccountRepository.findByProviderAndProviderId(provider, providerId);
    }

    /**
     * Persists a new OAuth mapping for an existing user.
     * * @param userId     The local database ID of the user.
     * @param provider   The OAuth provider name.
     * @param providerId The remote provider's unique user ID.
     * @return The saved {@link OAuthAccount} entity.
     */
    public OAuthAccount createAccount(User user, String provider, String providerId) {
        OAuthAccount account = OAuthAccount.builder()
                .user(user)
                .provider(provider)
                .providerId(providerId)
                .build();
        return oauthAccountRepository.save(account);
    }

    /**
     * Idempotent method to retrieve an existing OAuth mapping or create a new one.
     * <p>
     * This ensures that a single local {@link User} can be linked to their
     * external identity (e.g., GitHub) without creating duplicate mappings.
     * </p>
     *
     * @param user       The local {@link User} entity to link if the mapping doesn't exist.
     * @param provider   The name of the OAuth provider (e.g., "github").
     * @param providerId The unique identifier string returned by the provider.
     * @return The existing or newly persisted {@link OAuthAccount}.
     */
    public OAuthAccount findOrCreate(User user, String provider, String providerId) {

        return oauthAccountRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    try {
                        return createAccount(user, provider, providerId);
                    } catch (Exception ex) {
                        // fallback in case of race condition
                        try {
                            return oauthAccountRepository
                                    .findByProviderAndProviderId(provider, providerId)
                                    .orElseThrow(() -> ex);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }
}