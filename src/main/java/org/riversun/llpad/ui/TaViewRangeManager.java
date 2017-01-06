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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.PlainDocument;

import org.riversun.llpad.AppDef;
import org.riversun.llpad.fw.Disposable;
import org.riversun.llpad.ui.GUIBuilder.GuiComponent;
import org.riversun.llpad.ui.TaBuilder.CaretDir;
import org.riversun.llpad.ui.TaBuilder.JTextAreaEventListener;
import org.riversun.llpad.ui.ViewRange.ECursorDir;
import org.riversun.llpad.ui.ViewRange.ViewRangeEventListener;
import org.riversun.llpad.util.AddressFormatter;
import org.riversun.llpad.util.file_buffer.TextBlock;
import org.riversun.llpad.util.file_buffer.TextFileBufferedWrapper;
import org.riversun.llpad.util.file_buffer.TextFileBufferedWrapper.Text2Display;
import org.riversun.llpad.widget.component.DiagTextArea;
import org.riversun.llpad.widget.component.VerticalSeekBar;
import org.riversun.llpad.widget.component.VerticalSeekBar.TrackAreaEventListener;
import org.riversun.llpad.widget.component.VerticalSeekBar.TrackClickEventType;
import org.riversun.llpad.widget.component.VerticalSeekBar.VScrollBarEvent;
import org.riversun.llpad.widget.helper.EDTHandler;

/**
 * 
 * The responsibility of this class is to coordinate TextArea{@see DiagTextArea}
 * and ViewRange{@see ViewRange} and VerticalSeekBar{@see VerticalSeekBar}.
 * <p>
 * Case 1: {@see ViewRange} and {@see DiagTextArea}<br>
 * {@see ViewRange} tells where the user is looking in the file, Manage where in
 * the file should be displayed next.
 * 
 * Specifically, the range to be viewed is displayed in {@link DiagTextArea},
 * but {@see ViewRange} does not directly deal with the GUI part.
 * 
 * So, {@see #TaViewRangeManager} bridges TextArea and ViewArea.
 * <p>
 * Case 2: {@see DiagTextArea} and {@see VerticalSeekBar}<br>
 * 
 * - Pattern 1:Reading range changes depending on TextArea event by user.And the
 * updated reading range must also be reflected to the {@see ViewRange} and
 * {@see VerticalSeekBar}
 * 
 * - Pattern 2:Reading range changes depending on {@see VerticalSeekBar} event
 * by user.{@see ViewRange} and {@link DiagTextArea} must be updated in the same
 * way as above.
 * <p>
 * Naming rules for caret variables<br>
 * - Cursor ... Indicates the cursor position in the entire document.<br>
 * - Caret ... Indicates the cursor position in the text area
 * {@link DiagTextArea}.<br>
 * <p>
 * Prefix naming rules for something like a pointer<br>
 * Index · · · Use when counting character by character<br>
 * Addr · · · Use when handling in 1-byte units as an address<br>
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 *
 */
public class TaViewRangeManager implements JTextAreaEventListener, ViewRangeEventListener, Disposable {

	private static final Logger LOGGER = Logger.getLogger(TaViewRangeManager.class.getName());

	private final EDTHandler mHandler = new EDTHandler();
	private final PlainDocument mTempDocument = new PlainDocument();

	private final ViewRange mViewRange;

	private final TextFileBufferedWrapper mFileWrapper;
	private final GuiComponent mViews;

	private long mPrevViewStartAddr = -1;
	private long mPrevCursorAddr = -1;

	private static final int EXPECTED_AVERAGE_LENGTH_OF_ONE_LINE = 80;

