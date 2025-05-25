package utils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import javax.swing.tree.DefaultTreeCellRenderer;

public class DarkModeManager {
    private static boolean isDarkMode = false;

    public static boolean isDarkMode() {
        return isDarkMode;
    }
    //TODO: 서버에서 저장(DB가 나을 듯)
    /* public static void setDarkMode(boolean darkMode) {
        isDarkMode = darkMode;
    } */

    public static void toggle() {
        isDarkMode = !isDarkMode;
    }

    public static void apply(Component root) {
        Color bg = isDarkMode ? Style.DARK_BACKGROUND_COLOR : Style.BACKGROUND_COLOR;
        Color fg = isDarkMode ? Style.DARK_TEXT_COLOR : Color.BLACK;
        applyToComponent(root, bg, fg);
    }

    private static void applyToComponent(Component comp, Color bg, Color fg) {
        comp.setBackground(bg);
        comp.setForeground(fg);

        if (comp instanceof JPanel && ((JPanel) comp).getBorder() instanceof TitledBorder) {
            TitledBorder border = (TitledBorder) ((JPanel) comp).getBorder();
            border.setTitleColor(fg);
        }

        if (comp instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) comp;
            scroll.setBackground(bg);
            scroll.getViewport().setBackground(bg);
            Component view = scroll.getViewport().getView();
            if (view != null) applyToComponent(view, bg, fg);
        } else if (comp instanceof JSplitPane) {
            JSplitPane split = (JSplitPane) comp;
            applyToComponent(split.getLeftComponent(), bg, fg);
            applyToComponent(split.getRightComponent(), bg, fg);
        } else if (comp instanceof JTree) {
            ((JTree) comp).setCellRenderer(new CustomTreeRenderer());
        } else if (comp instanceof JButton) {
            JButton button = (JButton) comp;
            button.setBackground(isDarkMode ? Style.DARK_BUTTON_COLOR : Style.PRIMARY_COLOR);
            button.setForeground(isDarkMode ? Style.DARK_TEXT_COLOR : Color.WHITE);
        } else if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                applyToComponent(child, bg, fg);
            }
        }
    }

    static class CustomTreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            setBackgroundNonSelectionColor(isDarkMode ? Style.DARK_BACKGROUND_COLOR : Style.BACKGROUND_COLOR);
            setBackgroundSelectionColor(isDarkMode ? new Color(70, 70, 70) : UIManager.getColor("Tree.selectionBackground"));
            setForeground(isDarkMode ? Style.DARK_TEXT_COLOR : Color.BLACK);
            return this;
        }
    }
}
