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

import java.util.logging.Logger;

import org.riversun.llpad.fw.Disposable;
import org.riversun.llpad.util.file_buffer.TextBlock;
import org.riversun.llpad.util.file_buffer.TextFileBufferedWrapper;
import org.riversun.llpad.util.file_buffer.TextFileBufferedWrapper.Text2Display;

/**
 * 
 * Manage where the user is looking in the file {@see TaViewRangeManager}
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 */
public class ViewRange implements Disposable {

	private static final Logger LOGGER = Logger.getLogger(ViewRange.class.getName());
	private final Text2Display mText2DCache = new Text2Display();

	private final int mViewAreaSizeBytes;

	private ViewRangeEventListener mViewRangeListener;
	private TextFileBufferedWrapper mFileBufferedWrapper = null;

	private long mPrevViewStartAddr = -1;

	private long mViewStartAddr;
	private long mViewEndAddr;
	private long mCursorAddr;
	private int mPageIncrementSizeBytes = 512;

	public enum ECursorDir {

		FORWARD,
		BACKWARD,
		SEEKING,
		SEEK_END,
		NOTHING
	}

	public ViewRange(TextFileBufferedWrapper fileBufferedWrapper, long viewStartAddr, int viewAreaSizeBytes) {

		mFileBufferedWrapper = fileBufferedWrapper;

		final long fileSize = fileBufferedWrapper.getFileSize();

		if (fileSize < viewAreaSizeBytes) {
			viewAreaSizeBytes = (int) fileSize;
		}

		mViewStartAddr = viewStartAddr;
		mViewEndAddr = mViewStartAddr + viewAreaSizeBytes - 1;
		mViewAreaSizeBytes = viewAreaSizeBytes;

		LOGGER.fine("prefferedViewStartAddr=" + viewStartAddr + " prefferedViewEndAddr=" + mViewEndAddr + " viewAreaSizeBytes=" + viewAreaSizeBytes);

		mFileBufferedWrapper.readAreaFromCachedText(viewStartAddr, mViewEndAddr, mText2DCache);
	}

	public long getFileEndAddress() {
		return mFileBufferedWrapper.getFileEndAddress();
	}

	public void setPageIncrementSizeBytes(int pageIncrementSizeBytes) {
		mPageIncrementSizeBytes = pageIncrementSizeBytes;
	}

	public void readForward() {
		mPrevViewStartAddr = mViewStartAddr;
		mViewStartAddr += mPageIncrementSizeBytes;
		updateView(ECursorDir.FORWARD);
	}

	public void readBackward() {
		mPrevViewStartAddr = mViewStartAddr;
		mViewStartAddr -= mPageIncrementSizeBytes;
		updateView(ECursorDir.BACKWARD);
	}

	public void seek(boolean adjusting, long addr) {

		setCursorAddress(addr);

		mPrevViewStartAddr = mViewStartAddr;
		mViewStartAddr = addr;
		if (adjusting) {
			updateView(ECursorDir.SEEKING);
		} else {
			updateView(ECursorDir.SEEK_END);
		}
	}

	public void updateView() {
		mPrevViewStartAddr = mViewStartAddr;
		updateView(ECursorDir.NOTHING);
	}

