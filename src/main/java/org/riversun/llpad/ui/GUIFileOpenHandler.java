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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.riversun.llpad.AppDef;
import org.riversun.llpad.R;
import org.riversun.llpad.ui.GUIBuilder.GuiComponent;
import org.riversun.llpad.util.file.TextFileInfoHelper;
import org.riversun.llpad.widget.component.DiagTextArea.FileDropListener;
import org.riversun.string_grabber.StringGrabber;

/**
 * 
 * Helps to open files in two ways
 * <p>
 * - Open file from menu bar using file chooser dialog<br>
 * - Open file from drag and drop<br>
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 *
 */
public class GUIFileOpenHandler {

	private static final Logger LOGGER = Logger.getLogger(GUIFileOpenHandler.class.getName());

	private static final String EXTENSIONS;

	static {

		UIManager.put("FileChooser.openDialogTitleText", R.getString(R.string.FileChooser__openDialogTitleText));
		UIManager.put("FileChooser.cancelButtonText", R.getString(R.string.FileChooser__cancelButtonText));
		UIManager.put("FileChooser.filesOfTypeLabelText", R.getString(R.string.FileChooser__filesOfTypeLabelText));

		final StringGrabber sg = new StringGrabber();
		sg.append("(");
		for (String ext : AppDef.Common.TEXTFILE_EXTENSIONS) {
			sg.append("*.").append(ext).append(",");
		}
		sg.removeTail();
		sg.append(")");

		EXTENSIONS = sg.toString();
	}

	public static interface FileSelectionListener {
		public void onFileSelected(boolean isTextFile, File file);

	}

	private final TextFileInfoHelper mTfHelper = new TextFileInfoHelper();

	private FileSelectionListener mFileSelectionListener;

	public void setFileSelectionListener(FileSelectionListener listener) {
		mFileSelectionListener = listener;
	}

	/**
	 * 
	 * @param views
	 *            holder of GUI components
	 */
	public GUIFileOpenHandler(final GuiComponent views) {

		// set menu open event listener
		views.menuOpen.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				final JFileChooser filechooser = new JFileChooser();

				// In order to decide the order freely, erase all files
				filechooser.setAcceptAllFileFilterUsed(false);
				filechooser.setFileFilter(null);

				// But,at the present , AllFiles is the top.
				filechooser.addChoosableFileFilter(filechooser.getAcceptAllFileFilter());
				filechooser.addChoosableFileFilter(
						new FileNameExtensionFilter(R.getString(R.string.FileChooser__FILTER_TEXTFILES) + EXTENSIONS,
								AppDef.Common.TEXTFILE_EXTENSIONS));

				int selected = filechooser.showOpenDialog(views.frame);

				if (selected == JFileChooser.APPROVE_OPTION) {

					final File file = filechooser.getSelectedFile();
					LOGGER.fine("file=" + file.getAbsolutePath() + " selected via file chooser.");
					doCallback(file);

				} else if (selected == JFileChooser.CANCEL_OPTION) {
					LOGGER.warning("file chooser selection canceled");
				} else if (selected == JFileChooser.ERROR_OPTION) {

					LOGGER.warning("file chooser error occurred");
				}

			}
		});

		// set file drop listener on textArea
		views.textArea.setFileDropListener(new FileDropListener() {
			@Override
			public boolean canFileDrop(List<File> fileList) {
				if (fileList.size() != 1) {
					// Display prohibited mark when the user tries to drop
					// multiple files
					return false;
				} else {

					// (manage file extension if needed.)

					return true;
				}

			}

			@Override
			public void onFileDrop(List<File> fileList) {

				final File file = fileList.get(0);

				LOGGER.fine("file=" + file.getAbsolutePath() + " selected via drop");

				doCallback(file);
			}

		});

	}

	private void doCallback(File file) {

		final String encoding = mTfHelper.detectEncoding(file);

		if (mFileSelectionListener != null) {
			LOGGER.fine("file=" + file.getName() + " encoding=" + encoding);
			mFileSelectionListener.onFileSelected(encoding != null, file);
		}
	}

}
