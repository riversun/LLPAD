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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextPane;

import org.riversun.llpad.widget.component.JTextAreaHelper.JTextAreaInfo;
import org.riversun.llpad.widget.component.JTextAreaHelper.JTextAreaRowInfo;
import org.riversun.llpad.widget.component.JTextAreaHelper.TextAreaVisibleArea;

/**
 * 
 * Diagnosable textArea for efficient editing and viewing.Fixed a bug in
 * JTextArea by continuously changing text
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 *
 */
@SuppressWarnings("serial")
//public class DiagTextArea extends JTextArea {
public class DiagTextArea extends JTextPane {
  private static final Logger LOGGER = Logger.getLogger(DiagTextArea.class.getName());

  /**
   * Direction of scroll
   */
  enum EScroll {
    TOP,
    BOTTOM
  }

  public static interface FileDropListener {
    /**
     * Determine if the file can be dropped
     * <p>
     * If returning false here, a prohibited mark is displayed when dropping
     * enter
     * 
     * @param fileList
     *                 files to be dropped
     * @return
     */
    public boolean canFileDrop(List<File> fileList);

    /**
     * Called when a file is dropped
     * 
     * @param fileList
     */
    public void onFileDrop(List<File> fileList);

  }

  private final TextAreaVisibleArea m_Ref_VisibleArea = new TextAreaVisibleArea();
  private final JTextAreaHelper mTextAreaUtil = new JTextAreaHelper();
  private final DropTarget mDropTarget = new DropTarget();

  private FileDropListener mFileDropListener;
  private DropTargetListener mDropTargetListener;
  private boolean mIsLineWrapping = false;
  private String mCrrText = null;
  private List<String> mHighlightingTextList = null;

  public DiagTextArea() {
    enableDrop();
  }

  /**
   * Set callback fired when file is dropped
   * 
   * @param listener
   */
  public void setFileDropListener(FileDropListener listener) {
    mFileDropListener = listener;
  }

  public int getCaretIndex() {
    return getCaretPosition();
  }

  /**
   * Display the text
   */
  @Override
  public void setText(String text) {
    // TODO Set a new document every time to avoid JTextAarea bugs
    mCrrText = text;
    setText(text, true);
  }

  /**
   * Display the text and update the current display area
   * 
   * @param text
   * @param updateCurrentVisibleArea
   */
  public void setText(String text, boolean updateCurrentVisibleArea) {
    super.setText(text);

    boolean isUseHTML = true;

    if (isUseHTML) {
      // - use html for partial text decoration
      this.setContentType("text/html");

      if (mHighlightingTextList != null) {
        for (int i = 0; i < mHighlightingTextList.size(); i++) {
          text = wrapTextWithSpan(text, mHighlightingTextList.get(i));
        }
      }

      if (mIsLineWrapping) {
        text = text.replaceAll("\\r\\n|\\n\\r|\\n|\r", "<br>");

        super.setText("<html>" + text + "</html>");
      } else {
        super.setText("<html><pre>" + text + "</pre></html>");
      }
    } else {

      // - use plain text
      super.setText(text);
    }

    if (updateCurrentVisibleArea) {
      getCurrentVisibleAreaAndUpdate();
    }

  }

  String wrapTextWithSpan(String input, String targetText) {
    String regex = Pattern.quote(targetText);
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(input);

    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(result, "<span style=\"color:red;font-weight:bold;\">" + matcher.group() + "</span>");
    }
    matcher.appendTail(result);

