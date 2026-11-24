package use_case.manage_users;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Supplier;

import entity.User;

/**
 * Reports a finished account change together with the refreshed list.
 *
 * The change has already been written when this runs, so a failure to read the
 * list back must not be reported as a failure to make the change: the message
 * says the change was saved and only the list is out of date.
 */
final class AccountListReport {

    private AccountListReport() {
    }

    /**
     * Reads the accounts and shows them with the message, or says the list could
     * not be refreshed.
     *
     * @param presenter where the result goes
     * @param done      what was done, as a sentence
     * @param read      reads the accounts
     */
    static void show(ManageUsersOutputBoundary presenter, String done,
                     Supplier<List<User>> read) {
        final List<User> accounts;
        try {
            accounts = read.get();
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView(unrefreshed(done, ex));
            return;
        }
        presenter.prepareSuccessView(ManageUsersOutputData.of(done, accounts));
    }

    /**
     * The sentence for a change that was saved when the list could not be re-read.
     *
     * @param done what was done, as a sentence
     * @param ex   why the list could not be read
     * @return the message
     */
    static String unrefreshed(String done, RuntimeException ex) {
        return done + " It was saved, but the list could not be refreshed: " + ex.getMessage();
    }
}
