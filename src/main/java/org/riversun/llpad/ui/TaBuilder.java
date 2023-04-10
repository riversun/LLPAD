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

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.logging.Logger;

import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;
import javax.swing.text.StyledDocument;

import org.riversun.llpad.AppDef;
import org.riversun.llpad.fw.Disposable;
import org.riversun.llpad.widget.component.DiagTextArea;

/**
 * TextArea Builder
 * <p>
 * Initialize etxtArea,Set events of textArea,Receive raw events from
 * textArea(like KeyListener,MouseListener,MouseWheelListener) and interpret it
 * and fire an abstracted event.
 *
 * @author Tom Misawa (riversun.org@gmail.com)
 */
public class TaBuilder implements Disposable {

  private static final Logger LOGGER = Logger.getLogger(TaBuilder.class.getName());

  public enum CaretDir {
    LEFT, UP, RIGHT, DOWN
  }

  public static interface JTextAreaEventListener {
    public void onCaretMovingStarted(CaretDir dir, int caretIndex, int caretRow);

    public void onCaretMovingFinished(int caretIndex, int caretRow);
  }

  private final DiagTextArea mTextArea;

  private JTextAreaEventListener mTextAreaEventListener;
  private KeyListener mKeyListener = null;
  private MouseListener mMouseListener = null;
  private MouseWheelListener mMouseWheelListener = null;

  public TaBuilder(DiagTextArea textArea) {

    LOGGER.fine("initialize " + this);

    mTextArea = textArea;
    buildTextArea();
  }

  public void setJTextAreaEvent(JTextAreaEventListener listener) {
    mTextAreaEventListener = listener;
  }