	private void updateView(ECursorDir dir) {

		mViewEndAddr = mViewStartAddr + mViewAreaSizeBytes - 1;

		if (mViewStartAddr < 0) {
			mViewStartAddr = 0;
			mViewEndAddr = mViewStartAddr + mViewAreaSizeBytes - 1;

		}

		if (mViewEndAddr > mFileBufferedWrapper.getFileEndAddress()) {
			mViewEndAddr = mFileBufferedWrapper.getFileEndAddress();
			mViewStartAddr = mViewEndAddr - mViewAreaSizeBytes + 1;
		}

		LOGGER.fine("mViewStartAddr=" + mViewStartAddr + " mViewEndAddr=" + mViewEndAddr + " mCursorAddr=" + mCursorAddr + " dir=" + dir);

		/*
		 * Read specified position from buffered file wrapper.But in the case
		 * the document is to be displayed from the middle of the
		 * sentence.Wrapper will extract a sentence by searching from line feeds
		 * to line feeds so that the display text becomes pretty.So,it is highly
		 * possible that the viewStartAddress specified as an argument and
		 * viewStartAddress stored in POJO(text2dcache) are different.
		 */
		mFileBufferedWrapper.readAreaFromCachedText(mViewStartAddr, mViewEndAddr, mText2DCache);

		// refer viewStartAddress stored in POJO(text2dcache)
		final long prefferedViewStartAddr = mText2DCache.prefferedViewStartAddr;

		if (mViewStartAddr != mPrevViewStartAddr && prefferedViewStartAddr == mPrevViewStartAddr) {

			/*
			 * When attempting to move the view area in the textArea. After
			 * pretty formatting ,the view start position remained the previous
			 * position. For example, when trying to move up (move back) with
			 * the cursor key, the previous sentence is very long, eventually
			 * trying to extract from the line feed code to the line feed code
			 * becomes the same area as the previous time. If this situation
			 * happens, no operation can scroll the page
			 */
			LOGGER.warning("!!WARNING!! Tried to get a correct position to prety print."
					+ "But failed to get a correct position because of the prev/next sentense may be too long.You should try to change mPageShiftSizeBytes value bigger.");

			// TODO Consider self-diagnosis function
		}

		LOGGER.fine("pretty print view area before=(" + mViewStartAddr + "-" + mViewEndAddr + ") after=(" + mText2DCache.prefferedViewStartAddr + "-" + mText2DCache.prefferedViewEndAddr + ")");

		// Update the address according to the result of pretty
		// formatting of a sentence
		mViewStartAddr = mText2DCache.prefferedViewStartAddr;
		mViewEndAddr = mText2DCache.prefferedViewEndAddr;

		if (mViewStartAddr > mCursorAddr) {

			// If the cursor address is smaller than ViewAddr after pretty
			// formatting of a sentence ,set the cursor address to ViewAddr
			mCursorAddr = mViewStartAddr;
		}

		if (mViewRangeListener != null) {
			mViewRangeListener.onUpdateText(mText2DCache, dir);
		}
	}

	public void setCursorAddress(long addr) {
		mCursorAddr = addr;
	}

	public long getCursorAddress() {
		return mCursorAddr;
	}

	public void setViewRangeEventListener(ViewRangeEventListener listener) {
		mViewRangeListener = listener;
	}

	public static interface ViewRangeEventListener {
		public void onUpdateText(Text2Display text2d, ECursorDir dir);
	}

	private TextBlock getTextBlock() {
		return mFileBufferedWrapper.getTextBlock();
	}

	/**
	 * Return end address of current view(visible part)
	 * 
	 * @return
	 */
	public long getViewEndAddress() {
		return mViewEndAddr;
	}

	/**
	 * Return start address of current view(visible part)
	 * 
	 * @return
	 */
	public long getViewStartAddress() {
		return mViewStartAddr;
	}

	public int getViewStartIndex() {
		final int viewStartIndex = getTextBlock().getStringIndexFromAddress(getViewStartAddress());
		return viewStartIndex;

	}

	public int getViewEndIndex() {
		final int viewEndIndex = getTextBlock().getStringIndexFromAddress(getViewEndAddress());
		return viewEndIndex;
	}

	public int getCursorIndex(int caretIndex) {
		final int cursorIndex = caretIndex + getViewStartIndex();
		return cursorIndex;

	}

	public long getAddressFromStringIndex(int strIndex) {
		return getTextBlock().getAddressFromStringIndex(strIndex);
	}

	public boolean isViewShowingTheEndOfTheFile() {
		// Whether the viewArea is now looking at the end of the file
		final boolean isTextAreaShowingTheEndOfTheFile = getViewEndAddress() == getFileEndAddress();// mFileBufferedWrapper.getFileEndAddress();
		return isTextAreaShowingTheEndOfTheFile;
	}

	@Override
	public void dispose() {
		mViewRangeListener = null;
		mFileBufferedWrapper = null;
		mText2DCache.dispose();
	}

}
