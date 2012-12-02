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
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.plaf.ScrollBarUI;

/**
 * VerticalSeekBarUI just inherits BasicScrollBarUI.
 * <p>
 * Except that it is exactly the same as {@link VerticalSeekBar4Windows} <br>
 * <p>
 * For now,<br>
 * Since this class is subordinate to {@link VerticalSeekBar4Windows}, so
 * changing this class first is prohibited.make share always change
 * {@link VerticalSeekBar4Windows}first.
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 * 
 */
@SuppressWarnings("serial")
public class VerticalSeekBar4Common extends VerticalSeekBar {

	private static final Logger LOGGER = Logger.getLogger(VerticalSeekBar4Common.class.getName());

	@Override
	public ScrollBarUI createUI() {
		return new VerticalSeekBarUI();
	}

	class VerticalSeekBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {

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

}