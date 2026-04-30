package com.itc.funkart.user.service;

import com.itc.funkart.user.entity.OAuthAccount;
import com.itc.funkart.user.entity.Role;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.repository.OAuthAccountRepository;
import org.junit.jupiter.api.BeforeEach;
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
 * <h2>OAuthAccountService — Unit Tests</h2>
 *
 * <p>Verifies the identity linking contract between local {@link User} entities
 * and external OAuth provider records.
 *
 * <p><b>Key corrections from old tests:</b>
 * <ul>
 *   <li>{@code createAccount} takes a {@link User} entity — NOT a {@code Long userId}.
 *       The entity has a {@code User user} relation, not a {@code userId} field.</li>
 *   <li>{@code findOrCreate} takes a {@link User} entity — same reason.</li>
 *   <li>{@link OAuthAccount#getUser()} returns the {@code User} object, so assertions
 *       must navigate through it (e.g. {@code account.getUser().getId()}).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OAuthAccountServiceTest {

    @Mock
    private OAuthAccountRepository oauthAccountRepository;

    @InjectMocks
    private OAuthAccountService oauthAccountService;

    private static final String PROVIDER    = "github";
    private static final String PROVIDER_ID = "gh_12345";

    /** Shared user entity — note we use the full object, not just an ID. */
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(99L)
                .email("alice@example.com")
                .name("Alice")
                .role(Role.ROLE_USER)
                .build();
    }

    /** Builds a realistic OAuthAccount entity backed by the shared user. */
    private OAuthAccount buildAccount() {
        return OAuthAccount.builder()
                .id(1L)
                .user(testUser)           // entity relation, NOT a userId field
                .provider(PROVIDER)
                .providerId(PROVIDER_ID)
                .build();
    }

    // -------------------------------------------------------------------------
    // findByProviderAndProviderId
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findByProviderAndProviderId")
    class LookupTests {

        @Test
        @DisplayName("Returns Optional containing the account when found")
        void returnsAccountWhenFound() {
            OAuthAccount account = buildAccount();
            when(oauthAccountRepository.findByProviderAndProviderId(PROVIDER, PROVIDER_ID))
                    .thenReturn(Optional.of(account));

            Optional<OAuthAccount> result =
                    oauthAccountService.findByProviderAndProviderId(PROVIDER, PROVIDER_ID);

            assertThat(result).isPresent();
            assertThat(result.get().getProviderId()).isEqualTo(PROVIDER_ID);
        }

        @Test
        @DisplayName("Returns empty Optional when account does not exist")
        void returnsEmptyWhenNotFound() {
            when(oauthAccountRepository.findByProviderAndProviderId(any(), any()))
                    .thenReturn(Optional.empty());

            Optional<OAuthAccount> result =
                    oauthAccountService.findByProviderAndProviderId("none", "none");

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // createAccount
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createAccount(User, provider, providerId)")
    class CreationTests {

        @Test
        @DisplayName("Saves a new OAuthAccount and returns it")
        void savesAndReturnsAccount() {
            OAuthAccount saved = buildAccount();
            when(oauthAccountRepository.save(any(OAuthAccount.class))).thenReturn(saved);

            OAuthAccount result = oauthAccountService.createAccount(testUser, PROVIDER, PROVIDER_ID);

            assertThat(result).isNotNull();
            verify(oauthAccountRepository, times(1)).save(any(OAuthAccount.class));
        }

        @Test
        @DisplayName("Saved account is linked to the correct User entity")
        void savedAccountLinkedToUser() {
            OAuthAccount saved = buildAccount();
            when(oauthAccountRepository.save(any(OAuthAccount.class))).thenReturn(saved);

            OAuthAccount result = oauthAccountService.createAccount(testUser, PROVIDER, PROVIDER_ID);

            // Navigate through the User relation — there is no userId field on OAuthAccount
            assertThat(result.getUser().getId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("Saved account has the correct provider and providerId")
        void savedAccountHasCorrectProviderData() {
            OAuthAccount saved = buildAccount();
            when(oauthAccountRepository.save(any(OAuthAccount.class))).thenReturn(saved);

            OAuthAccount result = oauthAccountService.createAccount(testUser, PROVIDER, PROVIDER_ID);

            assertThat(result.getProvider()).isEqualTo(PROVIDER);
            assertThat(result.getProviderId()).isEqualTo(PROVIDER_ID);
        }
    }

    // -------------------------------------------------------------------------
    // findOrCreate
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findOrCreate(User, provider, providerId)")
    class FindOrCreateTests {

        @Test
        @DisplayName("Returns existing account without calling save")
        void returnsExistingWithoutSave() {
            OAuthAccount existing = buildAccount();
            when(oauthAccountRepository.findByProviderAndProviderId(PROVIDER, PROVIDER_ID))
                    .thenReturn(Optional.of(existing));

            OAuthAccount result =
                    oauthAccountService.findOrCreate(testUser, PROVIDER, PROVIDER_ID);

            assertThat(result).isEqualTo(existing);
            verify(oauthAccountRepository, never()).save(any());
        }

        @Test
        @DisplayName("Creates and returns a new account when none exists")
        void createsNewAccountWhenAbsent() {
            when(oauthAccountRepository.findByProviderAndProviderId(PROVIDER, PROVIDER_ID))
                    .thenReturn(Optional.empty());
            OAuthAccount newAccount = buildAccount();
            when(oauthAccountRepository.save(any(OAuthAccount.class))).thenReturn(newAccount);

            OAuthAccount result =
                    oauthAccountService.findOrCreate(testUser, PROVIDER, PROVIDER_ID);

            assertThat(result).isEqualTo(newAccount);
            verify(oauthAccountRepository).save(any(OAuthAccount.class));
        }

        @Test
        @DisplayName("Does not save when account already exists (idempotent)")
        void idempotentWhenAccountExists() {
            when(oauthAccountRepository.findByProviderAndProviderId(PROVIDER, PROVIDER_ID))
                    .thenReturn(Optional.of(buildAccount()));

            oauthAccountService.findOrCreate(testUser, PROVIDER, PROVIDER_ID);
            oauthAccountService.findOrCreate(testUser, PROVIDER, PROVIDER_ID);

            verify(oauthAccountRepository, never()).save(any());
        }
    }
}