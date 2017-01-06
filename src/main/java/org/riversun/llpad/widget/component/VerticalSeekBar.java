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

/**
 * Vertical scroll bar that can handle long type value.
 * <p>
 * Calling setValue does not change the position of the scroll bar's thumb. <br>
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 * 
 */
@SuppressWarnings("serial")
public class VerticalSeekBar extends JScrollBar {

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

	private AdjustmentListener mSafeAdjustmentListener;
	private TrackAreaEventListener mTrackAreaEventListener;
	private VScrollBarEvent mVScrollBarEvent;

	private long mMaxLongValue = 0;
	private long mLongValue = 0;
	private boolean mIsThumbDragging = false;
	private int mMaxIntValue = DEFAULT_MAX_INT_VALUE;

	public VerticalSeekBar(int orientation) {

		super(orientation);

		setEnabled(false);
		setUI(new VerticalSeekBarUI());
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

	/**
	 * TODO for general use,use javax.swing.plaf.basic.BasicScrollBarUI
	 *
	 */
	private class VerticalSeekBarUI extends com.sun.java.swing.plaf.windows.WindowsScrollBarUI {

		@Override
		protected ArrowButtonListener createArrowButtonListener() {

			return new _ArrowButtonListener();
		}

		private class _ArrowButtonListener extends ArrowButtonListener {

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {

				if (mTrackAreaEventListener != null) {

					TrackClickEventType trackType = null;

					if (e.getSource() == incrButton) {
						// down arrow clicked
						trackType = TrackClickEventType.ARROW_DOWN;

						LOGGER.finer("seekbar arrow button clicked " + trackType);
					}
					else if (e.getSource() == decrButton) {
						// up arrow clicked
						trackType = TrackClickEventType.ARROW_UP;

						LOGGER.finer("seekbar arrow button clicked " + trackType);
					}

					mTrackAreaEventListener.onTrackClicked(trackType);
				}

			}

		}

		@Override
		protected TrackListener createTrackListener() {

			return new _TrackListener();
		}

		private class _TrackListener extends javax.swing.plaf.basic.BasicScrollBarUI.TrackListener {
			private int mPrevValue = -1;
			private long mTravelDistance = 0;

			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				LOGGER.fine("mousePressed THUMB-drag finished intValue=" + getValue() + "/" + mMaxIntValue);

				// Rectanble of the Knob
				final Rectangle knobBounds = getThumbBounds();

				final boolean isMouseCursorInsideKnob = knobBounds.contains(currentMouseX, currentMouseY);

				if (isMouseCursorInsideKnob) {
					// When grasping the knob
					mIsThumbDragging = true;
					mPrevValue = getValue();
				} else {
					// When clicking the track area
					mIsThumbDragging = false;

					// TODO
					// May add handling for repeating of track area scroll when
					// long pressing.

				}

			}

			@Override
			public void mouseDragged(MouseEvent e) {
				super.mouseDragged(e);

				if (mIsThumbDragging) {

					mTravelDistance++;

					if (mSafeAdjustmentListener != null) {

						LOGGER.finer("seekbar THUMB-drag adjusting intValue=" + getValue() + "/" + mMaxIntValue);
						mSafeAdjustmentListener.adjustmentValueChanged(null);

					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {

				super.mouseReleased(e);

				if (mIsThumbDragging) {

					final boolean isTraveled = (mTravelDistance > 0);

					mTravelDistance = 0;

					// When grasping the knob and moving it (dragging)
					mIsThumbDragging = false;

					if (mSafeAdjustmentListener != null) {
						final int value = getValue();
						if (mPrevValue != value || isTraveled) {
							LOGGER.fine("seekbar THUMB-drag finished intValue=" + getValue() + "/" + mMaxIntValue);

							mSafeAdjustmentListener.adjustmentValueChanged(null);
						} else {
							LOGGER.finer("seekbar THUMB-drag canceled.");
						}
					}
					return;

				} else {

					// Rectanble of the Knob
					final Rectangle knobBounds = getThumbBounds();

					final int currentMouseY = e.getY();

					// When not knobing knob
					final boolean trackUpClicked = currentMouseY < knobBounds.y;
					final boolean trackDownClicked = currentMouseY > knobBounds.y + knobBounds.height;

					TrackClickEventType trackType = null;

					if (trackUpClicked) {
						trackType = TrackClickEventType.TRACK_UP;
						LOGGER.finer("seekbar track area clicked " + trackType);

					} else if (trackDownClicked) {
						trackType = TrackClickEventType.TRACK_DOWN;
						LOGGER.finer("seekbar track area clicked " + trackType);
					}

					if (mTrackAreaEventListener != null) {
						mTrackAreaEventListener.onTrackClicked(trackType);
					}
				}
			}

		}

	}

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