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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Utilities;

/**
 * Helper class for JTextArea
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 *
 */
public class JTextAreaHelper {

	private static final Logger LOGGER = Logger.getLogger(JTextAreaHelper.class.getName());

	public static class JTextAreaInfo {

		/**
		 * Each row information
		 */
		public final List<JTextAreaRowInfo> rowInfoList = new ArrayList<JTextAreaRowInfo>();

		/**
		 * The limit position of the caret of the current textArea
		 */
		public int maxCaretIndexOfThisTextArea;

		@Override
		public String toString() {
			return "JTextAreaInfo [rowInfoList=" + rowInfoList + ", maxCaretIndexOfThisTextArea=" + maxCaretIndexOfThisTextArea + "]";
		}

	}

	public static class JTextAreaRowInfo {
		public int rowIndex;
		public int startIndex;
		public int endIndex;

		public String text;

		@Override
		public String toString() {
			return "JTextAreaRowInfo [rowIndex=" + rowIndex + ", startIndex=" + startIndex + ", endIndex=" + endIndex + ", text=" + text + "]";
		}

	}

	public static class TextAreaVisibleArea {

		public int startRowIndex;
		public int endRowIndex;

		// Lines of textArea
		public int numOfRows;

		public JTextAreaInfo textAreaInfo;

		/**
		 * Returns what row index is the cursor.
		 * 
		 * @param carretIndex
		 * @return
		 */
		public int getRowIndexOfCarretInTextArea(int carretIndex) {

			int ret = 0;

			final List<JTextAreaRowInfo> rowInfoList = textAreaInfo.rowInfoList;

			for (int i = 0; i < rowInfoList.size(); i++) {

				JTextAreaRowInfo rowInfo = rowInfoList.get(i);

				if (carretIndex < rowInfo.startIndex) {
					break;
				}

				ret = i;
			}
			return ret;
		}

		@Override
		public String toString() {
			return "TextAreaVisibleArea [startRowIndex=" + startRowIndex + ", endRowIndex=" + endRowIndex + ", textAreaInfo=" + textAreaInfo + "]";
		}

	}

	public void getCurrentVisibleArea(DiagTextArea textArea, TextAreaVisibleArea refVisibleArea) {

		final Rectangle taVisibleAreaRect = textArea.getVisibleRect();

		final int taVisibleAreaTop = taVisibleAreaRect.y;
		final int taVisibleAreaBottom = taVisibleAreaTop + taVisibleAreaRect.height;

		final int heightOfOneLine = textArea.getFontMetrics(textArea.getFont()).getHeight();

		final int startRowIndexOfViewArea = (int) Math.ceil((double) taVisibleAreaTop / (double) heightOfOneLine);
		final int endRowIndexOfViewArea = (int) Math.floor((double) taVisibleAreaBottom / (double) heightOfOneLine) - 1;

		LOGGER.finer("startRowIndexOfViewArea=" + startRowIndexOfViewArea + " " + taVisibleAreaRect + " heightOfOneLine=" + heightOfOneLine + " textArea.getFont()=" + textArea.getFont());

		final int numOfRows = endRowIndexOfViewArea - startRowIndexOfViewArea + 1;
		final JTextAreaInfo textAreaInfo = getTextAreaInfo(textArea);

		if (LOGGER.isLoggable(Level.FINEST)) {

			final List<JTextAreaRowInfo> ri = textAreaInfo.rowInfoList;
			for (int i = 0; i < ri.size(); i++) {
				JTextAreaRowInfo r = ri.get(i);
				LOGGER.finest("JTextAreaRowInfo[" + i + "]" + " " + r);
			}

		}

		final int numOfTotalRow = textAreaInfo.rowInfoList.size();
		final int maxRowIndex = numOfTotalRow - 1;
		final int endRowIndexConsidreredMaxRow = Math.min(endRowIndexOfViewArea, maxRowIndex);

		refVisibleArea.startRowIndex = startRowIndexOfViewArea;
		refVisibleArea.endRowIndex = endRowIndexConsidreredMaxRow;
		refVisibleArea.numOfRows = numOfRows;
		refVisibleArea.textAreaInfo = textAreaInfo;

	}

	private JTextAreaInfo getTextAreaInfo(final JTextArea textArea) {

		final JTextAreaInfo taInfo = new JTextAreaInfo();

		final Document document = textArea.getDocument();
		final int textAreaTextLeng = document.getLength();

		if (textAreaTextLeng == 0) {
			return taInfo;
		}

		int prevRowEndIndex = 0;
		int rowIndex = 0;

		String fullText = null;

		if (LOGGER.isLoggable(Level.FINER)) {
			fullText = textArea.getText();
		}

		try {

			while (true) {

				JTextAreaRowInfo rowInfo = new JTextAreaRowInfo();

				final int rowStartIndex = Utilities.getRowStart(textArea, prevRowEndIndex + 1);
				final int rowEndIndex = Utilities.getRowEnd(textArea, rowStartIndex);

				// memory previous pos
				prevRowEndIndex = rowEndIndex;

				rowInfo.rowIndex = rowIndex;
				rowInfo.startIndex = rowStartIndex;
				rowInfo.endIndex = rowEndIndex;

				if (LOGGER.isLoggable(Level.FINER)) {

					String text = fullText.substring(rowStartIndex, rowEndIndex);
					if (text.length() > 20) {
						text = text.substring(0, 21);
					}

					rowInfo.text = "[" + rowIndex + "]" + text;
				}

				taInfo.rowInfoList.add(rowInfo);

				rowIndex++;

				if (rowEndIndex == textAreaTextLeng) {
					break;
				}

			}

			taInfo.maxCaretIndexOfThisTextArea = prevRowEndIndex;

		} catch (BadLocationException e) {
			LOGGER.log(Level.WARNING, "", e);

		}

		return taInfo;
	}

	public void showCursor(JTextArea textArea) {
		textArea.requestFocus();
		textArea.getCaret().setVisible(true);
	}

}
