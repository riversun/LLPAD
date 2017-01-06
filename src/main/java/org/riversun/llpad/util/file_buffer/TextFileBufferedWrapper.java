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
package org.riversun.llpad.util.file_buffer;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.riversun.llpad.AppDef;
import org.riversun.llpad.fw.Disposable;
import org.riversun.llpad.util.file.TextFileInfoHelper;

/**
 * Partially read the text file and cache it in memory.
 * <p>
 * Automatically extract line feed position to prevent things such as being
 * displayed from the middle of a sentence.And cut it to be pretty looking when
 * displayed.
 *
 * @author Tom Misawa (riversun.org@gmail.com)
 */
public class TextFileBufferedWrapper implements Disposable {

	private static final Logger LOGGER = Logger.getLogger(TextFileBufferedWrapper.class.getName());

	/**
	 * The text to display
	 * 
	 * @author Tom Misawa (riversun.org@gmail.com)
	 *
	 */
	public static class Text2Display implements Disposable {

		/**
		 * What is "preffered address"
		 * <p>
		 * Position where it is preferable to display from here if it is taken
		 * into consideration such as line break. Though it is different from
		 * the designated position
		 */
		public long prefferedViewStartAddr;
		public long prefferedViewEndAddr;
		public long prefferedCursorAddr;

		public String text;

		public String _firstLine;
		public String _lastLine;
		public int _numOfLines;

		@Override
		public void dispose() {
			// make sure
			text = null;
			_firstLine = null;
			_lastLine = null;
		}

	}

	private final FilePartialReader mFilePartialReader = new FilePartialReader();
	private final TextFileInfoHelper mFileUtil = new TextFileInfoHelper();
	private final File mSrcFile;

	private String mEncoding = null;
	private boolean mIsTextFile = false;

	private long mFileSize = 0;

	private long mBufferStartAddr = -1;
	private long mBufferEndAddr = -1;

	private int mBufferSize = 10 * 1024;

	private long mWindowSize = 256;

	private TextBlock mCurrentCachedTextBlock;

	// This file's line feed code
	private String mFileLineFeedCode = "";

	// System line feed code (used to display in text area)
	// private final String SYSTEM_LF_CODE =
	// // System.lineSeparator() //JDK1.7
	// System.getProperty("line.separator");

	/**
	 * 
	 * @param file
	 * @param bufferSize
	 *            The size of the buffer to be read and cached at one time from
	 *            the file
	 * @param windowSize
	 *            The size of the bytes if the difference between the starting
	 *            address of the buffer(or the end of the buffer) and the
	 *            current display starting address (or ending address) is less
	 *            than this value, do read from the file additionally.
	 */
	public TextFileBufferedWrapper(File file, int bufferSize, int windowSize) {

		if (file == null || file.isFile() == false) {
			throw new RuntimeException("This file doesn't exist.");
		}

		this.mSrcFile = file;
		this.mFileSize = file.length();
		this.mEncoding = mFileUtil.detectEncoding(file);
		this.mFileLineFeedCode = mFileUtil.detectLineFeed(file, mEncoding);
		this.mBufferSize = bufferSize;
		this.mWindowSize = windowSize;

		if (mEncoding == null) {
			mIsTextFile = false;
		} else {
			mIsTextFile = true;
		}

		if (mFileSize < mBufferSize) {
			// If the file is smaller than the buffer
			// make the buffer the same as the file size
			mBufferSize = (int) mFileSize;
		}
	}

	public void setBufferStartAddress(long globalAddressOfFile) {
		this.mBufferStartAddr = globalAddressOfFile;
	}

	public void setBufferEndAddress(long globalAddressOfFile) {
		this.mBufferEndAddr = globalAddressOfFile;
	}

