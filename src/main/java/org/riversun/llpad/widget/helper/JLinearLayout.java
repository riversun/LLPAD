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
package org.riversun.llpad.widget.helper;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * LinearLayout like component for Pure java GUI component like AWT/Swing.
 *
 * You can put the components of a pure java as Android's LinearLayout-like
 * style.
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 */
@SuppressWarnings("serial")
public class JLinearLayout extends Component {

	public enum Orientation {
		HORIZONTAL, VERTICAL,
	}

	private JPanel mBasePanel = new JPanel();

	private Orientation mOrientation = Orientation.VERTICAL;
	private List<ComponentHolder> mChildViewList = new ArrayList<ComponentHolder>();

	private class ComponentHolder {
		public Component component;
		public double weight;
	}

	public JLinearLayout() {
		mBasePanel.setBackground(Color.black);
		mBasePanel.setOpaque(true);
	}

	@Override
	public void setBackground(Color c) {
		super.setBackground(c);
		mBasePanel.setBackground(c);

	}

	public JLinearLayout setChildOrientation(Orientation orientation) {
		this.mOrientation = orientation;
		return JLinearLayout.this;
	}

	/**
	 * Add view(component) to this LayoutGroup
	 * 
	 * @param component
	 * @return
	 */
	public JLinearLayout addView(Component component) {
		addView(component, 1.0d);
		return JLinearLayout.this;
	}

	/**
	 * Add view(component) to this LayoutGroup
	 * 
	 * @param component
	 * @param weight
	 * @return
	 */
	public JLinearLayout addView(Component component, double weight) {

		final ComponentHolder compontentHolder = new ComponentHolder();
		compontentHolder.component = component;
		compontentHolder.weight = weight;

		mChildViewList.add(compontentHolder);

		return JLinearLayout.this;
	}

	/**
	 * Set visible
	 * 
	 * @param visible
	 * @return
	 */
	public JLinearLayout setVisibility(boolean visible) {
		mBasePanel.setVisible(visible);
		return JLinearLayout.this;
	}

	public JLinearLayout insertToFrame(JFrame frame) {
		frame.add(getAsPanel());
		return JLinearLayout.this;
	}

	/**
	 * Get this layout group as a JPanel
	 * 
	 * @return
	 */
	public JPanel getAsPanel() {

		final int countOfChildObject = mChildViewList.size();

		final GridBagLayout layout = new GridBagLayout();

		if (mOrientation == Orientation.VERTICAL) {
			mBasePanel.setLayout(layout);
		} else {
			mBasePanel.setLayout(layout);
		}

		for (int i = 0; i < countOfChildObject; i++) {

			ComponentHolder childComponentHolder = mChildViewList.get(i);
			final Component childComponent = childComponentHolder.component;
			final double childComponentWeight = childComponentHolder.weight;

			final GridBagConstraints gbc = new GridBagConstraints();

			if (mOrientation == Orientation.VERTICAL) {
				gbc.gridx = 0;
				gbc.gridy = i;
				gbc.weightx = 1.0d;
				gbc.weighty = childComponentWeight;
			} else {
				gbc.gridx = i;
				gbc.gridy = 0;
				gbc.weightx = childComponentWeight;
				gbc.weighty = 1.0d;
			}

			gbc.gridwidth = 1;
			gbc.gridheight = 1;

			gbc.fill = GridBagConstraints.BOTH;

			if (!(childComponent instanceof JLinearLayout)) {
				// If child component is Swing or AWT component
				layout.setConstraints(childComponent, gbc);
				mBasePanel.add(childComponent);

			} else {
				// If child component is JLayoutGroup

				final JLinearLayout childLayoutGroup = (JLinearLayout) childComponent;

				gbc.gridwidth = 1;
				gbc.gridheight = 1;
				gbc.weightx = 1.0d;
				gbc.weighty = 1.0d;

				if (mOrientation == Orientation.VERTICAL) {
					gbc.weighty = childComponentWeight;// childLayoutGroup.mWeight;
				} else {
					gbc.weightx = childComponentWeight;// childLayoutGroup.mWeight;
				}

				// Set weight to the panel that becomes base of the panel
				layout.setConstraints(childLayoutGroup.mBasePanel, gbc);

				JPanel childPanel = childLayoutGroup.getAsPanel();

				mBasePanel.add(childPanel);
			}
		}
		return mBasePanel;
	}

}