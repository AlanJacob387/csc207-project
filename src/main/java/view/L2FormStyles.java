package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Shared, compact Swing presentation helpers for the attendance and RSVP pages. */
final class L2FormStyles {
    static final int FORM_WIDTH = 720;
    private static final Color INK = new Color(28, 32, 38);
    private static final Color MUTED = new Color(100, 110, 123);

    private L2FormStyles() { }

    static JPanel page(String title, String subtitle) {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 20, 12));
        panel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(FORM_WIDTH, Integer.MAX_VALUE));

        final JLabel heading = new JLabel(title);
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        heading.setForeground(INK);
        heading.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(5));

        final JLabel info = new JLabel(subtitle);
        info.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        info.setForeground(MUTED);
        info.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        panel.add(info);
        panel.add(Box.createVerticalStrut(22));
        return panel;
    }

    static void field(JPanel page, String label, JComponent control) {
        final JLabel caption = new JLabel(label);
        caption.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        caption.setForeground(INK);
        caption.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        page.add(caption);
        page.add(Box.createVerticalStrut(6));
        control.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        control.setMaximumSize(new Dimension(FORM_WIDTH, control.getPreferredSize().height));
        page.add(control);
        page.add(Box.createVerticalStrut(16));
    }

    static JPanel leftAligned(JPanel form) {
        final JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Color.WHITE);
        outer.add(form, BorderLayout.NORTH);
        return outer;
    }
}