	/**
	 * Populates the cache of the specified range
	 * <p>
	 * Determine the position of the buffer from the view start position /view
	 * end position <br>
	 * Determine whether new buffer reading is necessary or not.
	 * <p>
	 * If the specified address is too close to the cached area(difference is
	 * less than windowSize), new data is read from the file additionally.
	 * <p>
	 * In the case the document is to be displayed from the middle of the
	 * sentence. Extract a sentence by searching from line feeds to line feeds
	 * so that the display text becomes pretty.
	 * 
	 * @param viewStartAddr
	 * @param viewEndAddr
	 * @param refText2DCache
	 */
	public void readAreaFromCachedText(long viewStartAddr, long viewEndAddr, Text2Display refText2DCache) {

		// Remember the previous state
		long prevBufferStartPos = mBufferStartAddr;
		long prevBufferEndPos = mBufferEndAddr;

		final boolean isReadFirst = mBufferStartAddr < 0 && mBufferEndAddr < 0;

		if (isReadFirst) {

			// In the first case
			mBufferStartAddr = (viewStartAddr - mBufferSize / 2);
			mBufferEndAddr = (viewEndAddr + mBufferSize / 2);

			if (mBufferStartAddr < 0L) {
				// When sticking on top
				mBufferStartAddr = 0L;
				mBufferEndAddr = mBufferStartAddr + mBufferSize - 1;
			}

			if (mBufferEndAddr > mFileSize - 1) {
				// When sticking on bottom
				mBufferEndAddr = mFileSize - 1;
				mBufferStartAddr = mBufferEndAddr - mBufferSize + 1;
			}

			LOGGER.fine("!!FIRST CALL!! viewAddr=(" + viewStartAddr + "-" + viewEndAddr + ") bufferAddr=(" + mBufferStartAddr + "-" + mBufferEndAddr + ")");

		}

		LOGGER.fine("viewAddr=(" + viewStartAddr + "-" + viewEndAddr + ") bufferAddr=(" + mBufferStartAddr + "-" + mBufferEndAddr + ")");

		// Difference from the beginning of the buffer to the current display
		// start position
		final long upperWindow = viewStartAddr - mBufferStartAddr;

		// Difference from the end of the buffer to the current display end
		// position
		final long bottomWindow = mBufferEndAddr - viewEndAddr;

		// When the ceiling(top) approaches
		if (upperWindow < mWindowSize)
		{
			// It means that the cursor wants to go up.
			// Therefore bring the center of the read buffer to the upper side
			// of the display area.
			mBufferStartAddr = viewStartAddr - mBufferSize / 2;

			if (mBufferStartAddr < 0) {
				mBufferStartAddr = 0L;
			}
			mBufferEndAddr = mBufferStartAddr + mBufferSize - 1;
		}

		// When the floor(bottom) approaches
		if (bottomWindow < mWindowSize) {

			// It means that the cursor wants to go down.
			// Therefore bring the center of the read buffer to the bottom
			// of the display area.
			mBufferEndAddr = viewEndAddr + mBufferSize / 2;

			if (mBufferEndAddr > mFileSize - 1) {
				mBufferEndAddr = mFileSize - 1;
			}
			mBufferStartAddr = mBufferEndAddr - mBufferSize + 1;
		}

		if (prevBufferStartPos != mBufferStartAddr || prevBufferEndPos != mBufferEndAddr) {
			// Determine whether need to read text from a file again

			if (isReadFirst) {
				LOGGER.fine("!!READ NEW BLOCK FIRST!! from file"
						+ " to-Be read BufferArea=(" + mBufferStartAddr + "-" + mBufferEndAddr + ")"
						+ ",to-Be show ViewArea=(" + viewStartAddr + "-" + viewEndAddr + ")"
						);

			} else {
				LOGGER.fine("!!READ NEW BLOCK!! from file as-Is red BufferArea=(" + prevBufferStartPos + "-" + prevBufferEndPos + ")"
						+ ",to-Be read BufferArea=(" + mBufferStartAddr + "-" + mBufferEndAddr + ")"
						+ ",to-Be show ViewArea=(" + viewStartAddr + "-" + viewEndAddr + ")"
						);
			}

			if (mCurrentCachedTextBlock != null) {
				mCurrentCachedTextBlock.dispose();
				mCurrentCachedTextBlock = null;
			}

			mCurrentCachedTextBlock = readAsTextBlock(mBufferStartAddr, mBufferEndAddr);

		}

		// Process to make up pretty lines
		// Try not to break the read sentence of the text,
		// find the line feed position and display it line by line
		{
			// Retrieve the character string in the specified section(from
			// address to address)
			final String textForView = mCurrentCachedTextBlock.getStringBetweenAdress(viewStartAddr, viewEndAddr);

			// [NOTE]
			// Carriage Return (CR) (return, 0x0D) \r
			// Line Feed (LF) (line feed, 0x0A) \n

			if (mFileLineFeedCode == null || AppDef.TextArea.SHOW_PRETTY_SENTENCES == false) {
				// When the line feed code of this file can not be detected

				// Skip finding the line feed position and skip making up pretty
				// lines to display
				refText2DCache.text = textForView;
				refText2DCache.prefferedViewStartAddr = viewStartAddr;
				refText2DCache.prefferedViewEndAddr = viewEndAddr;

			} else {

				// When a line feed code of a file can be detected

				// First line feed in read text
				final int firstPositionOfNewLine = textForView.indexOf(mFileLineFeedCode);

				// Last line feed in read text
				final int lastPositionOfNewLine = textForView.lastIndexOf(mFileLineFeedCode);

				int cutStartIndex = firstPositionOfNewLine + 1;
				int cutEndIndex = lastPositionOfNewLine;

				if (viewStartAddr == 0) {

					// If looking at the beginning of the file,
					// Make sure it display from the very beginning.
					// prevent from being displayed from the second line.
					cutStartIndex = 0;
				}

				if (viewEndAddr == mFileSize - 1) {
					LOGGER.fine("This view can show the end of the text file.");

					cutEndIndex = textForView.length();
				}

				// Extract from line feed to line feed.
				// Furthermore, replace the
				// line feed code so that it is properly displayed in the text
				// area.
				// (Remember ,#indexof contains the starting index but does not
				// include the
				// value specified in the ending index)
				final String textForPrettyView = textForView.substring(cutStartIndex, cutEndIndex);

				final int viewStartIndex = mCurrentCachedTextBlock.getStringIndexFromAddress(viewStartAddr);

				final long cutStartAddr = mCurrentCachedTextBlock.getAddressFromStringIndex(cutStartIndex + viewStartIndex);
				final long cutEndAddr = mCurrentCachedTextBlock.getAddressFromStringIndex(cutEndIndex - 1 + viewStartIndex);

				refText2DCache.text = textForPrettyView;
				refText2DCache.prefferedViewStartAddr = cutStartAddr;
				refText2DCache.prefferedViewEndAddr = cutEndAddr;

				if (LOGGER.isLoggable(Level.FINE)) {
					refText2DCache._firstLine = getFirstLine(textForPrettyView);
					refText2DCache._lastLine = getLastLine(textForPrettyView);
					refText2DCache._numOfLines = textForPrettyView.split(mFileLineFeedCode).length;
				}

			}
		}

	}

