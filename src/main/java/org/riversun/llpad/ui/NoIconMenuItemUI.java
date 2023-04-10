package org.riversun.llpad.ui;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicMenuItemUI;

/**
 * A custom MenuItemUI implementation that removes the icon space in JMenuItem.
 * This class extends BasicMenuItemUI and overrides the paint() method to avoid
 * painting the icon and its reserved space. The paintText() method is used to
 * draw the text with proper antialiassing and font colors based on the state of
 * the JMenuItem.
 * Note that this implementation completely disables the use of icons for menu
 * items.
 * If icons are required, consider using a different approach.
 * 
 * @author riversun.org@gmail.com
 *
 */
public class NoIconMenuItemUI extends BasicMenuItemUI {

  @Override
  protected String getPropertyPrefix() {
    return "MenuItem";
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    JMenuItem menuItem = (JMenuItem) c;
    paintBackground(g, menuItem, selectionBackground);
    paintText(g, menuItem);
  }

  protected void paintText(Graphics g, JMenuItem menuItem) {
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    FontMetrics fm = menuItem.getFontMetrics(menuItem.getFont());
    String text = menuItem.getText();
    int textX = menuItem.getInsets().left + 6;
    int textY = menuItem.getInsets().top + fm.getAscent();
    g.setFont(menuItem.getFont());

    if (menuItem.isEnabled()) {
      if (menuItem.isArmed() || (menuItem instanceof JMenu && menuItem.isSelected())) {
        g.setColor(selectionForeground);
      } else {
        g.setColor(menuItem.getForeground());
      }
    } else {
      g.setColor(UIManager.getColor("MenuItem.disabledForeground"));
    }

    g.drawString(text, textX, textY);
  }
}