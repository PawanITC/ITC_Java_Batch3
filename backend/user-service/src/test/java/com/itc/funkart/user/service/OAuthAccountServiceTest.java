package com.itc.funkart.user.service;

import com.itc.funkart.user.entity.OAuthAccount;
import com.itc.funkart.user.repository.OAuthAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OAuthAccountService}.
 * <p>
 * This suite verifies the business logic for external identity mappings using Mockito
 * to isolate the service from the underlying persistence layer.
 * </p>
 *
 * @author Gemini
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class OAuthAccountServiceTest {

    @Mock
    private OAuthAccountRepository oauthAccountRepository;

    @InjectMocks
    private OAuthAccountService oauthAccountService;

    private static final String PROVIDER = "github";
    private static final String PROVIDER_ID = "git_12345";
    private static final Long USER_ID = 99L;

    /**
     * Helper to build a standard OAuthAccount entity.
     */
    private OAuthAccount createMockAccount() {
        return OAuthAccount.builder()
                .id(1L)
                .userId(USER_ID)
                .provider(PROVIDER)
                .providerId(PROVIDER_ID)
                .build();
    }

    @Nested
    @DisplayName("Lookup Operations")
    class LookupTests {

        @Test
        @DisplayName("Should return Optional with account when it exists")
        void findByProviderAndProviderId_Found() {
            // Arrange
            OAuthAccount mockAccount = createMockAccount();
            when(oauthAccountRepository.findByProviderAndProviderId(PROVIDER, PROVIDER_ID))
                    .thenReturn(Optional.of(mockAccount));

            // Act
            Optional<OAuthAccount> result = oauthAccountService.findByProviderAndProviderId(PROVIDER, PROVIDER_ID);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getProviderId()).isEqualTo(PROVIDER_ID);
        }

        @Test
        @DisplayName("Should return empty Optional when account does not exist")
        void findByProviderAndProviderId_NotFound() {
            // Arrange
            when(oauthAccountRepository.findByProviderAndProviderId(any(), any()))
                    .thenReturn(Optional.empty());

            // Act
            Optional<OAuthAccount> result = oauthAccountService.findByProviderAndProviderId("none", "none");

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Creation Operations")
    class CreationTests {

        @Test
        @DisplayName("Should correctly map and save a new OAuth account")
        void createAccount_Success() {
            // Arrange
            OAuthAccount mockAccount = createMockAccount();
            when(oauthAccountRepository.save(any(OAuthAccount.class))).thenReturn(mockAccount);

            // Act
            OAuthAccount result = oauthAccountService.createAccount(USER_ID, PROVIDER, PROVIDER_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            verify(oauthAccountRepository, times(1)).save(any(OAuthAccount.class));
        }
    }

    @Nested
    @DisplayName("Logic: findOrCreate")
    class FindOrCreateTests {

        @Test
        @DisplayName("Should return existing account without calling save")
        void findOrCreate_ReturnsExisting() {
            // Arrange
            OAuthAccount existingAccount = createMockAccount();
            when(oauthAccountRepository.findByProviderAndProviderId(PROVIDER, PROVIDER_ID))
                    .thenReturn(Optional.of(existingAccount));

            // Act
            OAuthAccount result = oauthAccountService.findOrCreate(USER_ID, PROVIDER, PROVIDER_ID);

            // Assert
            assertThat(result).isEqualTo(existingAccount);
            verify(oauthAccountRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create and return new account if none found")
        void findOrCreate_CreatesNew() {
            // Arrange
            when(oauthAccountRepository.findByProviderAndProviderId(PROVIDER, PROVIDER_ID))
                    .thenReturn(Optional.empty());

            OAuthAccount newAccount = createMockAccount();
            when(oauthAccountRepository.save(any(OAuthAccount.class))).thenReturn(newAccount);

            // Act
            OAuthAccount result = oauthAccountService.findOrCreate(USER_ID, PROVIDER, PROVIDER_ID);

            // Assert
            assertThat(result).isEqualTo(newAccount);
            verify(oauthAccountRepository).save(any(OAuthAccount.class));
        }
    }
}