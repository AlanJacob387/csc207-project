package view;

import java.awt.CardLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import interface_adapter.ViewManagerModel;

/**
 * Holds all pages of the application in a CardLayout and switches
 * between them by name.
 *
 * Presenters trigger navigation by setting the state of a ViewManagerModel
 * and firing a change; this class listens and swaps the visible card. Views
 * may also call show(...) directly, which is handy for previews.
 */
public class ViewManager extends JPanel implements PropertyChangeListener {

    // Page names, shared with the presenters that trigger navigation
    public static final String LOGIN = ViewManagerModel.LOGIN;
    public static final String SIGNUP = ViewManagerModel.SIGNUP;
    public static final String DATABASE = ViewManagerModel.DATABASE;
    public static final String APP = ViewManagerModel.APP;

    private final CardLayout cards = new CardLayout();
    private final ViewManagerModel viewManagerModel;

    /** Creates a view manager that follows no model, for previews. */
    public ViewManager() {
        this(null);
    }

    /**
     * Creates a view manager that follows a view manager model, so a use case
     * can move the user between pages.
     *
     * @param viewManagerModel holds the name of the page that should be visible
     */
    public ViewManager(ViewManagerModel viewManagerModel) {
        this.viewManagerModel = viewManagerModel;
        setLayout(cards);
        if (viewManagerModel != null) {
            viewManagerModel.addPropertyChangeListener(this);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("state".equals(evt.getPropertyName()) && viewManagerModel != null) {
            show(viewManagerModel.getState());
        }
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