  private void buildTextArea() {

    LOGGER.fine("initialize text area");

    // TODO font handling
    // mTextArea.setFont());
    mTextArea.setForeground(UIManager.getColor("Label.foreground"));
    mTextArea.setBackground(Color.white);
    
    if(AppDef.TextArea.USE_JTEXT_PANE) {
      // mTextArea.setLineWrap(true);//  Enable line wrap in JTextArea      
    }else {
      
    }

  
    mTextArea.setOpaque(false);
    mTextArea.setEditable(false);
    mTextArea.setFocusable(true);
    mTextArea.getCaret().setVisible(true);
    mTextArea.setAutoscrolls(false);

    /*
     * If you do not call #setBorder(null), even if you try to scroll
     * JTextArea to the top, the upper left coordinate of visibleRect
     * becomes like (0,2) not (0,0). Since the upper left coordinate does
     * not become (0, 0), it is impossible to scroll to the top.
     */
    mTextArea.setBorder(null);

    mKeyListener = new KeyListener() {

      @Override
      public void keyPressed(KeyEvent e) {

        final int caretIndex = mTextArea.getCaretIndex();
        final int caretRowIndex = mTextArea.getRowIndexAt(caretIndex);

        CaretDir dir = null;

        if (mTextAreaEventListener != null) {

          final int keyCode = e.getKeyCode();

          if (keyCode == KeyEvent.VK_KP_UP || keyCode == KeyEvent.VK_UP) {
            dir = CaretDir.UP;
          } else if (keyCode == KeyEvent.VK_PAGE_UP) {
            // clear current key event
            e.consume();
            mTextArea.scrollTextLine(-AppDef.TextArea.NUM_OF_LINES_INCREMENTED_BY_PAGE_KEY);
            return;
          } else if (keyCode == KeyEvent.VK_KP_LEFT || keyCode == KeyEvent.VK_LEFT) {
            dir = CaretDir.LEFT;
          } else if (keyCode == KeyEvent.VK_KP_RIGHT || keyCode == KeyEvent.VK_RIGHT) {
            dir = CaretDir.RIGHT;
          } else if (keyCode == KeyEvent.VK_KP_DOWN || keyCode == KeyEvent.VK_DOWN) {

            dir = CaretDir.DOWN;

          } else if (keyCode == KeyEvent.VK_PAGE_DOWN) {
            /*
             * If you released the PageUp Key or PageDown key,
             * keyReleased event will occur. When the keyReleased
             * event occurs, #doHandleCaretMovingFinish is called
             * and #doHandleCaretMovingFinish will update the value
             * of vertical seekbar.
             */
            dir = CaretDir.DOWN;

            // clear current key event
            e.consume();
            mTextArea.scrollTextLine(AppDef.TextArea.NUM_OF_LINES_INCREMENTED_BY_PAGE_KEY);
            return;
          } else if (keyCode == KeyEvent.VK_ENTER) {
            mTextArea.getFirstCaretIndexOfCurrentVisible();
          }

          mTextAreaEventListener.onCaretMovingStarted(dir, caretIndex, caretRowIndex);
        }

      }

      @Override
      public void keyReleased(KeyEvent e) {

        doHandleCaretMovingFinish();

      }

      @Override
      public void keyTyped(KeyEvent e) {
      }

    };
    mTextArea.addKeyListener(mKeyListener);

    mMouseListener = new MouseListener() {
      @Override
      public void mouseClicked(MouseEvent e) {

        /*
         * Since the cursor disappears when you return from another
         * app's window. So, explicitly display the cursor again.
         */
        mTextArea.showCaret();
      }

      @Override
      public void mouseReleased(MouseEvent e) {
      }

      @Override
      public void mousePressed(MouseEvent e) {

        doHandleCaretMovingFinish();
      }

      @Override
      public void mouseExited(MouseEvent e) {
      }

      @Override
      public void mouseEntered(MouseEvent e) {
      }

    };

    mTextArea.addMouseListener(mMouseListener);

    mMouseWheelListener = new MouseWheelListener() {

      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {

        final int notches = e.getWheelRotation();

        if (notches < 0) {

          final int numOfScrollLines = -AppDef.TextArea.NUM_OF_LINES_INCREMENTED_BY_MOUSE_WHEEL * Math.abs(notches);
          LOGGER.finer("notches=" + notches + " SCROLL UP numOfScrollLines=" + numOfScrollLines + " @ " + TaBuilder.this);

          mTextArea.scrollTextLine(numOfScrollLines);
          LOGGER.finer("call doHandleCaretMovingFinish");

          doHandleCaretMovingFinish();
        } else if (notches > 0) {
          final int numOfScrollLines = AppDef.TextArea.NUM_OF_LINES_INCREMENTED_BY_MOUSE_WHEEL * Math.abs(notches);
          LOGGER.finer("notches=" + notches + " SCROLL DOWN numOfScrollLines=" + numOfScrollLines + " @ " + TaBuilder.this);

          mTextArea.scrollTextLine(numOfScrollLines);
          LOGGER.finer("call doHandleCaretMovingFinish");
          doHandleCaretMovingFinish();
        }

      }
    };
    mTextArea.addMouseWheelListener(mMouseWheelListener);
  }

  private void doHandleCaretMovingFinish() {

    final int caretIndex = mTextArea.getCaretIndex();
    final int caretRowIndex = mTextArea.getRowIndexAt(caretIndex);

    LOGGER.finer("#doHandleCaretMovingFinish caretIndex=" + caretIndex + " caretRowIndex=" + caretRowIndex);

    if (mTextAreaEventListener != null) {
      mTextAreaEventListener.onCaretMovingFinished(caretIndex, caretRowIndex);
    }
  }

  @Override
  public void dispose() {
    mTextAreaEventListener = null;

    if (mKeyListener != null) {
      mTextArea.removeKeyListener(mKeyListener);
      mKeyListener = null;
    }

    if (mMouseListener != null) {
      mTextArea.removeMouseListener(mMouseListener);
      mMouseListener = null;
    }
    if (mMouseWheelListener != null) {
      mTextArea.removeMouseWheelListener(mMouseWheelListener);
      mMouseWheelListener = null;
    }

  }
}
