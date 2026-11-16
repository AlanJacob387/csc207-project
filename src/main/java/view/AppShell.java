package view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import entity.Role;
import interface_adapter.logout.LogoutController;

/**
 * The signed-in application shell: a dark sidebar with navigation on the
 * left and a CardLayout content area on the right.
 *
 * Pages are added with addPage(name, component); nav buttons are created
 * automatically and highlight the active page.
 *
 * Pages are added with addPage(name, component, roles...), and applyRole(...)
 * rebuilds the sidebar so each role only sees the pages it may use. A page
 * with no roles listed is visible to everyone.
 */
public class AppShell extends JPanel {

    private static final Color SIDEBAR_BG = new Color(16, 24, 44);
    private static final Color SIDEBAR_ITEM = new Color(187, 196, 212);
    private static final Color SIDEBAR_ITEM_ACTIVE_BG = new Color(255, 255, 255, 22);
    private static final Color ACCENT = new Color(96, 148, 255);

    private static final Font FONT_BRAND = new Font("Segoe UI", Font.BOLD, 17);
    private static final Font FONT_NAV = new Font("Segoe UI", Font.PLAIN, 14);

    private final CardLayout contentCards = new CardLayout();
    private final JPanel content = new JPanel(contentCards);
    private final JPanel navList = new JPanel();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, List<Role>> pageRoles = new LinkedHashMap<>();
    private String activePage;

    private LogoutController logoutController;

    public AppShell() {
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);

        content.setBackground(Color.WHITE);
        add(content, BorderLayout.CENTER);
    }

    /**
     * Gives the shell the controller the Log out button should call.
     *
     * @param logoutController the logout controller
     */
    public void setLogoutController(LogoutController logoutController) {
        this.logoutController = logoutController;
    }

    /**
     * Adds a page and a matching sidebar nav item. The first page added is shown.
     *
     * @param name  the page name, also used as the nav label
     * @param page  the page component
     * @param roles the roles allowed to see this page; none means everyone
     */
    public void addPage(String name, Component page, Role... roles) {
        content.add(page, name);
        pageRoles.put(name, List.of(roles));
        final JButton item = navButton(name, () -> showPage(name));
        navButtons.put(name, item);
        navList.add(item);
        navList.add(Box.createVerticalStrut(4));
        if (activePage == null) {
            showPage(name);
        }
    }

    /** Switch the content area to the named page. */
    public void showPage(String name) {
        activePage = name;
        contentCards.show(content, name);
        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            e.getValue().repaint();
        }
    }

    /**
     * Rebuilds the sidebar for a role, so each user only sees their own pages.
     * If the page on screen is no longer allowed, the first allowed page is shown.
     *
     * @param role the signed-in user's role, or null to show every page
     */
    public void applyRole(Role role) {
        navList.removeAll();
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            if (isVisibleFor(entry.getKey(), role)) {
                navList.add(entry.getValue());
                navList.add(Box.createVerticalStrut(4));
            }
        }
        navList.revalidate();
        navList.repaint();

        if (activePage != null && !isVisibleFor(activePage, role)) {
            for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
                if (isVisibleFor(entry.getKey(), role)) {
                    showPage(entry.getKey());
                    break;
                }
            }
        }
    }

    /** Says whether a page is allowed for a role; no roles listed means everyone. */
    private boolean isVisibleFor(String page, Role role) {
        final List<Role> allowed = pageRoles.get(page);
        return role == null || allowed == null || allowed.isEmpty()
                || allowed.contains(role);
    }

    // ------------------------------------------------------------------
    private JPanel buildSidebar() {
        final JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 16, 24, 16));

        final JLabel brand = new JLabel("Team Sports");
        brand.setFont(FONT_BRAND);
        brand.setForeground(Color.WHITE);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(28));

        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setOpaque(false);
        navList.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(navList);

        // Log out sits in the top-left nav group, right under the pages.
        final JButton logout = navButton("Log out", () -> {
            if (logoutController != null) {
                logoutController.execute();
            }
        });
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(logout);

        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JButton navButton(String text, Runnable action) {
        final JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                final boolean active = text.equals(activePage);
                final Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                if (active || getModel().isRollover()) {
                    g2.setColor(SIDEBAR_ITEM_ACTIVE_BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                if (active) {
                    g2.setColor(ACCENT);
                    g2.fillRoundRect(0, 6, 3, getHeight() - 12, 3, 3);
                }
                g2.dispose();
                setForeground(active ? Color.WHITE : SIDEBAR_ITEM);
                super.paintComponent(g);
            }
        };
        b.setFont(FONT_NAV);
        b.setForeground(SIDEBAR_ITEM);
        b.setHorizontalAlignment(JButton.LEFT);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.addActionListener(e -> action.run());
        return b;
    }
}