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

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.riversun.llpad.ui.GUIBuilder;
import org.riversun.llpad.ui.GUIFileOpenHandler;
import org.riversun.llpad.ui.GUIFileOpenHandler.FileSelectionListener;
import org.riversun.llpad.ui.TaViewRangeManager;
import org.riversun.llpad.util.file_buffer.TextFileBufferedWrapper;
import org.riversun.llpad.widget.helper.EDTHandler;

/**
 * Entry point of LLPAD
 * 
 * Add VM Option to "--add-exports
 * java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED" when you use on
 * OpenJDK
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 */
public class AppMain {

  public static void main(String[] args) {

    try {

      if (AppDef.DEBUG.LOGGING == false) {
        LogManager.getLogManager().reset();
      } else {
        try {
          InputStream configStream = AppMain.class.getResourceAsStream("/logging.properties");
          LogManager.getLogManager().readConfiguration(configStream);
        } catch (IOException e) {
          System.err.println("Failed to load logging configuration: " + e.getMessage());
          e.printStackTrace();
        }
      }

      LOGGER.fine("Start the app");

      System.setProperty("jsse.enableSNIExtension", "false");

      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

      final AppMain app = new AppMain();
      final GUIBuilder gui = app.buildGUI();

      if (args != null && args.length == 1) {
        String path = args[0];
        File file = new File(path);
        if (file.exists() && file.isFile()) {
          app.openFile(gui, file, 0);
        }
      } else {
        // do nothing
      }
      
      if(true) {
        app.openFile(gui, new File("D:\\downloads\\jparacrawl_english_to_japanese\\Parallel_corpus\\en-ja\\en-ja.bicleaner05.txt"), 0);
      }

    } catch (Exception e) {
      // TODO show dialog
      LOGGER.log(Level.SEVERE, "Can not open the app.", e);
    }
  }

  private static final Logger LOGGER = Logger.getLogger(AppMain.class.getName());

  private final EDTHandler mHandler = new EDTHandler();
  private TextFileBufferedWrapper mFileWrapper;
  private TaViewRangeManager mTextAreaViewRangeManager;

  /**
   * Building an window with GUI components
   * 
   * @return
   */
  public GUIBuilder buildGUI() {

    final GUIBuilder gui = new GUIBuilder();

    final GUIFileOpenHandler fileOpenHandler = new GUIFileOpenHandler(gui.getGuiComponent());

    fileOpenHandler.setFileSelectionListener(new FileSelectionListener() {

      @Override
      public void onFileSelected(boolean isTextFile, File file) {
        if (isTextFile) {
          openFile(gui, file, 0);
        } else {
          // Display on the dialog informing that only text files are
          // supported now.

          final Component parent = gui.getGuiComponent().frame;

          JOptionPane.showMessageDialog(parent,
              R.getString(R.string.FileChooser_Msg__THIS_FILE_IS_NOT_A_TEXT_FILE, file.getName()),
              R.getString(R.string.FileChooser_Msg__COULD_NOT_OPEN_FILE),
              JOptionPane.WARNING_MESSAGE);
        }

      }

    });

    // Create a window on the UI thread
    mHandler.post(new Runnable() {
      public void run() {
        gui.createWindow();
      }
    });

    return gui;
  }

  /**
   * Open specified file and read contents
   * 
   * @param gui
   *                      GUI buider
   * @param file
   *                      target file
   * @param viewStartAddr
   *                      starting address of view
   */
  public void openFile(final GUIBuilder gui, final File file, final long viewStartAddr) {

    mHandler.post(new Runnable() {

      @Override
      public void run() {

        LOGGER.fine("gui=" + gui + " file=" + file + " viewStart=" + viewStartAddr);

        gui.initGui();

        gui.setFile(file);

        if (mFileWrapper != null) {
          mFileWrapper.dispose();
          mFileWrapper = null;
        }

        if (mTextAreaViewRangeManager != null) {
          mTextAreaViewRangeManager.dispose();
          mTextAreaViewRangeManager = null;
        }

        mFileWrapper = new TextFileBufferedWrapper(file, AppDef.TextBuffer.BUFFER_SIZE_BYTES, AppDef.TextBuffer.BUFFER_WINDOW_SIZE_BYTES);
        mTextAreaViewRangeManager = new TaViewRangeManager(mFileWrapper, gui.getGuiComponent(), viewStartAddr, AppDef.TextViewRange.VIEW_AREA_SIZE_BYTES,
            AppDef.TextViewRange.PAGE_INCREMENT_SIZE_BYTES);

        mTextAreaViewRangeManager.show();

      }
    });

  }
}