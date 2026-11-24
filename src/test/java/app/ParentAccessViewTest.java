package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import entity.Role;
import entity.User;
import interface_adapter.Session;
import interface_adapter.link_parent_to_player.LinkParentToPlayerController;
import interface_adapter.link_parent_to_player.LinkParentToPlayerPresenter;
import interface_adapter.link_parent_to_player.LinkParentToPlayerViewModel;
import interface_adapter.submit_absence_notice.SubmitAbsenceNoticeController;
import interface_adapter.submit_absence_notice.SubmitAbsenceNoticePresenter;
import interface_adapter.submit_absence_notice.SubmitAbsenceNoticeViewModel;
import interface_adapter.view_absence_notices.ViewAbsenceNoticesController;
import interface_adapter.view_absence_notices.ViewAbsenceNoticesPresenter;
import interface_adapter.view_absence_notices.ViewAbsenceNoticesViewModel;
import use_case.link_parent_to_player.LinkParentToPlayerInputData;
import use_case.link_parent_to_player.LinkParentToPlayerOutputData;
import use_case.submit_absence_notice.SubmitAbsenceNoticeInputData;
import use_case.submit_absence_notice.SubmitAbsenceNoticeOutputData;
import use_case.view_absence_notices.ViewAbsenceNoticesOutputData;
import view.AbsenceNoticesView;
import view.ParentLinksView;

/** Verifies the new forms through their buttons and session notifications without a window. */
class ParentAccessViewTest {
    @Test
    void theParentFormSubmitsItsSelectedChildAndEventThenClearsTheSavedDraft() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final Session session = session("parent", Role.PARENT);
            final SubmitAbsenceNoticeViewModel submitModel = new SubmitAbsenceNoticeViewModel();
            final SubmitAbsenceNoticePresenter submitPresenter = new SubmitAbsenceNoticePresenter(submitModel);
            final ViewAbsenceNoticesViewModel readModel = new ViewAbsenceNoticesViewModel();
            final ViewAbsenceNoticesPresenter readPresenter = new ViewAbsenceNoticesPresenter(readModel);
            final AtomicReference<SubmitAbsenceNoticeInputData> request = new AtomicReference<>();
            final AbsenceNoticesView page = new AbsenceNoticesView(
                    new SubmitAbsenceNoticeController(input -> {
                        request.set(input);
                        submitPresenter.prepareSuccessView(new SubmitAbsenceNoticeOutputData("notice", "Saved"));
                    }), submitModel,
                    new ViewAbsenceNoticesController(input -> readPresenter.prepareSuccessView(parentChoices())),
                    readModel, session);

            find(page, "absenceReason", JTextArea.class).setText("Doctor appointment");
            find(page, "absenceSubmit", JButton.class).doClick();