	/**
	 * 
	 * @param fileWrapper
	 *            buffered wrapper
	 * 
	 * @param views
	 *            holder of GUI components
	 * 
	 * @param viewStartAddr
	 *            staring address for view
	 * 
	 * @param viewAreaSizeBytes
	 *            The size(as bytes) of the text to display in the textArea at
	 *            one time. App reads a part of the file into the buffer,
	 * 
	 * @param pageIncrementSizeBytes
	 *            the size that the display area is incremented by when the
	 *            caret reaches the end of the text area
	 */
	public TaViewRangeManager(TextFileBufferedWrapper fileWrapper, GuiComponent views, long viewStartAddr, int viewAreaSizeBytes, int pageIncrementSizeBytes) {

		mViews = views;
		mFileWrapper = fileWrapper;

		if (mFileWrapper.getFileSize() < viewAreaSizeBytes) {
			viewAreaSizeBytes = (int) mFileWrapper.getFileSize();
		}

		mViewRange = new ViewRange(mFileWrapper, viewStartAddr, viewAreaSizeBytes);

		mViewRange.setPageIncrementSizeBytes(pageIncrementSizeBytes);
		mViewRange.setViewRangeEventListener(this);
		mViews.taBuilder.setJTextAreaEvent(this);

		final VerticalSeekBar verticalSeekBar = mViews.verticalSeekBar;
		verticalSeekBar.setEnabled(true);

		final long fileSize = mFileWrapper.getFileSize();

		// Number of lines of text file to be read (Roughly predicted value from
		// file size)
		final long expectedLines = fileSize / EXPECTED_AVERAGE_LENGTH_OF_ONE_LINE;

		/*
		 * Number of lines that can be displayed on one page. Get the number of
		 * lines that can be displayed on one page from textArea.
		 */
		final int numOfRows = mViews.textArea.getCurrentVisibleAreaAndUpdate().numOfRows;

		if (expectedLines <= Integer.MAX_VALUE) {
			LOGGER.fine("set expected num of lines=" + expectedLines + " for seekbar thumb size.");

			// Width of the knob for 1 page
			// Width of knob =
			// ( Maximum value of verticalSeekBar) * (number of lines that can
			// be displayed on 1 page / number of whole lines)
			final int seekBarKnobWidth = (int) ((double) expectedLines * (double) numOfRows / (double) expectedLines);

			verticalSeekBar.setVisibleAmount(seekBarKnobWidth);
			verticalSeekBar.setMaxIntValue((int) expectedLines);

		} else {
			verticalSeekBar.setVisibleAmount(0);
			verticalSeekBar.setMaxIntValue(Integer.MAX_VALUE);
		}

		verticalSeekBar.setMaxLongValue(fileSize - 1);

		verticalSeekBar.setVScrollBarEvent(new VScrollBarEvent() {
			long oldAddress = -1;

			@Override
			public void onLongValueChanged(boolean adjusting, long address) {

				if (AppDef.SeekBar.SEEK_WHEN_ADJUSTING_WITH_SEEKBAR || (!AppDef.SeekBar.SEEK_WHEN_ADJUSTING_WITH_SEEKBAR && adjusting == false)) {

					LOGGER.fine("!!SEEK!! with vertical scrollbar address=" + AddressFormatter.getFmtAddress(address));

					if (Math.abs(oldAddress - address) > AppDef.TextBuffer.BUFFER_SIZE_BYTES) {

						LOGGER.fine("!!BUFFER JUMP SEEK!! with vertical scrollbar address=" + AddressFormatter.getFmtAddress(address));
					} else {
						LOGGER.fine("!!SEEK!! with vertical scrollbar address=" + AddressFormatter.getFmtAddress(address));
					}

					mViewRange.seek(adjusting, address);

				}

			}
		});

		verticalSeekBar.setOnTrackAreaClickedListener(new TrackAreaEventListener() {

			@Override
			public void onTrackClicked(TrackClickEventType trackEvent) {

				final DiagTextArea textArea = mViews.textArea;

				if (trackEvent == null) {
					LOGGER.warning("position=" + trackEvent);
					return;
				}
				switch (trackEvent) {
				case TRACK_UP:
					textArea.scrollTextLine(-AppDef.SeekBar.NUM_OF_LINES_INCREMENTED_BY_TRACK_CLICK);
					verticalSeekBar.setLongValue(getCursorAddressOfCurrentCaret());
					break;
				case TRACK_DOWN:
					textArea.scrollTextLine(AppDef.SeekBar.NUM_OF_LINES_INCREMENTED_BY_TRACK_CLICK);
					verticalSeekBar.setLongValue(getCursorAddressOfCurrentCaret());
					break;
				case ARROW_UP:
					textArea.scrollTextLine(-AppDef.SeekBar.NUM_OF_LINES_INCREMENTED_BY_ARROW_CLICK);
					verticalSeekBar.setLongValue(getCursorAddressOfCurrentCaret());
					break;
				case ARROW_DOWN:
					textArea.scrollTextLine(AppDef.SeekBar.NUM_OF_LINES_INCREMENTED_BY_ARROW_CLICK);
					verticalSeekBar.setLongValue(getCursorAddressOfCurrentCaret());
					break;

				}

			}
		});
	}

