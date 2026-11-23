package app;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Application entry point.
 *
 * Every piece of wiring lives in {@link AppBuilder}: this class only says which
 * views and use cases the application has, builds it, and shows the window.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            final AppBuilder appBuilder = new AppBuilder();

            final JFrame frame = configure(appBuilder).build();
            frame.setVisible(true);
        });
    }

    /** Uses one wiring path for normal startup and isolated application verification. */
    public static AppBuilder configure(AppBuilder appBuilder) {
        return appBuilder
                    .addLoginView()
                    .addSignupView()
                    .addAppShell()
                    .addLoginUseCase()
                    .addSignupUseCase()
                    .addLogoutUseCase()
                    .addCreateEventUseCase()
                    .addViewScheduleUseCase()
                    .addViewEventDetailsUseCase()
                    .addEditEventUseCase()
                    .addCancelEventUseCase()
                    .addBrowseEventsUseCase()
                    .addExportScheduleUseCase()
                    .addChangePasswordView()
                    .addChangePasswordUseCase()
                    .addAnnouncementUseCases()
                    .addNotificationUseCase()
                    .addAbsenceNoticeUseCases()
                    .addParentLinksUseCase()
                    .addWeatherUseCases();
    }
}
