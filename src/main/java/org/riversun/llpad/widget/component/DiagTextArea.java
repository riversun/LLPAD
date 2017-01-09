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
package org.riversun.llpad.widget.component;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;

import org.riversun.llpad.widget.component.JTextAreaHelper.JTextAreaInfo;
import org.riversun.llpad.widget.component.JTextAreaHelper.JTextAreaRowInfo;
import org.riversun.llpad.widget.component.JTextAreaHelper.TextAreaVisibleArea;

/**
 * 
 * Diagnosable textArea for efficient editing and viewing.Fixed a bug in
 * JTextArea by continuously changing text
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 *
 */
@SuppressWarnings("serial")
public class DiagTextArea extends JTextArea {
	private static final Logger LOGGER = Logger.getLogger(DiagTextArea.class.getName());

	/**
	 * Direction of scroll
	 */
	enum EScroll {
		TOP,
		BOTTOM
	}

	public static interface FileDropListener {
		/**
		 * Determine if the file can be dropped
		 * <p>
		 * If returning false here, a prohibited mark is displayed when dropping
		 * enter
		 * 
		 * @param fileList
		 *            files to be dropped
		 * @return
		 */
		public boolean canFileDrop(List<File> fileList);

		/**
		 * Called when a file is dropped
		 * 
		 * @param fileList
		 */
		public void onFileDrop(List<File> fileList);

	}