    return result.toString();
  }

  /**
   * Enable or disable line wrapping for the component.
   *
   * @param enabled If true, line wrapping is enabled; otherwise, it is disabled.
   */
  public void setLineWrappingEnabled(boolean enabled) {
    this.mIsLineWrapping = enabled;
    this.setText(mCrrText);
  }

  /**
   * Add the specified text to the highlighting list.
   * The component will highlight all occurrences of the specified text.
   *
   * @param text The text to be highlighted.
   */
  public void setHighlightingText(String text) {
    if (mHighlightingTextList == null) {
      mHighlightingTextList = new ArrayList<>();
    }
    mHighlightingTextList.add(text);
    this.setText(mCrrText);
  }

  /**
   * Clear the highlighting list.
   * The component will no longer highlight any text.
   */
  public void clearHighlightingText() {
    mHighlightingTextList = null;
    this.setText(mCrrText);
  }

  public TextAreaVisibleArea getCurrentVisibleAreaAndUpdate() {
    return getVisibleArea(true);
  }

  /**
   *
   * Returns text visible area
   * 
   * @param needUpdate
   *                   When setText is done, to fix the number of characters and
   *                   the
   *                   total number of lines displayed in the current textArea
   *                   When
   *                   scrolling occurs in the textArea, there are changing values
   *                   (for example, visible rows also change), So update textArea
   *                   again and specify whether to retrieve text and populate it
   *                   into the POJO again.
   * @return
   */
  private TextAreaVisibleArea getVisibleArea(boolean needUpdate) {

    if (needUpdate) {
      mTextAreaUtil.getCurrentVisibleArea(DiagTextArea.this, m_Ref_VisibleArea);
    }
    return m_Ref_VisibleArea;
  }

  public int getLastCaretIndexOfCurrentVisible() {

    final TextAreaVisibleArea visibleArea = getCurrentVisibleAreaAndUpdate();

    int visibleEndRowIndex = visibleArea.endRowIndex;

    JTextAreaInfo taInfo = m_Ref_VisibleArea.textAreaInfo;
    List<JTextAreaRowInfo> riList = taInfo.rowInfoList;

    // fail safe
    if (riList.size() - 1 < visibleEndRowIndex) {
      visibleEndRowIndex = riList.size();
    }

    JTextAreaRowInfo visibleAreaLastRowInfo = riList.get(visibleEndRowIndex);

    return visibleAreaLastRowInfo.endIndex;
  }

  public int getFirstCaretIndexOfCurrentVisible() {

    final TextAreaVisibleArea visibleArea = getCurrentVisibleAreaAndUpdate();

    int visibleStartRowIndex = visibleArea.startRowIndex;
    if (LOGGER.isLoggable(Level.FINER)) {
      LOGGER.finer("visibleArea=" + visibleArea);
    }
    // fail safe
    if (visibleStartRowIndex < 0) {
      visibleStartRowIndex = 0;
    }

    JTextAreaInfo taInfo = m_Ref_VisibleArea.textAreaInfo;
    List<JTextAreaRowInfo> riList = taInfo.rowInfoList;

    JTextAreaRowInfo visibleFirstRowInfo = riList.get(visibleStartRowIndex);
    LOGGER.finer("first row is visibleFirstRowInfo=" + visibleFirstRowInfo);
    return visibleFirstRowInfo.startIndex;
  }

  /**
   * Returns the index of the tail of the caret can be moved taking the text
   * stored in the current textArea into consideration,
   * 
   * @return
   */
  public int getMaxCaretIndex() {
    final JTextAreaInfo textAreaInfo = getVisibleArea(false).textAreaInfo;
    return textAreaInfo.maxCaretIndexOfThisTextArea;
  }

  /**
   * Put the caret at the bottom
   * 
   * @param caretIndex
   */
  public void putCaretAtTheBottom(int caretIndex) {
    setCaretPosition(0);
    setCaretPosition(caretIndex);
    scrollToTheTop();
  }

  /**
   * Move - the line where the specified caret is located - to the top
   * 
   * @param caretIndex
   */
  public void moveCaretLineToTheTop(int caretIndex) {
    scrollToTheBottom();
    setCaretPosition(0);
    setCaretPosition(caretIndex);
  }

  /**
   * Return the total number of rows in the current text area
   * 
   * @return
   */
  public int getNumOfRow() {
    final JTextAreaInfo textAreaInfo = getVisibleArea(false).textAreaInfo;
    return textAreaInfo.rowInfoList.size();
  }

  public void dragEnter(DropTargetDragEvent dtde) {

  }

  /**
   * Returns the line number of the specified caret position in index
   * 
   * @param caretIndex
   * @return
   */
  public int getRowIndexAt(int caretIndex) {
    return getVisibleArea(false).getRowIndexOfCarretInTextArea(caretIndex);
  }

  public int getVisibleRowEndIndex() {
    return getCurrentVisibleAreaAndUpdate().endRowIndex;
  }

  public int getVisibleRowStartIndex() {
    return getCurrentVisibleAreaAndUpdate().startRowIndex;
  }

  /**
   * Scroll the textArea to the top
   */
  public void scrollToTheTop() {
    scroll(EScroll.TOP);
  }

  /**
   * Scroll the textArea to the bottom
   */
  public void scrollToTheBottom() {
    scroll(EScroll.BOTTOM);
  }

  /**
   * Scroll the textArea by the specified number of lines
   * 
   * @param numOfLines
   */
  public void scrollTextLine(final int numOfLines) {

    if (numOfLines < 0) {
      LOGGER.finer("numOfLines=" + numOfLines + " move caret to the first caret pos.");

      setCaretPosition(getFirstCaretIndexOfCurrentVisible());

    } else if (numOfLines > 0) {
      LOGGER.finer("numOfLines=" + numOfLines + " move caret to the last caret pos.");
      setCaretPosition(getLastCaretIndexOfCurrentVisible());
    }

    final int loopCount;

    if (numOfLines < 0) {
      loopCount = Math.abs(numOfLines);
    } else if (numOfLines > 0) {
      loopCount = Math.abs(numOfLines);
    } else {
      loopCount = 0;
    }
    LOGGER.finer("loopCount=" + loopCount);

    for (int i = 0; i < (loopCount); i++) {
      if (numOfLines < 0) {
        dispatchVkey(KeyEvent.VK_UP);
      } else if (numOfLines > 0) {
        dispatchVkey(KeyEvent.VK_DOWN);
      }

    }
  }

  private void scroll(EScroll scroll) {
    final Rectangle visibleRect = getVisibleRect();
    final Rectangle bounds = getBounds();

    if (EScroll.TOP == scroll) {
      visibleRect.y = 0;
    } else {
      visibleRect.y = bounds.height - visibleRect.height;
    }

    scrollRectToVisible(visibleRect);
  }

  /**
   * Display caret even if it is read only
   */
  public void showCaret() {
    mTextAreaUtil.showCursor(DiagTextArea.this);
  }

  private void dispatchVkey(int keyCode) {
    dispatchEvent(new KeyEvent(DiagTextArea.this, KeyEvent.KEY_PRESSED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED));
    dispatchEvent(new KeyEvent(DiagTextArea.this, KeyEvent.KEY_RELEASED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED));
  }

  /**
   * Enable external file drop
   */
  private void enableDrop() {

    setDropTarget(mDropTarget);

    try {

      if (mDropTargetListener != null) {
        mDropTarget.removeDropTargetListener(mDropTargetListener);
        mDropTargetListener = null;
      }

      mDropTargetListener = new DropTargetListener() {
        @SuppressWarnings("unchecked")
        public void dragEnter(DropTargetDragEvent evt) {

          boolean objectDrappable = false;

          final Transferable t = evt.getTransferable();

          if (mFileDropListener != null) {
            List<File> fileList;

            try {
              fileList = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
              objectDrappable = mFileDropListener.canFileDrop(fileList);

            } catch (UnsupportedFlavorException | IOException ex) {
              objectDrappable = false;
            }
          }

          if (objectDrappable) {
            evt.acceptDrag(DnDConstants.ACTION_COPY);
          } else {
            evt.rejectDrag();
          }
        }

        public void drop(DropTargetDropEvent evt) {
          evt.acceptDrop(DnDConstants.ACTION_COPY);

          if (mFileDropListener != null) {
            try {
              Transferable transferble = evt.getTransferable();
              if (transferble.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                List<File> fileList = (List<File>) transferble.getTransferData(DataFlavor.javaFileListFlavor);
                mFileDropListener.onFileDrop(fileList);
              }
            } catch (UnsupportedFlavorException dex) {
            } catch (IOException dex) {
            }
          }
        }

        public void dropActionChanged(DropTargetDragEvent e) {
        }

        public void dragOver(DropTargetDragEvent e) {
        }

        public void dragExit(DropTargetEvent e) {
        }
      };
      mDropTarget.addDropTargetListener(mDropTargetListener);
    } catch (TooManyListenersException e) {
      LOGGER.log(Level.WARNING, "too many listeners.", e);
    }
  }

  @Deprecated
  @Override
  public int getCaretPosition() {
    return super.getCaretPosition();
  }
  // Since getVisibleRect is buggy,preparing to override
  // it.[begin]/////////////
  // @Override
  // public Rectangle getVisibleRect() {
  // Rectangle visibleRect = new Rectangle();
  //
  // computeVisibleRect(visibleRect);
  // return visibleRect;
  // }
  //
  // @Override
  // public void computeVisibleRect(Rectangle visibleRect) {
  // computeVisibleRectEx(this, visibleRect);
  // }
  //
  // private void computeVisibleRectEx(Component c, Rectangle visibleRect) {
  //
  // Container p = c.getParent();
  // Rectangle bounds = c.getBounds();
  //
  // if (p == null || p instanceof Window || p instanceof Applet) {
  // visibleRect.setBounds(0, 0, bounds.width, bounds.height);
  // } else {
  // computeVisibleRectEx(p, visibleRect);
  // visibleRect.x -= bounds.x;
  // visibleRect.y -= bounds.y;
  // computeIntersectionEx(0, 0, bounds.width, bounds.height, visibleRect);
  // }
  // }
  //
  // private Rectangle computeIntersectionEx(int x, int y, int width, int
  // height, Rectangle dest) {
  // int x1 = (x > dest.x) ? x : dest.x;
  // int x2 = ((x + width) < (dest.x + dest.width)) ? (x + width) : (dest.x +
  // dest.width);
  // int y1 = (y > dest.y) ? y : dest.y;
  // int y2 = ((y + height) < (dest.y + dest.height) ? (y + height) : (dest.y
  // + dest.height));
  //
  // dest.x = x1;
  // dest.y = y1;
  // dest.width = x2 - x1;
  // dest.height = y2 - y1;
  //
  // // If rectangles don't intersect, return zero'd intersection.
  // if (dest.width < 0 || dest.height < 0) {
  // dest.x = dest.y = dest.width = dest.height = 0;
  // }
  //
  // return dest;
  // }

  // Since getVisibleRect is buggy,preparing to override it.[end]/////////////
}
