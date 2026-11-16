package use_case.logout;

/**
 * Input boundary for the Log Out use case.
 */
public interface LogoutInputBoundary {

    /** Signs the current user out. */
    void execute();
}