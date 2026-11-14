package view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import interface_adapter.export_schedule.ExportScheduleController;
import interface_adapter.export_schedule.ExportScheduleViewModel;
import interface_adapter.view_schedule.ViewScheduleController;
import interface_adapter.view_schedule.ViewScheduleViewModel;

/**
 * Swing view for the Team Schedule calendar.
 *
 * Two views, toggled with a segmented Month | Week control:
 *  - MONTH: flat month grid with soft event pills.
 *  - WEEK:  hourly grid (7 AM - 10 PM) with events drawn as timed
 *           blocks in each day column; untimed events appear in an
 *           all-day strip under the day headers.
 *
 * Styling: airy header, pill navigation buttons, hairline grid,
 * accent circle on today, tinted blocks (green = practice, amber = game).
 *
 * Events come from the View Schedule use case: whenever the visible range
 * changes, or the page is shown again, the view asks the controller for that
 * range and redraws when the view model reports back.
 */
public class CalendarView extends JPanel implements PropertyChangeListener {

    /** One event as the calendar draws it, built from a view model entry. */
    public static class CalendarEvent {
        public final String eventId;  // null = not clickable
        public final String title;
        public final boolean isGame;
        public final LocalTime start; // null = all-day / untimed
        public final LocalTime end;   // null = default 1h after start

        public CalendarEvent(String eventId, String title, boolean isGame,
                             LocalTime start, LocalTime end) {
            this.eventId = eventId;
            this.title = title;
            this.isGame = isGame;
            this.start = start;
            this.end = end;
        }
    }

    private enum ViewMode { MONTH, WEEK }

    // ------------------------------------------------------------------
    // Palette & typography
    // ------------------------------------------------------------------
    private static final Color BG = Color.WHITE;
    private static final Color INK = new Color(28, 32, 38);
    private static final Color INK_MUTED = new Color(130, 138, 148);
    private static final Color HAIRLINE = new Color(233, 236, 240);
    private static final Color ACCENT = new Color(23, 66, 148);
    private static final Color ACCENT_SOFT = new Color(23, 66, 148, 14);

