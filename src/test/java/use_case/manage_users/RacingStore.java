package use_case.manage_users;

import java.util.List;

import data_access.InMemoryUserDataAccessObject;
import entity.Role;
import entity.User;

/**
 * An in-memory store that misbehaves the way a shared database can: the list may
 * stop being readable once something has been written, another administrator can
 * act in the gap right after a write, and a later write can fail.
 */
class RacingStore extends InMemoryUserDataAccessObject {

    private boolean listFailsAfterWrite;
    private Runnable afterFirstWrite;
    private int failWritesFrom = Integer.MAX_VALUE;
    private int writes;

    /** From now on the list cannot be read once a write has happened. */
    void failTheListAfterAWrite() {
        listFailsAfterWrite = true;
    }

    /**
     * Runs something once, right after the first write, as if another client acted
     * between this client's write and its next read.
     *
     * @param action what the other client does
     */
    void runAfterTheFirstWrite(Runnable action) {
        afterFirstWrite = action;
    }

    /**
     * Makes the nth write and every later one fail.
     *
     * @param number the 1-based number of the first write that fails
     */
    void failWritesFrom(int number) {
        failWritesFrom = number;
    }

    @Override
    public void createUser(User user) {
        beforeWrite();
        super.createUser(user);
        afterWrite();
    }

    @Override
    public void updateRole(String username, Role role) {
        beforeWrite();
        super.updateRole(username, role);
        afterWrite();
    }

    @Override
    public void updateActive(String username, boolean active) {
        beforeWrite();
        super.updateActive(username, active);
        afterWrite();
    }

    @Override
    public List<User> getAllUsers() {
        if (listFailsAfterWrite && writes > 0) {
            throw new IllegalStateException("the list is unreadable");
        }
        return super.getAllUsers();
    }

    private void beforeWrite() {
        if (writes + 1 >= failWritesFrom) {
            throw new IllegalStateException("the store is unwritable");
        }
    }

    private void afterWrite() {
        writes++;
        if (afterFirstWrite != null) {
            final Runnable action = afterFirstWrite;
            afterFirstWrite = null;
            action.run();
        }
    }
}
