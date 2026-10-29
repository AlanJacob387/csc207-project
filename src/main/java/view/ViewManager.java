package view;

import java.awt.CardLayout;
import java.awt.Component;

import javax.swing.JPanel;

/**
 * Holds all pages of the application in a CardLayout and switches
 * between them by name.
 *
 * TODO (Clean Architecture): later, back this with a ViewManagerModel
 * in interface_adapter that presenters update, so use cases can trigger
 * navigation (e.g. successful login -> "app"). For now views call
 * show(...) directly via callbacks.
 */
public class ViewManager extends JPanel {

    // Page names used across the app
    public static final String LOGIN = "login";
    public static final String APP = "app";

    private final CardLayout cards = new CardLayout();

    public ViewManager() {
        setLayout(cards);
    }

    /** Register a page under a unique name. */
    public void register(String name, Component page) {
        add(page, name);
    }

    /** Show the page registered under the given name. */
    public void show(String name) {
        cards.show(this, name);
    }
}