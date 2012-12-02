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
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.JScrollBar;
import javax.swing.plaf.ScrollBarUI;

/**
 * Vertical scroll bar that can handle long type value.
 * <p>
 * Calling setValue does not change the position of the scroll bar's thumb. <br>
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 * 
 */
@SuppressWarnings("serial")
public abstract class VerticalSeekBar extends JScrollBar {

	private static final Logger LOGGER = Logger.getLogger(VerticalSeekBar.class.getName());

	public enum TrackClickEventType {
		/**
		 * Clicked the upper area of the knob
		 */
		TRACK_UP,
		/**
		 * Clicked the lower area of the knob
		 */
		TRACK_DOWN,
		/**
		 * Clicked up-arrow
		 */
		ARROW_UP,
		/**
		 * Clicked down-arrow
		 */
		ARROW_DOWN
	}

	public static interface TrackAreaEventListener {
		/**
		 * Called when track area is clicked
		 * 
		 * @param position
		 */
		public void onTrackClicked(TrackClickEventType position);
	}

	public static interface VScrollBarEvent {

		/**
		 * Returns when value changed
		 * 
		 * @param adjusting
		 *            true if this is one of multiple adjustment events.
		 * @param longValue
		 *            current value in the adjustment event.
		 */
		public void onLongValueChanged(boolean adjusting, long longValue);
	}

	private static final int DEFAULT_MAX_INT_VALUE = Integer.MAX_VALUE;
	private static final int DEFAULT_MIN_INT_VALUE = 0;

	protected AdjustmentListener mSafeAdjustmentListener;
	protected TrackAreaEventListener mTrackAreaEventListener;
	protected VScrollBarEvent mVScrollBarEvent;

	protected long mMaxLongValue = 0;
	protected long mLongValue = 0;
	protected boolean mIsThumbDragging = false;
	protected int mMaxIntValue = DEFAULT_MAX_INT_VALUE;

	public VerticalSeekBar() {

		super(JScrollBar.VERTICAL);

		setEnabled(false);
		setUI(createUI());
		setValue(0);
		setBlockIncrement(0);
		setUnitIncrement(0);

		super.setMaximum((int) mMaxIntValue);
		super.setMinimum((int) DEFAULT_MIN_INT_VALUE);

		// If you set JScrollBar#setValue from the code,
		// you just want to change the position of the bar.
		// but if you've set a listener via #addAdjustmentListener ,it will
		// cause #adjustmentValueChanged to
		// be called every time you call #setValue. It is inconvenient.
		// Therefore, I created a mechanism to call Adjustment Listener.
		// And named #setSafeAdjustmentListener.
		setSafeAdjustmentListener(new AdjustmentListener() {

			private int mPrevIntValue = -1;
			private boolean mPrevValueIsAdjusting = false;

			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {

				final boolean valueIsAdjusting = getValueIsAdjusting();
				final int intValue = getValue();

				if (mPrevIntValue == intValue && mPrevValueIsAdjusting == valueIsAdjusting) {
					// If the value does not change, do nothing
					// For example when you click on a track area
					return;
				}

				final double ratio = (double) mMaxLongValue / (double) mMaxIntValue;
				final long longValue = (long) ((double) intValue * ratio);

				mLongValue = longValue;

				LOGGER.finer("seekbar value changed mPrevIntValue=" + mPrevIntValue + " intValue=" + intValue + " longValue=" + longValue + " valueIsAdjusting=" + valueIsAdjusting);

				mPrevIntValue = intValue;
				mPrevValueIsAdjusting = valueIsAdjusting;

				if (mVScrollBarEvent != null) {
					mVScrollBarEvent.onLongValueChanged(valueIsAdjusting, longValue);
				}
			}
		});

	}

	/**
	 * Set the maximum integer value<br>
	 * taking the height of the knob into consideration
	 * 
	 * @param maxIntValue
	 */
	public void setMaxIntValue(int maxIntValue) {
		mMaxIntValue = maxIntValue;

		final int totalValue = (int) mMaxIntValue + getExtent();
		super.setMaximum(totalValue);
	}

	/**
	 * Set the maximum long value<br>
	 * 
	 * @param maxLongValue
	 */
	public void setMaxLongValue(long maxLongValue) {
		mMaxLongValue = maxLongValue;
	}

	/**
	 * If you set JScrollBar#setValue from the code, you just want to change the
	 * position of the bar. but if you've set a listener via
	 * #addAdjustmentListener ,it will cause #adjustmentValueChanged to be
	 * called every time you call #setValue. It is inconvenient. Therefore, I
	 * created a mechanism to call Adjustment Listener. And named
	 * #setSafeAdjustmentListener.
	 * 
	 * @param adjustmentListener
	 */
	public void setSafeAdjustmentListener(AdjustmentListener adjustmentListener) {
		mSafeAdjustmentListener = adjustmentListener;
	}

	/**
	 * Returns true when adjusting
	 */
	@Override
	public boolean getValueIsAdjusting() {
		return mIsThumbDragging;
	}

	public void setOnTrackAreaClickedListener(TrackAreaEventListener listener) {
		mTrackAreaEventListener = listener;
	}

	public void setVScrollBarEvent(VScrollBarEvent listener) {
		mVScrollBarEvent = listener;
	}

	public void setLongValue(long longValue) {
		mLongValue = longValue;
		final double ratio = (double) mMaxIntValue / (double) mMaxLongValue;
		final int value = (int) ((double) mLongValue * ratio);
		setValue(value);
	}

	@Override
	public void setValue(int value) {
		super.setValue(value);
	}

	public abstract ScrollBarUI createUI();

	/**
	 * Returns the height of the knob
	 * 
	 * @return
	 */
	public int getExtent() {
		return getVisibleAmount();
	}

	@Deprecated
	@Override
	public void setMaximum(int value) {
		throw new RuntimeException("Not supported.");
	}

	@Deprecated
	@Override
	public void setMinimum(int value) {
		throw new RuntimeException("Not supported.");
	}

}