package entity;

/**
 * Thrown when account details break {@link AccountRules}.
 *
 * It carries the {@link AccountRules.Refusal} rather than a sentence, so the
 * entity stays free of wording and the use case that shows the failure decides
 * how to say it ({@code use_case.accounts.AccountRefusalText} does that today).
 *
 * It is an {@link IllegalArgumentException} because it reports a bad argument,
 * which is what every caller of {@code UserFactory.create} and
 * {@code User.withNewPassword} is passing.
 */
public class InvalidAccountException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final transient AccountRules.Refusal refusal;

    /**
     * Creates the failure.
     *
     * @param refusal why the details were refused
     */
    public InvalidAccountException(AccountRules.Refusal refusal) {
        super(refusal == null ? "The account details are not acceptable." : refusal.name());
        this.refusal = refusal;
    }

    /**
     * Why the details were refused.
     *
     * @return the refusal
     */
    public AccountRules.Refusal getRefusal() {
        return refusal;
    }
}
