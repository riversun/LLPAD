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
package org.riversun.llpad;

/**
 * Application Configurations
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 *
 */
public class AppDef {

	public static class DEBUG {

		/**
		 * Whether to enable logging
		 */
		public static final boolean LOGGING = false;
	}

	public static class Common {

		public static final String RESOURCE_BUNDLE = "org.riversun.llpad.res.values.String";
	}

	public static class TextBuffer {

		/**
		 * The size of the buffer to be read and cached at one time from the
		 * file
		 * <p>
		 * If this value is set too large, it takes time to read even if using
		 * NIO.
		 */
		public static final int BUFFER_SIZE_BYTES = 60 * 1024;

		/**
		 * The size of the bytes if the difference between the starting address
		 * of the buffer(or the end of the buffer) and the current display
		 * starting address (or ending address) is less than this value
		 * {@link #BUFFER_WINDOW_SIZE_BYTES}, do read from the file
		 * additionally.
		 */
		public static final int BUFFER_WINDOW_SIZE_BYTES = 10 * 1024;
	}

	public static class TextViewRange {

		/**
		 * The size(as bytes) of the text to display in the textArea at one
		 * time. App reads a part of the file into the buffer,
		 * {@link #VIEW_AREA_SIZE_BYTES} is part of the buffer.
		 */
		public static final int VIEW_AREA_SIZE_BYTES = 20 * 1024;

		/**
		 * When the caret reaches the end of the text area, the display area is
		 * incremented by {@link #PAGE_INCREMENT_SIZE_BYTES}
		 * <p>
		 * If this value is too small, it will fail to detect the beginning of
		 * sentences for pretty formatting, and the same text will always be
		 * displayed when you try to scroll the view
		 * <p>
		 * This size should be at most about 1/3 of the
		 * {@link #VIEW_AREA_SIZE_BYTES} <br>
		 * 
		 */
		public static final int PAGE_INCREMENT_SIZE_BYTES = 4 * 1024;
	}

	public static class TextArea {

		/**
		 * Number of lines to scroll by Page-Up/Page-Down key.
		 */
		public static final int NUM_OF_LINES_INCREMENTED_BY_PAGE_KEY = 5;

		/**
		 * Base number of lines to scroll by mouse wheel.
		 */
		public static final int NUM_OF_LINES_INCREMENTED_BY_MOUSE_WHEEL = 3;

		/**
		 * Whether or not to scroll the knobs on the seek bar by key repeating
		 */
		public static final boolean UPDATE_SEEK_BAR_WHEN_KEY_REPEATING = true;

		/**
		 * Whether to enable the function to extract the sentence from the
		 * beginning so that the sentence does not start from the middle to
		 * display text beautifully.<br>
		 * <br>
		 * <br>
		 * Since the area specified by byte unit is displayed, the extracted
		 * portion may be in the middle of the sentence. <br>
		 * <br>
		 * By detecting a line feed, in order to make the text easier to read,
		 * it is possible to display from the beginning of the sentence rather
		 * than from the middle of the sentence.<br>
		 * <br>
		 * <br>
		 * However, if {@link AppDef.TextViewRange#PAGE_INCREMENT_SIZE_BYTES} is
		 * too small, you know, if the length of one sentence in a specified
		 * text file is longer than
		 * {@link AppDef.TextViewRange#PAGE_INCREMENT_SIZE_BYTES}, if you try to
		 * flip the page, you can not detect line feed in the sentence ,and,as a
		 * result, the page can not be flipped.<br>
		 * Therefore, it is necessary to set
		 * {@link AppDef.TextViewRange#PAGE_INCREMENT_SIZE_BYTES} to be
		 * sufficiently larger than the length of one sentence.
		 */
		public static final boolean SHOW_PRETTY_SENTENCES = true;
	}

	public static class SeekBar {

		/**
		 * Number of lines to scroll when clicking track area<br>
		 * Track area are the upper side and the lower side of the knob on the
		 * vertical scroll bar
		 */
		public static final int NUM_OF_LINES_INCREMENTED_BY_TRACK_CLICK = 10;

		/**
		 * The number of lines to scroll when clicking the arrows at the top and
		 * bottom of the vertical scroll bar
		 */
		public static final int NUM_OF_LINES_INCREMENTED_BY_ARROW_CLICK = 1;

		/**
		 * Whether or not to read the text and update the textArea in real time
		 * while moving(dragging) the knob
		 */
		public static final boolean SEEK_WHEN_ADJUSTING_WITH_SEEKBAR = true;

	}

	public static class Window {
		public static final int DEFAULT_WINDOW_WIDTH_PX = 800;
		public static final int DEFAULT_WINDOW_HEIGHT_PX = 600;

	}
}
