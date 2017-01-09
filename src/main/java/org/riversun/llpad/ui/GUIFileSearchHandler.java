package org.riversun.llpad.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import org.riversun.llpad.ui.GUIBuilder.GuiComponent;

public class GUIFileSearchHandler {
	private static final Logger LOGGER = Logger.getLogger(GUIFileSearchHandler.class.getName());

	/**
	 * 
	 * @param views
	 *            holder of GUI components
	 */
	public GUIFileSearchHandler(final GuiComponent views) {

		views.menuFind.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("find");

			}
		});
	}
}
