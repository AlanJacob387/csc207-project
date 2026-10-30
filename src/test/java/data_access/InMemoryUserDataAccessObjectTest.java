package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import entity.Role;
import entity.User;

/**
 * Tests the in-memory user fake used by use case tests.
 */
class InMemoryUserDataAccessObjectTest {

    @Test
    void unknownUserIsNullAndDoesNotExist() {
        final InMemoryUserDataAccessObject dao = new InMemoryUserDataAccessObject();

        assertNull(dao.get("ghost"));
        assertFalse(dao.existsByName("ghost"));
    }

    @Test
    void savedUserCanBeReadBack() {
        final InMemoryUserDataAccessObject dao = new InMemoryUserDataAccessObject();
        final User user = new User("p1", "pw", "Pat", Role.PLAYER);

        dao.save(user);

        assertTrue(dao.existsByName("p1"));
        assertEquals("pw", dao.get("p1").getPassword());
        assertEquals("Pat", dao.get("p1").getDisplayName());
    }

    @Test
    void savingTheSameUsernameTwiceOverwrites() {
        final InMemoryUserDataAccessObject dao = new InMemoryUserDataAccessObject();
        dao.save(new User("p1", "pw", "Pat", Role.PLAYER));

        dao.save(new User("p1", "pw2", "Patricia", Role.COACH));

        assertEquals("pw2", dao.get("p1").getPassword());
        assertEquals(Role.COACH, dao.get("p1").getRole());
    }
}
