package com.itc.funkart.user.service;

import com.itc.funkart.user.entity.OAuthAccount;
import com.itc.funkart.user.repository.OAuthAccountRepository;
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

    public Optional<OAuthAccount> findByProviderAndProviderId(String provider, String providerId) {
        return oauthAccountRepository.findByProviderAndProviderId(provider, providerId);
    }

    public OAuthAccount createAccount(Long userId, String provider, String providerId) {
        OAuthAccount account = new OAuthAccount();
        account.setUserId(userId);
        account.setProvider(provider);
        account.setProviderId(providerId);

        return oauthAccountRepository.save(account);
    }

    public OAuthAccount findOrCreate(Long userId, String provider, String providerId) {
        return findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> createAccount(userId, provider, providerId));
    }
}