	public long getCursorAddressOfCurrentCaret() {

		final int caretIndex = mViews.textArea.getCaretIndex();
		final int viewStartIndex = mViewRange.getViewStartIndex();
		final int viewEndIndex = mViewRange.getViewEndIndex();

		final int cursorIndex = caretIndex + viewStartIndex;

		// Whether or not the view shows the end of the file
		final boolean isViewShowingTheEndOfTheFile = mViewRange.isViewShowingTheEndOfTheFile();

		// Whether or not the caret is at the last position of the view
		final boolean isCursorPlacedInViewEnd = cursorIndex >= viewEndIndex;

		if (isCursorPlacedInViewEnd && isViewShowingTheEndOfTheFile) {
			LOGGER.fine("Caret is place in the end of the file");

			return mViewRange.getFileEndAddress();
		}

		final long cursorAddr = mViewRange.getAddressFromStringIndex(cursorIndex);

		return cursorAddr;
	}

	public void show() {
		mViewRange.updateView();
	}

	@Override
	public void onUpdateText(Text2Display text2disp, ECursorDir dir) {

		// Cursor is absolute position of the caret
		final long cursorAddr = mViewRange.getCursorAddress();

		final long viewStartAddr = mViewRange.getViewStartAddress();
		final long viewEndAddr = mViewRange.getViewEndAddress();

		LOGGER.fine("call show text at viewArea=(" + AddressFormatter.getFmtAddress(viewStartAddr) + "-" + AddressFormatter.getFmtAddress(viewEndAddr) + ")");

		if (mPrevViewStartAddr == viewStartAddr && (dir != ECursorDir.SEEKING && dir != ECursorDir.SEEK_END)) {
			// When not changing
			return;
		}

		LOGGER.fine("call #getStringIndexFromAddress (1)prefferedCursorAddr=" + AddressFormatter.getFmtAddress(cursorAddr) +
				" (2)prefferedView Arear=(" + AddressFormatter.getFmtAddress(viewStartAddr) + "-" + AddressFormatter.getFmtAddress(viewEndAddr) + ")");
		final TextBlock textBlock = mFileWrapper.getTextBlock();
		final int caretIndex = textBlock.getStringIndexFromAddress(cursorAddr) - textBlock.getStringIndexFromAddress(viewStartAddr);

		LOGGER.fine("caretIndex=" + caretIndex);

		mHandler.post(new Runnable() {

			@Override
			public void run() {

				if (ECursorDir.SEEKING == dir) {

					// When seeking, write to another document
					// TODO It is more efficient to have this processing in a
					// {@see DiagTextArea}
					mViews.textArea.setDocument(mTempDocument);
					mViews.textArea.setText(text2disp.text);
				} else {
					mViews.textArea.setDocument(mViews.textAreaDocument);
					mViews.textArea.setText(text2disp.text);
				}

				if (LOGGER.isLoggable(Level.FINE) && AppDef.TextArea.SHOW_PRETTY_SENTENCES) {
					LOGGER.fine("diaplay text below."
							+ " \n=====CACHED TEXT START at " + AddressFormatter.getFmtAddress(viewStartAddr) + " =======\n"
							+ text2disp._firstLine + "\n...[" + (text2disp._numOfLines - 2) + " lines here]...\n"
							+ text2disp._lastLine
							+ "\n=====CACHED TEXT END at " + AddressFormatter.getFmtAddress(viewEndAddr) + " =======");
				}

				if (dir == ECursorDir.FORWARD) {
					// When trying to show next

					// Put the caret at the bottom
					mViews.textArea.putCaretAtTheBottom(caretIndex);

				} else if (dir == ECursorDir.BACKWARD) {
					// When trying to show previous

					// Move - the line where the specified caret is located - to
					// the top
					mViews.textArea.moveCaretLineToTheTop(caretIndex);

				} else if (dir == ECursorDir.SEEK_END) {

					// When the seek by the scroll bar is completed

					mViews.textArea.setCaretPosition(caretIndex);

				} else if (dir == ECursorDir.NOTHING) {
					mViews.textArea.setCaretPosition(caretIndex);
				}
				mPrevViewStartAddr = viewStartAddr;
			}
		});

	}