    private static final Color PRACTICE_BG = new Color(224, 242, 231);
    private static final Color PRACTICE_FG = new Color(22, 101, 52);
    private static final Color GAME_BG = new Color(252, 237, 215);
    private static final Color GAME_FG = new Color(154, 85, 11);

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 26);
    private static final Font FONT_WEEKDAY = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_DAY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_CHIP = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_CHIP_BOLD = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_BUTTON = new Font("Segoe UI", Font.PLAIN, 13);

    // Week view hour range
    private static final int DAY_START_HOUR = 7;   // 7 AM
    private static final int DAY_END_HOUR = 22;    // 10 PM
    private static final int HOUR_HEIGHT = 56;     // px per hour
    private static final int TIME_COL_WIDTH = 64;  // px for the hour labels

    private ViewMode viewMode = ViewMode.MONTH;
    private YearMonth currentMonth = YearMonth.now();
    private LocalDate weekAnchor = LocalDate.now(); // any day inside the shown week

    private final Map<LocalDate, List<CalendarEvent>> events = new HashMap<>();

    private final JLabel monthLabel = new JLabel("", SwingConstants.LEFT);
    private final JLabel statusLabel = new JLabel(" ");
    private final JPanel content = new JPanel(new BorderLayout());
    private JButton monthToggle;
    private JButton weekToggle;

    private ViewScheduleController viewScheduleController;
    private ViewScheduleViewModel viewScheduleViewModel;
    // Range last asked for, so redrawing the same month doesn't re-query.
    private LocalDate loadedFrom;
    private LocalDate loadedTo;
    private Consumer<String> onOpenEvent;
    private ExportScheduleController exportScheduleController;
    private ExportScheduleViewModel exportScheduleViewModel;

    public CalendarView() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        add(buildHeader(), BorderLayout.NORTH);

        content.setOpaque(false);
        add(content, BorderLayout.CENTER);

        // Reload whenever the page comes on screen: after logging in, switching
        // users, or coming back from creating an event.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refresh();
            }
        });

        rebuild();
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Connects the calendar to the View Schedule use case.
     *
     * @param controller asked for events whenever the visible range changes
     * @param viewModel  observed for the events to draw
     */
    public void setViewScheduleController(ViewScheduleController controller,
                                          ViewScheduleViewModel viewModel) {
        this.viewScheduleController = controller;
        this.viewScheduleViewModel = viewModel;
        viewModel.addPropertyChangeListener(this);
    }

    /**
     * Sets what happens when an event is clicked.
     *
     * @param onOpenEvent receives the clicked event's id
     */
    public void setOnOpenEvent(Consumer<String> onOpenEvent) {
        this.onOpenEvent = onOpenEvent;
    }

    /** Reloads the visible range, e.g. after an event was changed elsewhere. */
    public void refresh() {
        loadedFrom = null;
        rebuild();
    }

    private void openEvent(CalendarEvent ev) {
        if (onOpenEvent != null && ev.eventId != null) {
            onOpenEvent.accept(ev.eventId);
        }
    }

    /**
     * Connects the Export button to the Export Schedule use case.
     *
     * @param controller runs the export once a file has been chosen
     * @param viewModel  observed for the result
     */
    public void setExportScheduleController(ExportScheduleController controller,
                                            ExportScheduleViewModel viewModel) {
        this.exportScheduleController = controller;
        this.exportScheduleViewModel = viewModel;
        viewModel.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == exportScheduleViewModel) {
            showExportResult();
            return;
        }
        events.clear();
        for (ViewScheduleViewModel.Entry entry : viewScheduleViewModel.getEntries()) {
            events.computeIfAbsent(entry.getDate(), d -> new ArrayList<>())
                    .add(new CalendarEvent(entry.getEventId(), entry.getTitle(),
                            entry.isGame(), entry.getStartTime(), null));
        }

        final String message = viewScheduleViewModel.getMessage();
        statusLabel.setText(message.isEmpty() ? " " : message);
        statusLabel.setForeground(viewScheduleViewModel.isError()
                ? new Color(217, 48, 37) : INK_MUTED);
        if (viewScheduleViewModel.isError()) {
            // Try again on the next redraw instead of keeping a failed range.
            loadedFrom = null;
        }
        render();
    }

    /** Replace all events shown on the calendar and refresh. */
    public void setEvents(Map<LocalDate, List<CalendarEvent>> newEvents) {
        events.clear();
        events.putAll(newEvents);
        render();
    }

    /** Add a single event to a date and refresh. */
    public void addEvent(LocalDate date, CalendarEvent event) {
        events.computeIfAbsent(date, d -> new ArrayList<>()).add(event);
        render();
    }

    // ------------------------------------------------------------------
    // Header:  Title            [Month|Week]   [<] [Today] [>]
    // ------------------------------------------------------------------
    private JPanel buildHeader() {
        final JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        monthLabel.setFont(FONT_TITLE);
        monthLabel.setForeground(INK);
        header.add(monthLabel, BorderLayout.WEST);

        final JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);

        monthToggle = pillButton("Month", () -> switchView(ViewMode.MONTH));
        weekToggle = pillButton("Week", () -> switchView(ViewMode.WEEK));
        controls.add(monthToggle);
        controls.add(weekToggle);
        controls.add(Box.createHorizontalStrut(10));
        controls.add(pillButton("‹", () -> navigate(-1)));
        controls.add(pillButton("Today", () -> {
            currentMonth = YearMonth.now();
            weekAnchor = LocalDate.now();
            rebuild();
        }));
        controls.add(pillButton("›", () -> navigate(1)));
        controls.add(Box.createHorizontalStrut(10));
        controls.add(pillButton("Export", this::onExport));

        header.add(controls, BorderLayout.EAST);

        statusLabel.setFont(FONT_DAY);
        statusLabel.setForeground(INK_MUTED);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        header.add(statusLabel, BorderLayout.SOUTH);
        return header;
    }

    private void switchView(ViewMode mode) {
        viewMode = mode;
        rebuild();
    }

    /**
     * Asks where to save the schedule, then hands the path to the Export
     * Schedule use case. The export covers the user's whole schedule, not just
     * what is on screen.
     */
    private void onExport() {
        if (exportScheduleController == null) {
            return;
        }

        final JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Schedule");
        chooser.setFileFilter(new FileNameExtensionFilter("iCalendar file (*.ics)", "ics"));
        chooser.setSelectedFile(new File("team-schedule.ics"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return; // user cancelled
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".ics")) {
            file = new File(file.getParentFile(), file.getName() + ".ics");
        }
        if (file.exists()) {
            final int overwrite = JOptionPane.showConfirmDialog(this,
                    file.getName() + " already exists. Overwrite?",
                    "Export Schedule", JOptionPane.YES_NO_OPTION);
            if (overwrite != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // The result comes back through the export view model.
        exportScheduleController.execute(file.getAbsolutePath());
    }

    /** Shows the outcome of an export in a dialog that matches its kind. */
    private void showExportResult() {
        final int messageType;
        switch (exportScheduleViewModel.getStatus()) {
            case ERROR:
                messageType = JOptionPane.ERROR_MESSAGE;
                break;
            case SUCCESS:
            case INFO:
            default:
                messageType = JOptionPane.INFORMATION_MESSAGE;
                break;
        }
        JOptionPane.showMessageDialog(this, exportScheduleViewModel.getMessage(),
                "Export Schedule", messageType);
    }

    private void navigate(int delta) {
        if (viewMode == ViewMode.MONTH) {
            currentMonth = currentMonth.plusMonths(delta);
        } else {
            weekAnchor = weekAnchor.plusWeeks(delta);
        }
        rebuild();
    }

    /** Flat, rounded, bordered button with hover; selected state = accent fill. */
    private JButton pillButton(String text, Runnable action) {
        final JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                final Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                final boolean selected = isToggleSelected(this);
                if (selected) {
                    g2.setColor(ACCENT);
                } else {
                    g2.setColor(getModel().isRollover() ? ACCENT_SOFT : BG);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                if (!selected) {
                    g2.setColor(HAIRLINE);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                }
                g2.dispose();
                setForeground(selected ? Color.WHITE : INK);
                super.paintComponent(g);
            }
        };
        b.setFont(FONT_BUTTON);
        b.setForeground(INK);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    private boolean isToggleSelected(JButton b) {
        return (b == monthToggle && viewMode == ViewMode.MONTH)
                || (b == weekToggle && viewMode == ViewMode.WEEK);
    }

    // ------------------------------------------------------------------
    // Rebuild current view
    // ------------------------------------------------------------------

    /**
     * Loads events for the visible range if it changed, then draws. When a load
     * is sent, the view model update triggers the draw instead.
     */
    private void rebuild() {
        if (!loadVisibleRange()) {
            render();
        }
    }

    /**
     * Asks the controller for the dates on screen, unless they're already loaded.
     *
     * @return true if a request was sent
     */
    private boolean loadVisibleRange() {
        if (viewScheduleController == null) {
            return false;
        }
        final LocalDate from;
        final LocalDate to;
        if (viewMode == ViewMode.MONTH) {
            // The grid always shows 6 full weeks, including spill-over days.
            from = startOfWeek(currentMonth.atDay(1));
            to = from.plusDays(41);
        } else {
            from = startOfWeek(weekAnchor);
            to = from.plusDays(6);
        }
        if (from.equals(loadedFrom) && to.equals(loadedTo)) {
            return false;
        }
        loadedFrom = from;
        loadedTo = to;
        viewScheduleController.execute(from, to);
        return true;
    }

    /** Redraws the current month or week from the events already loaded. */
    private void render() {
        content.removeAll();
        if (viewMode == ViewMode.MONTH) {
            monthLabel.setText(
                    currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                            + " " + currentMonth.getYear());
            content.add(buildMonthGrid(), BorderLayout.CENTER);
        } else {
            final LocalDate weekStart = startOfWeek(weekAnchor);
            final LocalDate weekEnd = weekStart.plusDays(6);
            monthLabel.setText(formatWeekTitle(weekStart, weekEnd));
            content.add(buildWeekView(weekStart), BorderLayout.CENTER);
        }
        content.revalidate();
        content.repaint();
    }

    private static LocalDate startOfWeek(LocalDate date) {
        LocalDate d = date;
        while (d.getDayOfWeek() != DayOfWeek.SUNDAY) {
            d = d.minusDays(1);
        }
        return d;
    }

    private static String formatWeekTitle(LocalDate start, LocalDate end) {
        final String s = start.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault())
                + " " + start.getDayOfMonth();
        final String e = end.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault())
                + " " + end.getDayOfMonth();
        return s + " – " + e + ", " + end.getYear();
    }

    // ==================================================================
    // MONTH VIEW
    // ==================================================================
    private JPanel buildMonthGrid() {
        final JPanel grid = new JPanel(new GridLayout(0, 7, 0, 0));
        grid.setBackground(BG);
        grid.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 0, HAIRLINE));

        final DayOfWeek[] week = {
                DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};
        for (DayOfWeek dow : week) {
            final JLabel label = new JLabel(
                    dow.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            .toUpperCase(Locale.getDefault()),
                    SwingConstants.CENTER);
            label.setFont(FONT_WEEKDAY);
            label.setForeground(INK_MUTED);
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, HAIRLINE),
                    BorderFactory.createEmptyBorder(10, 0, 10, 0)));
            grid.add(label);
        }

        LocalDate cursor = startOfWeek(currentMonth.atDay(1));
        for (int i = 0; i < 42; i++) {
            grid.add(buildDayCell(cursor));
            cursor = cursor.plusDays(1);
        }
        return grid;
    }

    private JPanel buildDayCell(LocalDate date) {
        final boolean inMonth = YearMonth.from(date).equals(currentMonth);
        final boolean isToday = date.equals(LocalDate.now());

        final JPanel cell = new JPanel();
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setBackground(BG);
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, HAIRLINE),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        final JLabel dayNumber = new JLabel(String.valueOf(date.getDayOfMonth()),
                SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                if (isToday) {
                    final Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(ACCENT);
                    final int d = Math.min(getWidth(), getHeight());
                    g2.fillOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        dayNumber.setFont(isToday ? FONT_DAY.deriveFont(Font.BOLD) : FONT_DAY);
        dayNumber.setForeground(isToday ? Color.WHITE : inMonth ? INK : INK_MUTED);
        dayNumber.setPreferredSize(new Dimension(26, 26));
        dayNumber.setMaximumSize(new Dimension(26, 26));
        dayNumber.setAlignmentX(Component.LEFT_ALIGNMENT);
        cell.add(dayNumber);

        final List<CalendarEvent> dayEvents = events.get(date);
        if (dayEvents != null) {
            cell.add(Box.createVerticalStrut(6));
            for (CalendarEvent ev : dayEvents) {
                cell.add(eventPill(ev));
                cell.add(Box.createVerticalStrut(3));
            }
        }

        // Pills handle their own clicks. A click on the rest of the cell only
        // opens something when there is no doubt which event was meant.
        if (dayEvents != null && dayEvents.size() == 1) {
            cell.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    openEvent(dayEvents.get(0));
                }
            });
        }
        return cell;
    }

    /** Soft tinted rounded pill for an event (month view / all-day strip). */
    private JLabel eventPill(CalendarEvent ev) {
        final Color bg = ev.isGame ? GAME_BG : PRACTICE_BG;
        final Color fg = ev.isGame ? GAME_FG : PRACTICE_FG;

        final JLabel pill = new JLabel(ev.title) {
            @Override
            protected void paintComponent(Graphics g) {
                final Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(FONT_CHIP);
        pill.setForeground(fg);
        pill.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        pill.setAlignmentX(Component.LEFT_ALIGNMENT);
        pill.setToolTipText(ev.title);
        if (ev.eventId != null) {
            pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pill.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    openEvent(ev);
                }
            });
        }
        return pill;
    }

    // ==================================================================
    // WEEK VIEW (hourly)
    // ==================================================================
    private JPanel buildWeekView(LocalDate weekStart) {
        final JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        wrapper.add(buildWeekHeader(weekStart), BorderLayout.NORTH);

        final WeekGridPanel grid = new WeekGridPanel(weekStart);
        final JScrollPane scroll = new JScrollPane(grid,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, HAIRLINE));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        wrapper.add(scroll, BorderLayout.CENTER);

        // Scroll so the morning hours are visible but not wasted space.
        SwingUtilities.invokeLater(() ->
                scroll.getVerticalScrollBar().setValue(HOUR_HEIGHT));
        return wrapper;
    }

    /** Day-name headers + all-day pills, aligned with the hour grid columns. */
    private JPanel buildWeekHeader(LocalDate weekStart) {
        final JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        // Spacer over the time column
        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(TIME_COL_WIDTH, 1));
        header.add(spacer, BorderLayout.WEST);

        final JPanel days = new JPanel(new GridLayout(1, 7, 0, 0));
        days.setOpaque(false);
        for (int i = 0; i < 7; i++) {
            final LocalDate date = weekStart.plusDays(i);
            final boolean isToday = date.equals(LocalDate.now());

            final JPanel col = new JPanel();
            col.setOpaque(false);
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
            col.setBorder(BorderFactory.createEmptyBorder(0, 6, 8, 6));

            final JLabel dow = new JLabel(date.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    .toUpperCase(Locale.getDefault()));
            dow.setFont(FONT_WEEKDAY);
            dow.setForeground(isToday ? ACCENT : INK_MUTED);
            dow.setAlignmentX(Component.CENTER_ALIGNMENT);

            final JLabel num = new JLabel(String.valueOf(date.getDayOfMonth()),
                    SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    if (isToday) {
                        final Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(ACCENT);
                        final int d = Math.min(getWidth(), getHeight());
                        g2.fillOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d);
                        g2.dispose();
                    }
                    super.paintComponent(g);
                }
            };
            num.setFont(new Font("Segoe UI", isToday ? Font.BOLD : Font.PLAIN, 18));
            num.setForeground(isToday ? Color.WHITE : INK);
            num.setPreferredSize(new Dimension(32, 32));
            num.setMaximumSize(new Dimension(32, 32));
            num.setAlignmentX(Component.CENTER_ALIGNMENT);

            col.add(dow);
            col.add(Box.createVerticalStrut(4));
            col.add(num);

            // All-day / untimed events as pills under the date
            final List<CalendarEvent> dayEvents = events.get(date);
            if (dayEvents != null) {
                for (CalendarEvent ev : dayEvents) {
                    if (ev.start == null) {
                        col.add(Box.createVerticalStrut(4));
                        final JLabel pill = eventPill(ev);
                        pill.setAlignmentX(Component.CENTER_ALIGNMENT);
                        col.add(pill);
                    }
                }
            }
            days.add(col);
        }
        header.add(days, BorderLayout.CENTER);
        return header;
    }

    /** Custom-painted hourly grid with timed event blocks. */
    private class WeekGridPanel extends JPanel {
        private final LocalDate weekStart;
        // Where each block was drawn on the last paint, for click hit-testing.
        private final List<Rectangle> blockBounds = new ArrayList<>();
        private final List<CalendarEvent> blockEvents = new ArrayList<>();

        WeekGridPanel(LocalDate weekStart) {
            this.weekStart = weekStart;
            setBackground(BG);
            final int height = (DAY_END_HOUR - DAY_START_HOUR) * HOUR_HEIGHT;
            setPreferredSize(new Dimension(0, height));

            final MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    final CalendarEvent hit = eventAt(e.getX(), e.getY());
                    if (hit != null) {
                        openEvent(hit);
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    setCursor(eventAt(e.getX(), e.getY()) != null
                            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            : Cursor.getDefaultCursor());
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        private CalendarEvent eventAt(int x, int y) {
            // Search backwards so the block painted on top wins.
            for (int i = blockBounds.size() - 1; i >= 0; i--) {
                if (blockBounds.get(i).contains(x, y) && blockEvents.get(i).eventId != null) {
                    return blockEvents.get(i);
                }
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            blockBounds.clear();
            blockEvents.clear();
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            final int w = getWidth();
            final int h = getHeight();
            final double colW = (w - TIME_COL_WIDTH) / 7.0;

            // Hour lines + labels
            g2.setFont(FONT_CHIP);
            for (int hour = DAY_START_HOUR; hour <= DAY_END_HOUR; hour++) {
                final int y = (hour - DAY_START_HOUR) * HOUR_HEIGHT;
                g2.setColor(HAIRLINE);
                g2.drawLine(TIME_COL_WIDTH, y, w, y);
                g2.setColor(INK_MUTED);
                final String label = formatHour(hour);
                final FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, TIME_COL_WIDTH - fm.stringWidth(label) - 10,
                        y + fm.getAscent() + 2);
            }

            // Day column separators; soft wash behind today
            for (int i = 0; i <= 7; i++) {
                final int x = TIME_COL_WIDTH + (int) Math.round(i * colW);
                g2.setColor(HAIRLINE);
                g2.drawLine(x, 0, x, h);
            }
            for (int i = 0; i < 7; i++) {
                if (weekStart.plusDays(i).equals(LocalDate.now())) {
                    final int x = TIME_COL_WIDTH + (int) Math.round(i * colW);
                    g2.setColor(ACCENT_SOFT);
                    g2.fillRect(x, 0, (int) colW, h);
                }
            }

            // Timed event blocks
            for (int i = 0; i < 7; i++) {
                final LocalDate date = weekStart.plusDays(i);
                final List<CalendarEvent> dayEvents = events.get(date);
                if (dayEvents == null) {
                    continue;
                }
                final int colX = TIME_COL_WIDTH + (int) Math.round(i * colW);
                for (CalendarEvent ev : dayEvents) {
                    if (ev.start == null) {
                        continue; // shown in the all-day strip
                    }
                    paintEventBlock(g2, ev, colX, (int) colW);
                }
            }

            // Red "now" line on today
            final LocalDate today = LocalDate.now();
            if (!today.isBefore(weekStart) && !today.isAfter(weekStart.plusDays(6))) {
                final LocalTime now = LocalTime.now();
                if (now.getHour() >= DAY_START_HOUR && now.getHour() < DAY_END_HOUR) {
                    final int i = (int) java.time.temporal.ChronoUnit.DAYS
                            .between(weekStart, today);
                    final int x = TIME_COL_WIDTH + (int) Math.round(i * colW);
                    final int y = timeToY(now);
                    g2.setColor(new Color(217, 48, 37));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(x, y, x + (int) colW, y);
                    g2.fillOval(x - 4, y - 4, 8, 8);
                }
            }
            g2.dispose();
        }

        private void paintEventBlock(Graphics2D g2, CalendarEvent ev, int colX, int colW) {
            final LocalTime start = ev.start;
            final LocalTime end = ev.end != null ? ev.end : start.plusHours(1);

            final int y1 = timeToY(start);
            final int y2 = Math.max(timeToY(end), y1 + 22);
            final int x = colX + 4;
            final int wdt = colW - 8;

            final Color bg = ev.isGame ? GAME_BG : PRACTICE_BG;
            final Color fg = ev.isGame ? GAME_FG : PRACTICE_FG;

            blockBounds.add(new Rectangle(x, y1 + 2, wdt, y2 - y1 - 4));
            blockEvents.add(ev);

            g2.setColor(bg);
            g2.fillRoundRect(x, y1 + 2, wdt, y2 - y1 - 4, 10, 10);
            g2.setColor(fg);
            g2.fillRoundRect(x, y1 + 2, 3, y2 - y1 - 4, 3, 3); // left accent bar

            // Title + time, clipped to the block
            final java.awt.Shape oldClip = g2.getClip();
            g2.clipRect(x, y1 + 2, wdt, y2 - y1 - 4);
            g2.setColor(fg);
            g2.setFont(FONT_CHIP_BOLD);
            g2.drawString(ev.title, x + 10, y1 + 16);
            g2.setFont(FONT_CHIP);
            g2.drawString(formatRange(start, end), x + 10, y1 + 30);
            g2.setClip(oldClip);
        }

        private int timeToY(LocalTime t) {
            final double hours = (t.getHour() - DAY_START_HOUR) + t.getMinute() / 60.0;
            return (int) Math.round(hours * HOUR_HEIGHT);
        }
    }

    private static String formatHour(int hour) {
        if (hour == 12) {
            return "12 PM";
        }
        return hour < 12 ? hour + " AM" : (hour - 12) + " PM";
    }

    private static String formatRange(LocalTime start, LocalTime end) {
        return shortTime(start) + " – " + shortTime(end);
    }

    private static String shortTime(LocalTime t) {
        final int h12 = t.getHour() % 12 == 0 ? 12 : t.getHour() % 12;
        final String ampm = t.getHour() < 12 ? "AM" : "PM";
        return t.getMinute() == 0 ? h12 + " " + ampm
                : h12 + ":" + String.format("%02d", t.getMinute()) + " " + ampm;
    }

    /** Standalone preview — run this class directly to see the calendar. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            final JFrame frame = new JFrame("Team Schedule — Calendar");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new CalendarView());
            frame.setMinimumSize(new Dimension(1000, 700));
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}