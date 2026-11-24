package entity;

import java.util.ArrayList;
import java.util.List;

/**
 * The roles a user can have on the platform.
 *
 * Whether a role may be chosen when somebody signs up is a rule about accounts,
 * so it lives here rather than in the sign-up page: an administrative account
 * creates and re-roles everyone else, and must come from an administrator or the
 * demo seed.
 */
public enum Role {
    COACH,
    PLAYER,
    PARENT,
    ADMIN;

    /**
     * Says whether an account may choose this role for itself on the sign-up
     * page.
     *
     * @return true for every role except {@link #ADMIN}
     */
    public boolean isSelfAssignable() {
        return this != ADMIN;
    }

    /**
     * The names of the roles the sign-up page may offer, in enum order.
     *
     * Read from the enum values, so a role added later appears at sign-up without
     * touching the page, and only {@link #isSelfAssignable()} decides who is left
     * out.
     *
     * @return the names the sign-up page may show
     */
    public static List<String> selfAssignableNames() {
        final List<String> names = new ArrayList<>();
        for (Role role : values()) {
            if (role.isSelfAssignable()) {
                names.add(role.name());
            }
        }
        return List.copyOf(names);
    }
}
