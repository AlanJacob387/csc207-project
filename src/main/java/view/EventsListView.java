package view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import interface_adapter.browse_events.BrowseEventsController;
import interface_adapter.browse_events.BrowseEventsViewModel;

/**
 * The Events page: every event of the signed-in user's teams as a list, with
 * filters by type and team. Clicking a row opens its details.
 *
 * The filters are only read here and sent to the controller; the use case does
 * the filtering, so this view only ever holds the rows it shows.
 */
public class EventsListView extends JPanel implements PropertyChangeListener {

    private static final Color BG = Color.WHITE;
    private static final Color INK = new Color(28, 32, 38);
    private static final Color INK_MUTED = new Color(130, 138, 148);
    private static final Color ERROR_RED = new Color(217, 48, 37);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 26);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);

    private static final String[] TYPE_LABELS = {"All types", "Practices", "Games"};
    private static final String[] TYPE_VALUES = {null, "PRACTICE", "GAME"};
    private static final String[] COLUMNS = {"Date", "Time", "Event", "Team", "Location", "Status"};
    private static final int STATUS_COLUMN = 5;

    private static final String TABLE_CARD = "table";
    private static final String MESSAGE_CARD = "message";

    private BrowseEventsController controller;
    private BrowseEventsViewModel viewModel;
    private Consumer<String> onOpenEvent;

    private final JComboBox<String> typeBox = new JComboBox<>(TYPE_LABELS);
    private final JComboBox<TeamItem> teamBox = new JComboBox<>();
    // Set while the team box is refilled, so that doesn't fire a new request.
    private boolean updatingFilters;

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    // Event id and cancelled flag for each table row, in the same order.
    private final List<String> rowEventIds = new ArrayList<>();
    private final List<Boolean> rowCancelled = new ArrayList<>();

    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);

    /** Builds the page. It shows nothing until a controller is connected. */
    public EventsListView() {
        setLayout(new BorderLayout(0, 16));
        setBackground(BG);
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        add(buildHeader(), BorderLayout.NORTH);

        table.setFont(FONT_BODY);
        table.setRowHeight(28);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(233, 236, 240));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        table.setDefaultRenderer(Object.class, new RowRenderer());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && onOpenEvent != null) {
                    onOpenEvent.accept(rowEventIds.get(row));
                }
            }
        });

        final JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(233, 236, 240)));
        scroll.getViewport().setBackground(BG);

        messageLabel.setFont(FONT_BODY);
        messageLabel.setForeground(INK_MUTED);

        body.setOpaque(false);
        body.add(scroll, TABLE_CARD);
        body.add(messageLabel, MESSAGE_CARD);
        add(body, BorderLayout.CENTER);

        // Reload whenever the page comes on screen, so changes made elsewhere
        // (a new event, a different user) show up. The team filter goes back
        // to "all", since a different user may not be on the team picked before.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                updatingFilters = true;
                teamBox.setSelectedItem(TeamItem.ALL);
                updatingFilters = false;
                refresh();
            }
        });
    }

    private JPanel buildHeader() {
        final JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        final JLabel title = new JLabel("Events");
        title.setFont(FONT_TITLE);
        title.setForeground(INK);
        header.add(title, BorderLayout.WEST);

        final JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filters.setOpaque(false);
        filters.add(filterLabel("Type"));
        filters.add(typeBox);
        filters.add(filterLabel("Team"));
        filters.add(teamBox);
        header.add(filters, BorderLayout.EAST);

        teamBox.addItem(TeamItem.ALL);
        typeBox.addActionListener(e -> refresh());
        teamBox.addActionListener(e -> {
            if (!updatingFilters) {
                refresh();
            }
        });
        return header;
    }

    /**
     * Connects the page to the Browse Events use case.
     *
     * @param browseController asked for events whenever the page or a filter changes
     * @param browseViewModel  observed for the rows to show
     */
    public void setBrowseEventsController(BrowseEventsController browseController,
                                          BrowseEventsViewModel browseViewModel) {
        this.controller = browseController;
        this.viewModel = browseViewModel;
        browseViewModel.addPropertyChangeListener(this);
    }

    /**
     * Sets what happens when a row is clicked.
     *
     * @param onOpenEvent receives the clicked event's id
     */
    public void setOnOpenEvent(Consumer<String> onOpenEvent) {
        this.onOpenEvent = onOpenEvent;
    }

    /** Reloads the list with the current filters. */
    public void refresh() {
        if (controller == null) {
            return;
        }
        final TeamItem team = (TeamItem) teamBox.getSelectedItem();
        controller.execute(TYPE_VALUES[typeBox.getSelectedIndex()],
                team == null ? null : team.teamId);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!viewModel.isError()) {
            refillTeams(viewModel.getTeams());
        }

        tableModel.setRowCount(0);
        rowEventIds.clear();
        rowCancelled.clear();
        for (BrowseEventsViewModel.Entry entry : viewModel.getEntries()) {
            tableModel.addRow(new Object[] {
                entry.getDate(), entry.getTime(), entry.getTitle(), entry.getTeamName(),
                entry.getLocation(), entry.isCancelled() ? "Cancelled" : "",
            });
            rowEventIds.add(entry.getEventId());
            rowCancelled.add(entry.isCancelled());
        }

        if (viewModel.getEntries().isEmpty()) {
            messageLabel.setText(viewModel.getMessage());
            messageLabel.setForeground(viewModel.isError() ? ERROR_RED : INK_MUTED);
            cards.show(body, MESSAGE_CARD);
        }
        else {
            cards.show(body, TABLE_CARD);
        }
    }

    /** Rebuilds the team filter, keeping the current choice if it still exists. */
    private void refillTeams(List<BrowseEventsViewModel.TeamChoice> teams) {
        final TeamItem selected = (TeamItem) teamBox.getSelectedItem();
        final String selectedId = selected == null ? null : selected.teamId;

        updatingFilters = true;
        teamBox.removeAllItems();
        teamBox.addItem(TeamItem.ALL);
        for (BrowseEventsViewModel.TeamChoice team : teams) {
            final TeamItem item = new TeamItem(team.getTeamId(), team.getName());
            teamBox.addItem(item);
            if (team.getTeamId().equals(selectedId)) {
                teamBox.setSelectedItem(item);
            }
        }
        updatingFilters = false;
    }

    private static JLabel filterLabel(String text) {
        final JLabel label = new JLabel(text);
        label.setFont(FONT_BODY);
        label.setForeground(INK_MUTED);
        return label;
    }

    /** Greys out cancelled rows and shows their status in red. */
    private class RowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            final Component cell = super.getTableCellRendererComponent(
                    tbl, value, isSelected, false, row, column);
            if (!isSelected) {
                final boolean cancelled = row < rowCancelled.size() && rowCancelled.get(row);
                if (column == STATUS_COLUMN && cancelled) {
                    cell.setForeground(ERROR_RED);
                }
                else {
                    cell.setForeground(cancelled ? INK_MUTED : INK);
                }
            }
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return cell;
        }
    }

    /** An entry in the team filter; a null id means all of the user's teams. */
    private static final class TeamItem {
        static final TeamItem ALL = new TeamItem(null, "All my teams");

        private final String teamId;
        private final String label;

        TeamItem(String teamId, String label) {
            this.teamId = teamId;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
