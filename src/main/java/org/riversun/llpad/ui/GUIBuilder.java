/*
 * "LLPAD" You can open a very big document (more than 10GBytes) with ease
 * 
 * Copyright 2016-2017 Tom Misawa, riversun.org@gmail.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"), to deal in the 
 * Software without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the 
 * Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all 
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 *  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR 
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package org.riversun.llpad.ui;

import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.text.Document;

import org.apache.commons.lang3.SystemUtils;
import org.riversun.llpad.AppDef;
import org.riversun.llpad.R;
import org.riversun.llpad.widget.component.DiagTextArea;
import org.riversun.llpad.widget.component.VerticalSeekBar;
import org.riversun.llpad.widget.component.VerticalSeekBar4Common;
import org.riversun.llpad.widget.component.VerticalSeekBar4Windows;
import org.riversun.llpad.widget.helper.EDTHandler;
import org.riversun.llpad.widget.helper.JLinearLayout;
import org.riversun.llpad.widget.helper.JLinearLayout.Orientation;

/**
 * 
 * Build GUI.
 * <p>
 * The role of this class is just to arrange and initialize GUI components and
 * prepare the appearance(LOOK AND FEEL).
 * 
 * <p>
 * The handling of GUI component events is not a role of this class.<br>
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 */
public class GUIBuilder {

	private static final Logger LOGGER = Logger.getLogger(GUIBuilder.class.getName());

	private final EDTHandler mHandler = new EDTHandler();
	private final JFrame mFrame = new JFrame();

	private final DiagTextArea mTextArea = new DiagTextArea();
	private final JMenuItem mMenuItemOpen = new JMenuItem(R.getString(R.string.Window_Menu__OPEN));
	private final JMenuItem mMenuItemFind = new JMenuItem(R.getString(R.string.Window_Menu__FIND));

	private final JScrollPane mScrollpane = new JScrollPane(mTextArea,
			JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

	private TaBuilder mTextAreaBuilder;

	private JLinearLayout mParentLayoutGroup;

	private VerticalSeekBar mVerticalSeekBar;
	private final GuiComponent mComponent = new GuiComponent();
	private File mFile;

	public static class GuiComponent {
		public DiagTextArea textArea;
		public Document textAreaDocument;
		public TaBuilder taBuilder;
		public VerticalSeekBar verticalSeekBar;
		public JFrame frame;
		public JMenuItem menuOpen;
		public JMenuItem menuFind;
	}

	public GUIBuilder() {
		buildGui();
		initGui();
	}

	public GuiComponent getGuiComponent() {
		mComponent.textArea = mTextArea;
		mComponent.textAreaDocument = mTextArea.getDocument();
		mComponent.taBuilder = mTextAreaBuilder;
		mComponent.verticalSeekBar = mVerticalSeekBar;
		mComponent.frame = mFrame;
		mComponent.menuOpen = mMenuItemOpen;
		mComponent.menuFind = mMenuItemFind;
		return mComponent;
	}

	/**
	 * Initialize GUI when opening/reopening new file
	 */
	public void initGui() {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				LOGGER.fine("initialize");

				if (mTextAreaBuilder != null) {
					LOGGER.fine("dispose " + mTextAreaBuilder);
					mTextAreaBuilder.dispose();
					mTextAreaBuilder = null;
				}

				mTextAreaBuilder = new TaBuilder(mTextArea);

				mFrame.setTitle(R.getString(R.string.Window_File__NO_TITLE) + " - " + R.getString(R.string.Window__AppName));

				// clear vertical scroll bar
				mScrollpane.getHorizontalScrollBar().setEnabled(false);
				mVerticalSeekBar.setEnabled(false);
				mVerticalSeekBar.setLongValue(0);
				mVerticalSeekBar.setMaxLongValue(100);
				mVerticalSeekBar.setVisibleAmount(10);

			}
		});

	}

	private void buildGui() {

		mHandler.post(new Runnable() {

			@Override
			public void run() {
				mParentLayoutGroup = new JLinearLayout().setChildOrientation(Orientation.VERTICAL);

				final JLinearLayout middleArea = new JLinearLayout().setChildOrientation(Orientation.HORIZONTAL);

				middleArea.setBackground(R.color.Window_TextArea_Background);
				mScrollpane.setAutoscrolls(false);

				// To whiten the background of the scroll pane
				mScrollpane.getViewport().setOpaque(false);
				mScrollpane.setBorder(null);
				mScrollpane.setBackground(R.color.Window_TextArea_Background);

				if (SystemUtils.IS_OS_WINDOWS) {
					mVerticalSeekBar = new VerticalSeekBar4Windows();
				} else {
					mVerticalSeekBar = new VerticalSeekBar4Common();
				}

				middleArea.addView(mScrollpane, 1.0d);
				middleArea.addView(mVerticalSeekBar, 0.00d);

				mParentLayoutGroup.addView(middleArea, 0.8d);

				final JLinearLayout footerArea = new JLinearLayout().setChildOrientation(Orientation.HORIZONTAL);

				footerArea.setBackground(R.color.Window_Footer);

			}
		});

	}

	/**
	 * Set target file
	 * 
	 * @param file
	 */
	public void setFile(final File file) {

		mFile = file;

		mHandler.post(new Runnable() {

			@Override
			public void run() {
				mFrame.setTitle(file.getName() + " - " + R.getString(R.string.Window__AppName));
				// clear scroll bar
				mScrollpane.getHorizontalScrollBar().setEnabled(true);
				mVerticalSeekBar.setEnabled(true);

			}
		});

	}

	/**
	 * Create and show window
	 */
	public void createWindow() {

		mHandler.post(new Runnable() {

			@Override
			public void run() {
				// show as a window

				final List<Image> icons = new ArrayList<Image>();

				for (String iconFileName : R.image.ICONS) {
					icons.add(new ImageIcon(getClass().getResource(iconFileName)).getImage());
				}

				mFrame.setIconImages(icons);
				mFrame.addWindowListener(new MainWindowListener());
				mFrame.getContentPane().add(mParentLayoutGroup.getAsPanel());
				mFrame.setSize(AppDef.Window.DEFAULT_WINDOW_WIDTH_PX, AppDef.Window.DEFAULT_WINDOW_HEIGHT_PX);

				final JMenuBar menubar = new JMenuBar();

				// File Menu
				{
					// [File]
					final JMenu menuFile = new JMenu(R.getString(R.string.Window_Menu__FILE));
					menuFile.setMnemonic('F');
					menubar.add(menuFile);

					// [File]>[Open]
					mMenuItemOpen.setMnemonic('O');
					mMenuItemOpen.setAccelerator(KeyStroke.getKeyStroke(
							java.awt.event.KeyEvent.VK_O,
							java.awt.Event.CTRL_MASK));

					menuFile.add(mMenuItemOpen);
				}

				// Edit Menu
				{
					// [Edit]
					final JMenu menuEdit = new JMenu(R.getString(R.string.Window_Menu__EDIT));
					menuEdit.setMnemonic('E');
					menubar.add(menuEdit);

					// [Edit]>[Find]
					mMenuItemFind.setMnemonic('F');
					mMenuItemFind.setAccelerator(KeyStroke.getKeyStroke(
							java.awt.event.KeyEvent.VK_F,
							java.awt.Event.CTRL_MASK));
					menuEdit.add(mMenuItemFind);

				}

				mFrame.setJMenuBar(menubar);

				mFrame.setVisible(true);
			}
		});

	}
}