	public static Highlighter.HighlightPainter mHlPaint = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE);

	public static class HighlightInfo {

		public String word;
		public boolean caseSensitive;

	}

	private final TextAreaVisibleArea m_Ref_VisibleArea = new TextAreaVisibleArea();
	private final JTextAreaHelper mTextAreaUtil = new JTextAreaHelper();
	private final DropTarget mDropTarget = new DropTarget();

	private FileDropListener mFileDropListener;
	private DropTargetListener mDropTargetListener;

	private HighlightInfo mHlInfo = null;

	public DiagTextArea() {
		enableDrop();
	}

	/**
	 * Set callback fired when file is dropped
	 * 
	 * @param listener
	 */
	public void setFileDropListener(FileDropListener listener) {
		mFileDropListener = listener;
	}

	public int getCaretIndex() {
		return getCaretPosition();
	}

	/**
	 * Display the text
	 */
	public void setText(String text) {
		// TODO Set a new document every time to avoid JTextAarea bugs
		setText(text, true);
	}

	/**
	 * Display the text and update the current display area
	 * 
	 * @param text
	 * @param updateCurrentVisibleArea
	 */
	public void setText(String text, boolean updateCurrentVisibleArea) {
		super.setText(text);

		handleHighlightWord();

		if (updateCurrentVisibleArea) {
			getCurrentVisibleAreaAndUpdate();
		}

	}

	/**
	 * Returns text
	 */
	public String getText() {

		final Document doc = getDocument();
		try {
			return doc.getText(0, doc.getLength());
		} catch (BadLocationException e) {
		}
		return null;
	}

	public TextAreaVisibleArea getCurrentVisibleAreaAndUpdate() {
		return getVisibleArea(true);
	}

	/**
	 *
	 * Returns text visible area
	 * 
	 * @param needUpdate
	 *            When setText is done, to fix the number of characters and the
	 *            total number of lines displayed in the current textArea When
	 *            scrolling occurs in the textArea, there are changing values
	 *            (for example, visible rows also change), So update textArea
	 *            again and specify whether to retrieve text and populate it
	 *            into the POJO again.
	 * @return
	 */
	private TextAreaVisibleArea getVisibleArea(boolean needUpdate) {

		if (needUpdate) {
			mTextAreaUtil.getCurrentVisibleArea(DiagTextArea.this, m_Ref_VisibleArea);
		}
		return m_Ref_VisibleArea;
	}

	public int getLastCaretIndexOfCurrentVisible() {

		final TextAreaVisibleArea visibleArea = getCurrentVisibleAreaAndUpdate();

		int visibleEndRowIndex = visibleArea.endRowIndex;

		JTextAreaInfo taInfo = m_Ref_VisibleArea.textAreaInfo;
		List<JTextAreaRowInfo> riList = taInfo.rowInfoList;

		// fail safe
		if (riList.size() - 1 < visibleEndRowIndex) {
			visibleEndRowIndex = riList.size();
		}

		JTextAreaRowInfo visibleAreaLastRowInfo = riList.get(visibleEndRowIndex);

		return visibleAreaLastRowInfo.endIndex;
	}

	public int getFirstCaretIndexOfCurrentVisible() {

		final TextAreaVisibleArea visibleArea = getCurrentVisibleAreaAndUpdate();

		int visibleStartRowIndex = visibleArea.startRowIndex;
		if (LOGGER.isLoggable(Level.FINER)) {
			LOGGER.finer("visibleArea=" + visibleArea);
		}
		// fail safe
		if (visibleStartRowIndex < 0) {
			visibleStartRowIndex = 0;
		}

		JTextAreaInfo taInfo = m_Ref_VisibleArea.textAreaInfo;
		List<JTextAreaRowInfo> riList = taInfo.rowInfoList;

		JTextAreaRowInfo visibleFirstRowInfo = riList.get(visibleStartRowIndex);
		LOGGER.finer("first row is visibleFirstRowInfo=" + visibleFirstRowInfo);
		return visibleFirstRowInfo.startIndex;
	}

	/**
	 * Returns the index of the tail of the caret can be moved taking the text
	 * stored in the current textArea into consideration,
	 * 
	 * @return
	 */
	public int getMaxCaretIndex() {
		final JTextAreaInfo textAreaInfo = getVisibleArea(false).textAreaInfo;
		return textAreaInfo.maxCaretIndexOfThisTextArea;
	}

	/**
	 * Put the caret at the bottom
	 * 
	 * @param caretIndex
	 */
	public void putCaretAtTheBottom(int caretIndex) {
		setCaretPosition(0);
		setCaretPosition(caretIndex);
		scrollToTheTop();
	}

	/**
	 * Move - the line where the specified caret is located - to the top
	 * 
	 * @param caretIndex
	 */
	public void moveCaretLineToTheTop(int caretIndex) {
		scrollToTheBottom();
		setCaretPosition(0);
		setCaretPosition(caretIndex);
	}

	/**
	 * Return the total number of rows in the current text area
	 * 
	 * @return
	 */
	public int getNumOfRow() {
		final JTextAreaInfo textAreaInfo = getVisibleArea(false).textAreaInfo;
		return textAreaInfo.rowInfoList.size();
	}

	public void dragEnter(DropTargetDragEvent dtde) {

	}

	/**
	 * Returns the line number of the specified caret position in index
	 * 
	 * @param caretIndex
	 * @return
	 */
	public int getRowIndexAt(int caretIndex) {
		return getVisibleArea(false).getRowIndexOfCarretInTextArea(caretIndex);
	}

	public int getVisibleRowEndIndex() {
		return getCurrentVisibleAreaAndUpdate().endRowIndex;
	}

	public int getVisibleRowStartIndex() {
		return getCurrentVisibleAreaAndUpdate().startRowIndex;
	}

	/**
	 * Scroll the textArea to the top
	 */
	public void scrollToTheTop() {
		scroll(EScroll.TOP);
	}

	/**
	 * Scroll the textArea to the bottom
	 */
	public void scrollToTheBottom() {
		scroll(EScroll.BOTTOM);
	}

	/**
	 * Scroll the textArea by the specified number of lines
	 * 
	 * @param numOfLines
	 */
	public void scrollTextLine(final int numOfLines) {

		if (numOfLines < 0) {
			LOGGER.finer("numOfLines=" + numOfLines + " move caret to the first caret pos.");

			setCaretPosition(getFirstCaretIndexOfCurrentVisible());

		} else if (numOfLines > 0) {
			LOGGER.finer("numOfLines=" + numOfLines + " move caret to the last caret pos.");
			setCaretPosition(getLastCaretIndexOfCurrentVisible());
		}

		final int loopCount;

		if (numOfLines < 0) {
			loopCount = Math.abs(numOfLines);
		} else if (numOfLines > 0) {
			loopCount = Math.abs(numOfLines);
		} else {
			loopCount = 0;
		}
		LOGGER.finer("loopCount=" + loopCount);

		for (int i = 0; i < (loopCount); i++) {
			if (numOfLines < 0) {
				dispatchVkey(KeyEvent.VK_UP);
			} else if (numOfLines > 0) {
				dispatchVkey(KeyEvent.VK_DOWN);
			}

		}
	}

	private void scroll(EScroll scroll)
	{
		final Rectangle visibleRect = getVisibleRect();
		final Rectangle bounds = getBounds();

		if (EScroll.TOP == scroll) {
			visibleRect.y = 0;
		} else {
			visibleRect.y = bounds.height - visibleRect.height;
		}

		scrollRectToVisible(visibleRect);
	}

	/**
	 * Display caret even if it is read only
	 */
	public void showCaret() {
		mTextAreaUtil.showCursor(DiagTextArea.this);
	}

	private void dispatchVkey(int keyCode) {
		dispatchEvent(new KeyEvent(DiagTextArea.this, KeyEvent.KEY_PRESSED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED));
		dispatchEvent(new KeyEvent(DiagTextArea.this, KeyEvent.KEY_RELEASED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED));
	}

	/**
	 * Enable external file drop
	 */
	private void enableDrop() {

		setDropTarget(mDropTarget);

		try {

			if (mDropTargetListener != null) {
				mDropTarget.removeDropTargetListener(mDropTargetListener);
				mDropTargetListener = null;
			}

			mDropTargetListener = new DropTargetListener()
			{
				@SuppressWarnings("unchecked")
				public void dragEnter(DropTargetDragEvent evt) {

					boolean objectDrappable = false;

					final Transferable t = evt.getTransferable();

					if (mFileDropListener != null) {
						List<File> fileList;

						try {
							fileList = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
							objectDrappable = mFileDropListener.canFileDrop(fileList);

						} catch (UnsupportedFlavorException | IOException ex) {
							objectDrappable = false;
						}
					}

					if (objectDrappable) {
						evt.acceptDrag(DnDConstants.ACTION_COPY);
					} else {
						evt.rejectDrag();
					}
				}

				public void drop(DropTargetDropEvent evt)
				{
					evt.acceptDrop(DnDConstants.ACTION_COPY);

					if (mFileDropListener != null) {
						try
						{
							Transferable transferble = evt.getTransferable();
							if (transferble.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
							{
								@SuppressWarnings("unchecked")
								List<File> fileList = (List<File>) transferble.getTransferData(DataFlavor.javaFileListFlavor);
								mFileDropListener.onFileDrop(fileList);
							}
						}
						catch (UnsupportedFlavorException dex) {
						}
						catch (IOException dex) {
						}
					}
				}

				public void dropActionChanged(DropTargetDragEvent e) {
				}

				public void dragOver(DropTargetDragEvent e) {
				}

				public void dragExit(DropTargetEvent e) {
				}
			};
			mDropTarget.addDropTargetListener(mDropTargetListener);
		} catch (TooManyListenersException e) {
			LOGGER.log(Level.WARNING, "too many listeners.", e);
		}
	}

	@Deprecated
	@Override
	public int getCaretPosition() {
		return super.getCaretPosition();
	}

	public void setHighlightArea(int start, int end) {
		clearHighlightWord();

		final Highlighter hl = getHighlighter();
		try {
			hl.addHighlight(start, end, mHlPaint);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	public void setHighlightWord(String word, boolean caseSensitive) {

		clearHighlightWord();

		mHlInfo = new HighlightInfo();
		mHlInfo.word = word;
		mHlInfo.caseSensitive = caseSensitive;
		handleHighlightWord();
	}

	public void clearHighlightWord() {
		mHlInfo = null;
		handleHighlightWord();
	}

	private void clearHighlights() {
		final Highlighter hl = getHighlighter();
		hl.removeAllHighlights();

	}

	private void handleHighlightWord() {

		clearHighlights();

		if (mHlInfo == null) {
			return;
		}

		final int flags;

		if (mHlInfo.caseSensitive) {
			flags = 0;
		} else {
			flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
		}

		final Pattern pattern = Pattern.compile(mHlInfo.word, flags);
		final Matcher matcher = pattern.matcher(getText());

		int posisionIndex = 0;

		final Highlighter hl = getHighlighter();
		try {
			while (matcher.find(posisionIndex)) {
				int startIndex = matcher.start();
				int endIndex = matcher.end();
				hl.addHighlight(startIndex, endIndex, mHlPaint);
				posisionIndex = endIndex;
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}

	}
}