	/**
	 * Returns whether or not the currently wrapping file is a text file
	 * 
	 * @return
	 */
	public boolean isTextFile() {
		return mIsTextFile;
	}

	/**
	 * Returns file size
	 * 
	 * @return
	 */
	public long getFileSize() {
		return mFileSize;
	}

	/**
	 * Returns the last address of the file
	 * 
	 * @return
	 */
	public long getFileEndAddress() {
		return getFileSize() - 1;
	}

	/**
	 * Return the currently cached block
	 * 
	 * @return
	 */
	public TextBlock getTextBlock() {
		return mCurrentCachedTextBlock;
	}

	/**
	 * Read specified range as text
	 * 
	 * @param startAddr
	 *            starting address
	 * @param endAddr
	 *            ending address
	 * @return
	 */
	private TextBlock readAsTextBlock(long startAddr, long endAddr) {

		if (mIsTextFile == false) {
			throw new RuntimeException("This file isn't a text file.");
		}

		final byte[] srcBytes = mFilePartialReader.read(mSrcFile, startAddr, endAddr);

		LOGGER.fine("read area=(" + startAddr + "-" + endAddr + " srcBytes.size=" + srcBytes.length + ")");

		final TextBlock textBlock = new TextBlock(startAddr, srcBytes, mEncoding);
		return textBlock;

	}

	/**
	 * Get the first line of the string
	 * 
	 * @param str
	 * @return
	 */
	private String getFirstLine(String str) {
		int lfIndex = str.indexOf(mFileLineFeedCode);
		return str.substring(0, lfIndex);
	}

	/**
	 * Get the last line of the string
	 * 
	 * @param str
	 * @return
	 */
	private String getLastLine(String str) {

		final int indexOfLineFeedCode = str.lastIndexOf(mFileLineFeedCode);

		if (indexOfLineFeedCode == str.length() - mFileLineFeedCode.length()) {
			String str2 = str.substring(0, indexOfLineFeedCode);
			int lfIndex2 = str2.lastIndexOf(mFileLineFeedCode);
			return str.substring(lfIndex2 + 1, str2.length());
		}

		return str.substring(indexOfLineFeedCode + 1, str.length());
	}

	/**
	 * Release memory
	 */
	@Override
	public void dispose() {
		// make sure release memory
		if (mCurrentCachedTextBlock != null) {
			mCurrentCachedTextBlock.dispose();
			mCurrentCachedTextBlock = null;
		}

	}

}
