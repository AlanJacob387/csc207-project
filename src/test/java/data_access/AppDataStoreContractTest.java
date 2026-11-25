package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import entity.Role;
import entity.User;
import use_case.accounts.DuplicateUsernameException;

/**
 * The account behaviour every store must share, written once and run against the
 * implementations that can hold the whole app: {@link FileStoreContractTest} and
 * {@link HostedStoreContractTest}.
 *
 * Each store used to be tested only against itself, so the two could drift - a
 * save that rewrote a document here and patched one row there - without a test
 * naming the difference. These assertions are the contract a new store has to
 * meet. The in-memory fake is not part of it because it implements only the
 * account interfaces; every interactor test exercises it instead.
 *
 * Every test asks for a fresh store, so no test can lean on another's data.
 */
abstract class AppDataStoreContractTest {

    /**
     * A store with nothing in it.
     *
     * @return a new, empty store of the implementation under test
     */
    protected abstract AppDataStore newStore();

    /** An account as the use cases build one: a hashed credential and a role. */
    private static User account(String username, String displayName, Role role) {
        return new User(username, "pbkdf2$1000$c2FsdA==$aGFzaA==", displayName, role);
    }

    @Test
    void anUnknownAccountIsNull() {
        assertNull(newStore().get("ghost"));
    }

    @Test
    void anAccountIsReadBackAfterItIsCreated() {
        final AppDataStore store = newStore();

        store.createUser(account("p1", "Pat Player", Role.PLAYER));

        final User stored = store.get("p1");
        assertEquals("Pat Player", stored.getDisplayName());
        assertEquals(Role.PLAYER, stored.getRole());
        assertTrue(stored.isActive());
        assertEquals("pbkdf2$1000$c2FsdA==$aGFzaA==", stored.getPassword(),
                "a store must keep the credential exactly as it was written");
    }

    @Test
    void aSecondCreateForTheSameUsernameIsRefused() {
        final AppDataStore store = newStore();
        store.createUser(account("p1", "Pat Player", Role.PLAYER));

        final DuplicateUsernameException thrown = assertThrows(DuplicateUsernameException.class,
                () -> store.createUser(account("p1", "Impostor", Role.COACH)));

        assertEquals("p1", thrown.getUsername());
        assertEquals("Pat Player", store.get("p1").getDisplayName(),
                "a refused create must leave the stored account alone");
    }

    @Test
    void savingAnAccountReplacesTheStoredOne() {
        final AppDataStore store = newStore();
        store.createUser(account("p1", "Pat Player", Role.PLAYER));

        store.save(account("p1", "Pat Captain", Role.COACH));

        assertEquals("Pat Captain", store.get("p1").getDisplayName());
        assertEquals(Role.COACH, store.get("p1").getRole());
    }

    @Test
    void existenceAndTheAccountListAgree() {
        final AppDataStore store = newStore();
        assertFalse(store.existsByName("p1"));

        store.createUser(account("p1", "Pat Player", Role.PLAYER));
        store.createUser(account("c1", "Casey Coach", Role.COACH));

        assertTrue(store.existsByName("p1"));
        assertFalse(store.existsByName("ghost"));
        assertFalse(store.existsByName(""));
        assertEquals(2, store.getAllUsers().size());
    }

    @Test
    void updatingTheRoleChangesOnlyTheRole() {
        final AppDataStore store = newStore();
        store.createUser(account("p1", "Pat Player", Role.PLAYER));

        store.updateRole("p1", Role.COACH);

        final User stored = store.get("p1");
        assertEquals(Role.COACH, stored.getRole());
        assertEquals("Pat Player", stored.getDisplayName());
        assertEquals("pbkdf2$1000$c2FsdA==$aGFzaA==", stored.getPassword(),
                "a role change must not touch the credential");
        assertTrue(stored.isActive());
    }

    @Test
    void updatingTheStatusChangesOnlyTheStatusAndItIsReversible() {
        final AppDataStore store = newStore();
        store.createUser(account("p1", "Pat Player", Role.PLAYER));

        store.updateActive("p1", false);
        assertFalse(store.get("p1").isActive());
        assertEquals(Role.PLAYER, store.get("p1").getRole());
        assertEquals("pbkdf2$1000$c2FsdA==$aGFzaA==", store.get("p1").getPassword());

        store.updateActive("p1", true);
        assertTrue(store.get("p1").isActive());
    }

    @Test
    void updatingAnAccountThatIsNotStoredIsRefused() {
        final AppDataStore store = newStore();

        assertThrows(IllegalStateException.class, () -> store.updateRole("ghost", Role.COACH));
        assertThrows(IllegalStateException.class, () -> store.updateActive("ghost", false));
        assertFalse(store.existsByName("ghost"), "a refused update must not create the account");
    }

    @Test
    void aUsernameWithADotIsFoundAgainByItsExactName() {
        final AppDataStore store = newStore();
        store.createUser(account("pat.smith", "Pat Smith", Role.PLAYER));
        store.createUser(account("pat", "Pat Only", Role.PLAYER));

        assertEquals("Pat Smith", store.get("pat.smith").getDisplayName());
        assertEquals("Pat Only", store.get("pat").getDisplayName());
        assertTrue(store.existsByName("pat.smith"));
    }
}