	@Override
	public void onCaretMovingStarted(CaretDir caretDir, int caretIndex, int caretRowIndex) {

		LOGGER.fine("caretDir=" + caretDir + " caretIndex=" + caretIndex + " caretRowIndex=" + caretRowIndex);

		if (caretDir == null) {
			return;
		}

		final int viewStartIndex = mViewRange.getViewStartIndex();
		final int viewEndIndex = mViewRange.getViewEndIndex();

		final int cursorIndex = caretIndex + viewStartIndex;

		// Whether the current view shows the end of the file
		final boolean isViewShowingTheEndOfTheFile = mViewRange.isViewShowingTheEndOfTheFile();

		// Whether the caret is located at the end of the view on the buffer.
		final boolean isCursorPlacedInViewEnd = cursorIndex >= viewEndIndex;

		if (isCursorPlacedInViewEnd && isViewShowingTheEndOfTheFile) {
			// When the caret is at the end of the file
			return;
		}

		final long cursorAddr = mViewRange.getAddressFromStringIndex(cursorIndex);

		mViewRange.setCursorAddress(cursorAddr);

		if (mPrevCursorAddr == cursorAddr) {
			return;
		}

		mPrevCursorAddr = cursorAddr;

		// Whether or not the caret is at the first position in the text area
		final boolean isCarretPlacedInFirstIndex = (caretIndex == 0);

		// Whether or not the caret is at the last position in the text area
		final boolean isCaretPlacedInLastIndex = (caretIndex == mViews.textArea.getMaxCaretIndex());

		// Whether or not the caret is in the first line in the text area
		final boolean isCarretPlacedInFirstRow = (0 == caretRowIndex);

		// Whether or not the caret is in the last line in the text area
		final boolean isCarretPlacedInLastRow = (mViews.textArea.getNumOfRow() - 1 == caretRowIndex);

		switch (caretDir) {
		case UP:
			if (isCarretPlacedInFirstRow) {

				LOGGER.fine("!!SHOW PREV TEXT!! Caret placed in the first row of TEXTAREA " + "prefferedCursorAddr=" + cursorAddr);

				mViewRange.readBackward();
				if (AppDef.TextArea.UPDATE_SEEK_BAR_WHEN_KEY_REPEATING) {
					updateSeekBar(caretIndex, caretRowIndex);
				}

			}
			break;
		case LEFT:
			if (isCarretPlacedInFirstIndex) {

				LOGGER.fine("!!SHOW PREV TEXT!! Caret placed in the first position of TEXTAREA " + "prefferedCursorAddr=" + cursorAddr);

				mViewRange.readBackward();
				if (AppDef.TextArea.UPDATE_SEEK_BAR_WHEN_KEY_REPEATING) {
					updateSeekBar(caretIndex, caretRowIndex);
				}

			}
			break;
		case RIGHT:
			if (isCaretPlacedInLastIndex) {

				LOGGER.fine("!!SHOW NEXT TEXT!! Caret placed in the last position of TEXTAREA caretRowIndex=" + caretRowIndex + " prefferedCursorAddr=" + cursorAddr);

				mViewRange.readForward();
				if (AppDef.TextArea.UPDATE_SEEK_BAR_WHEN_KEY_REPEATING) {
					updateSeekBar(caretIndex, caretRowIndex);
				}

			}
			break;
		case DOWN:

			if (isCarretPlacedInLastRow) {

				LOGGER.fine("!!SHOW NEXT TEXT!! Caret placed in the last row of TEXTAREA caretRowIndex=" + caretRowIndex + " prefferedCursorAddr=" + cursorAddr);

				mViewRange.readForward();
				if (AppDef.TextArea.UPDATE_SEEK_BAR_WHEN_KEY_REPEATING) {
					updateSeekBar(caretIndex, caretRowIndex);
				}

			}
			break;

		}

	}

	@Override
	public void onCaretMovingFinished(int caretIndex, int caretRowIndex) {
		updateSeekBar(caretIndex, caretRowIndex);
	}

	private void updateSeekBar(int caretIndex, int caretRowIndex) {

		final int viewStartIndex = mViewRange.getViewStartIndex();
		final int viewEndIndex = mViewRange.getViewEndIndex();
		final int cursorIndex = caretIndex + viewStartIndex;

		LOGGER.finer("caretIndex=" + caretIndex + " caretRowIndex=" + caretRowIndex + " viewST<cursor<viewED -> " + viewStartIndex + "< " + cursorIndex + " <" + viewEndIndex);

		// Update VerticalSeekBar's position
		final VerticalSeekBar verticalSeekBar = mViews.verticalSeekBar;
		verticalSeekBar.setLongValue(getCursorAddressOfCurrentCaret());
	}

	public ViewRange getViewRange() {
		return mViewRange;
	}

	@Override
	public void dispose() {
		mFileWrapper.dispose();
		mViewRange.dispose();
	}
}