            assertEquals("child", request.get().playerUsername());
            assertEquals("event", request.get().eventId());
            assertEquals("Doctor appointment", request.get().reason());
            assertEquals("", find(page, "absenceReason", JTextArea.class).getText());
            assertEquals("Saved", submitModel.getData().message());
        });
    }

    @Test
    void aFailedSaveRetainsTheDraftAndLoggingOutClearsTheEntireParentPage() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final Session session = session("parent", Role.PARENT);
            final SubmitAbsenceNoticeViewModel submitModel = new SubmitAbsenceNoticeViewModel();
            final SubmitAbsenceNoticePresenter submitPresenter = new SubmitAbsenceNoticePresenter(submitModel);
            final ViewAbsenceNoticesViewModel readModel = new ViewAbsenceNoticesViewModel();
            final ViewAbsenceNoticesPresenter readPresenter = new ViewAbsenceNoticesPresenter(readModel);
            final AbsenceNoticesView page = new AbsenceNoticesView(
                    new SubmitAbsenceNoticeController(input -> submitPresenter.prepareFailView("Disk full")),
                    submitModel,
                    new ViewAbsenceNoticesController(input -> readPresenter.prepareSuccessView(parentChoices())),
                    readModel, session);
            find(page, "absenceReason", JTextArea.class).setText("Private family reason");

            find(page, "absenceSubmit", JButton.class).doClick();

            assertEquals("Private family reason", find(page, "absenceReason", JTextArea.class).getText());
            assertEquals("Disk full", submitModel.getErrorMessage());

            session.clearCurrentUser();

            assertEquals("", find(page, "absenceReason", JTextArea.class).getText());
            assertEquals("", find(page, "absenceFeed", JTextArea.class).getText());
            assertEquals(0, find(page, "absenceChild", JComboBox.class).getItemCount());
            assertEquals(0, find(page, "absenceEvent", JComboBox.class).getItemCount());
            assertFalse(find(page, "absenceSubmit", JButton.class).isEnabled());
            assertNull(readModel.getData());
        });
    }

    @Test
    void theAdminFormLinksAndUnlinksSelectedAccountsAndClearsOnUserChange() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final Session session = session("admin", Role.ADMIN);
            final LinkParentToPlayerViewModel model = new LinkParentToPlayerViewModel();
            final LinkParentToPlayerPresenter presenter = new LinkParentToPlayerPresenter(model);
            final AtomicReference<LinkParentToPlayerInputData> request = new AtomicReference<>();
            final ParentLinksView page = new ParentLinksView(new LinkParentToPlayerController(input -> {
                request.set(input);
                final List<LinkParentToPlayerOutputData.LinkRow> links = "link".equals(input.action())
                        ? List.of(new LinkParentToPlayerOutputData.LinkRow("parent", "Parent", "child", "Child"))
                        : List.of();
                presenter.prepareSuccessView(new LinkParentToPlayerOutputData(
                        List.of(new LinkParentToPlayerOutputData.AccountRow("parent", "Parent")),
                        List.of(new LinkParentToPlayerOutputData.AccountRow("child", "Child")), links, ""));
            }), model, session);

            find(page, "parentLinkSave", JButton.class).doClick();

            assertEquals("link", request.get().action());
            assertEquals("parent", request.get().parentUsername());
            assertEquals("child", request.get().playerUsername());
            assertEquals(1, find(page, "parentLinkList", JList.class).getModel().getSize());

            find(page, "parentLinkList", JList.class).setSelectedIndex(0);
            find(page, "parentLinkDelete", JButton.class).doClick();

            assertEquals("unlink", request.get().action());
            assertTrue(model.getData().links().isEmpty());

            session.setCurrentUser(new User("parent", "pw", "Parent", Role.PARENT));

            assertNull(model.getData());
            assertEquals(0, find(page, "parentLinkParent", JComboBox.class).getItemCount());
            assertFalse(find(page, "parentLinkSave", JButton.class).isEnabled());
        });
    }

    private static Session session(String username, Role role) {
        final Session session = new Session();
        session.setCurrentUser(new User(username, "pw", username, role));
        return session;
    }

    private static ViewAbsenceNoticesOutputData parentChoices() {
        return new ViewAbsenceNoticesOutputData(
                List.of(new ViewAbsenceNoticesOutputData.ChildRow("child", "Child")), "child",
                List.of(new ViewAbsenceNoticesOutputData.EventRow("event", "Raptors", "PRACTICE",
                        LocalDateTime.of(2026, 11, 24, 18, 0), "Gym")), List.of(), true);
    }

    private static <T extends Component> T find(Container root, String name, Class<T> type) {
        for (Component component : root.getComponents()) {
            if (name.equals(component.getName())) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                final T result = findOptional(child, name, type);
                if (result != null) {
                    return result;
                }
            }
        }
        throw new IllegalStateException("Missing form component " + name);
    }

    private static <T extends Component> T findOptional(Container root, String name, Class<T> type) {
        for (Component component : root.getComponents()) {
            if (name.equals(component.getName())) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                final T result = findOptional(child, name, type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
