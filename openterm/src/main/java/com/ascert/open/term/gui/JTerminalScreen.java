/*
 * Copyright (c) 2016, 2017 Ascert, LLC.
 * www.ascert.com
 *
 * Based on original code from FreeHost3270, copyright for derivations from original works remain:
 *  Copyright (C) 1998, 2001  Art Gillespie
 *  Copyright (2) 2005 the http://FreeHost3270.Sourceforge.net
 *
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ascert.open.term.gui;


import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import com.ascert.open.term.application.OpenTermConfig;
import com.ascert.open.term.core.AbstractTerminal.Page;

import com.ascert.open.term.core.TermChar;
import com.ascert.open.term.core.Terminal;
import com.ascert.open.term.core.TnAction;

import com.ascert.open.vnc.ScreenImageListener;
import com.ascert.open.vnc.Screen;

/**
 * A SWING component that interactively renders terminal screen contents and handles user to terminal interaction.
 *
 * <p>
 * The terminal is first rendered to a buffered image and then, whan a component repaint is requested this buffered image is painted on the
 * given component's graphics context. In case if a default font size is changed, the <code>JTerminalScreen</code> adjusts it size in order
 * to feed the grown terminal screen size.
 * </p>
 *
 * @see #paintComponent(Graphics)
 * @see #setFont(Font)
 * @since 0.1 RHPanel
 */
public class JTerminalScreen extends JPanel implements TnAction, Printable, Screen, MouseListener, MouseMotionListener
{
    private static final Logger log = Logger.getLogger(JTerminalScreen.class.getName());

    private static final AlphaComposite AC_50PCT = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
    private static final AlphaComposite AC_75PCT = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f);
    private static final AlphaComposite AC_OPAQUE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
    
    /**
     * The default size of the panel. Should be overriden upon panel initialization or font size change. TODO - need to optimise for common
     * screen sizes
     */
    public static final int DEFAULT_WIDTH_PER_COL = 10;
    public static final int DEFAULT_HEIGHT_PER_ROW = 25;

    private static final int MSG_CLOSED_REMOTE = 0;
    private static final int MSG_STRING = 1;
    private static final int MSG_BROADCAST = 2;

    public static final int MARGIN_LEFT = 5;
    public static final int MARGIN_TOP = 6;

    // These are really just stop-gap methods between using static defaults, and having a fully safe config model with 
    // bullet proof defaults
    
    public static int getFontSize()
    {
        return OpenTermConfig.getIntProp("font.size", 14);
    }
    
    public static String getFontName()
    {
        return OpenTermConfig.getProp("font.name", "Monospaced");
    }
    
    public static String getFgColor()
    {
        return OpenTermConfig.getProp("color.foreground", "Turquoise");
    }
    
    public static String getBgColor()
    {
        return OpenTermConfig.getProp("color.background", "Black");
    }
    
    public static String getBoldColor()
    {
        return OpenTermConfig.getProp("color.bold", "White");
    }
    
    public static String getCursorColor()
    {
        return OpenTermConfig.getProp("color.cursor", "Red");
    }
    
    
    public static Color getColor(String colorName)
    {
        // TODO - be much tidier as a map, this was quick and simple for now
        switch (colorName.toLowerCase())
        {
            case "black":       return Color.BLACK; 
            case "white":       return Color.WHITE;
            case "green":       return Color.GREEN;
            case "red":         return Color.RED;
            case "blue":        return Color.BLUE;
            case "orange":      return Color.ORANGE;
            case "turquoise":   return Color.CYAN;
            case "dark blue":   return new Color(0, 51, 102);
            case "light green": return new Color(204, 255, 204);
        }
        
        return null;
    }
        
    
    // buffer used to draw the screen in background.
    private BufferedImage frameBuff;

    private Color boldColor;
    private Color currentBGColor;
    private Color currentFGColor;
    private Color cursorColor;

    private Font font;
    private int fontSize;

    // graphics context of the background buffer. Use this context to
    // paint the components of the screen.
    private Graphics2D frame;
    private Terminal term;
    private KeyHandler kHandler;

    /**
     * Current status message.
     */
    //private String statusMessage;
    private String windowMessage;
    private boolean windowMsgOnScreen;
    private int messageNumber;
    private boolean tooManyConnections;

    private int char_ascent;
    private int char_height;
    private int char_width;
    private Point rectStartPoint;

    JToolBar toolbar;
    private boolean firstInit = true;

    private InputMap origInputMap;
    private ActionMap origActionMap;
    
    private boolean kbdEnabled = true;
    
    // Keyboad Lock listener handling - rather trivial implementation but workable for now
    protected final Set<ScreenImageListener> listeners = new CopyOnWriteArraySet<>();
    
    
    /**
     * Construct a new GUI session with a terminalModel of 2, and a terminalType of 3279-E.
     */
    public JTerminalScreen(Terminal term, JToolBar toolbar)
    {
        super();
        // We need a toolbar reference because F-Key names are terminal type sensitive, and hence
        // need initialising on new terminals
        this.toolbar = toolbar;

        fontSize = getFontSize();
        font = new Font(getFontName(), Font.PLAIN, fontSize);

        boldColor = getColor(getBoldColor());
        currentBGColor = getColor(getBgColor());
        currentFGColor = getColor(getFgColor());
        cursorColor = getColor(getCursorColor());
        
        origInputMap = this.getInputMap(WHEN_IN_FOCUSED_WINDOW);
        origActionMap = this.getActionMap();

        setTerminal(term);
    }

    public void setTerminal(Terminal term)
    {
        if (this.term != null)
        {
            term.disconnect();
            removeKeyListener(kHandler.getKeyListener());
            // Other cleanup?
        }

        this.term = term;
        if (this.term != null)
        {
            //TODO - probably better via a listener interface of some kind
            term.setClient(this);
            init();
        }
    }

    private void buildToolBar()
    {

        if (toolbar == null || kHandler == null)
        {
            return;
        }

        toolbar.removeAll();

        // Very basic approach for now. 
        // Really needs proper Bounds checks, handling for terms with only 1 button row etc.
        JButton[][] btnArr = kHandler.getFKeyButtons();
        GridLayout bl = new GridLayout(2, btnArr[0].length);
        toolbar.setLayout(bl);

        for (int ix = 0; ix < btnArr.length; ix++)
        {
            for (int jx = 0; jx < btnArr[ix].length; jx++)
            {
                toolbar.add(btnArr[ix][jx]);
            }
        }
    }

    void setKbdEnabled(boolean kbdEnabled)
    {
        //TODO - also need to disable mouse selection at some stage.
        //       it's a limited feature at present, and so probably non critical
        if (kbdEnabled)
        {
            addKeyListener(kHandler.getKeyListener());
            this.setInputMap(WHEN_IN_FOCUSED_WINDOW, kHandler.getInputMap(origInputMap));
            this.setActionMap(kHandler.getActionMap(origActionMap));
        }
        else
        {
            removeKeyListener(kHandler.getKeyListener());
            this.setInputMap(WHEN_IN_FOCUSED_WINDOW, new ComponentInputMap(this));
            this.setActionMap(new ActionMap());
        }
            
        this.kbdEnabled = kbdEnabled;
    }
    
    private void init()
    {
        kHandler = term.getKeyHandler();        
        setKbdEnabled(true);

        buildToolBar();

        frameBuff = new BufferedImage(term.getCols() * DEFAULT_WIDTH_PER_COL, term.getCols() * DEFAULT_HEIGHT_PER_ROW,
                                      BufferedImage.TYPE_INT_RGB);
        frame = frameBuff.createGraphics();
        windowMsgOnScreen = false;
        rectStartPoint = new Point();
        setBackground(currentBGColor);
        setFont(font);

        if (firstInit)
        {
            firstInit = false;
            
            addMouseListener(this);
            addMouseMotionListener(this);

            // originally, JPanel does not receive focus
            setFocusable(true);
            // to catch VK_TAB et al.
            setFocusTraversalKeysEnabled(false);
            //setVisible(true);
            //requestFocus();
        }
    }

    public void beep()
    {
        //TODO - option to silence
        Toolkit.getDefaultToolkit().beep();
        // TODO - vnc beep if kbd locked?
    }

    public void broadcastMessage(String msg)
    {
        log.fine("broadcast message: " + msg);
        windowMessage = msg;

        if (msg.indexOf("<too many connections>") != -1)
        {
            windowMessage = msg.substring(23);
            tooManyConnections = true;
        }

        setWindowMessage(MSG_BROADCAST);
    }

    //TODO - should probably abstract out some session handling object, with listener updates
    //       for objects needing status
    public Terminal getTerm()
    {
        return term;
    }

    //TODO - temp add sync lock to prevent overlapping updates, rendering Q or pipeline might be neater
    public void refresh()
    {

        synchronized (term.getLockObject())
        {
            renderScreen();
            repaint();
        }
    }

    public void paintCursor(int pos)
    {
        frame.setFont(font);
        frame.setColor(cursorColor);

        int x = ((pos % term.getCols()) * char_width) + (char_width + 5);
        int y = ((pos / term.getCols()) * char_height) + 7;
        int w = char_width;
        int h = char_height - 2;

        switch (term.getCursorStyle())
        {
            case BLOCK:
                frame.fillRect(x, y, w, h);
                frame.setColor(Color.black);

                //TODO - careful here over what char we are actually getting!
                byte[] c =
                {
                    (byte) term.getChar(pos).getDisplayChar()
                };
                frame.drawBytes(c, 0, 1, x, ((pos / term.getCols()) * char_height) + char_ascent + 5);
                break;

            case SQUARE:
                //TODO - check this
                frame.drawRect(x, y, w, h);
                break;

            case VERTICAL:
                //TODO - check this
                frame.drawLine(x, y, x, y + h);
                break;

            case HORIZONTAL:
                //TODO - check this
                frame.drawLine(x, y + h, x + w, y + h);
                break;
        }
    }

    public void paintStatusLine()
    {
        // Paints the border and status line
        // (on some devices, border should be user configurable!)
        frame.setColor(term.getStatusBorderColor(Color.red));
        frame.drawLine(char_width + 5, ((term.getRows()) * char_height) + char_ascent + 4,
                       (term.getCols() * char_width) + char_width + 5, ((term.getRows()) * char_height) + char_ascent + 4);

        TermChar[] stsCh = term.getStatusLine();
        if (stsCh != null)
        {
            // Status row is 2 lines below last row - will this ever need to be configurable?
            int statusOffset = (term.getRows() + 1) * term.getCols();
            for (int ix = 0; ix < stsCh.length; ix++)
            {
                paintChar(stsCh[ix], ix + statusOffset);
            }
        }
    }

    /**
     * Paints a message on the screen.
     */
    public void paintWindowMessage()
    {
        synchronized (term.getLockObject())
        {
            //TODO - option to silence
            beep();
            String message = null;
            windowMsgOnScreen = true;

            // frame.setFont(font);
            frame.setColor(currentBGColor);
            frame.fillRect(3, 3, getSize().width - 6, getSize().height - 6);

            switch (messageNumber)
            {
                case MSG_CLOSED_REMOTE:
                {
                    message = "Your connection to the server was lost or could not be established. "
                              + "Please try your session again, and contact your system administrator if the problem persists. ";
                    break;
                }

                case MSG_STRING:
                case MSG_BROADCAST:
                    message = windowMessage;
                    break;
            }

            frame.setColor(Color.red);
            frame.draw3DRect(5 + (char_width * 20), char_height * 2,
                             char_width * 40, char_width * 40, true);
            frame.setColor(Color.white);
            frame.setFont(new Font("Helvetica", Font.PLAIN, fontSize));

            // the next few lines of code handle broadcast messages of
            // varying length and therefore had to be able to auto-wrap on
            // whitespace
            if (message.length() <= 40)
            {
                frame.drawString(message, char_width * 22, char_height * 3);
            }
            else
            {
                int lineNo = 0;

                for (int i = 0; i < message.length(); i++)
                {
                    if ((message.length() - i) <= 45)
                    {
                        frame.drawString(message.substring(i, message.length()),
                                         char_width * 22, char_height * (3 + lineNo));

                        break;
                    }
                    else
                    {
                        String line = message.substring(i, i + 45);
                        int lastSpace = line.lastIndexOf(' ');
                        frame.drawString(message.substring(i, i + lastSpace),
                                         char_width * 22, char_height * (3 + lineNo));
                        i = i + lastSpace;
                        lineNo++;
                    }
                }
            }

            if ((messageNumber == MSG_BROADCAST) && (tooManyConnections == false))
            {
                frame.setFont(new Font("Helvetica", Font.BOLD, fontSize));
                frame.drawString("Message From Your System Administrator:",
                                 char_width * 22, (char_height * 2) - 5);
            }

            frame.setFont(new Font("Helvetica", Font.PLAIN, fontSize - 2));
            frame.drawString("Press any key to clear this message.",
                             char_width * 22, char_height * 19);
        }
    }

    /**
     * Renders a terminal screen on the buffered image graphics context.
     */
    //TODO - temp add sync lock to prevent overlapping updates, rendering Q or pipeline might be neater
    protected void renderScreen()
    {
        //start the blink thread
        //if (t != null) {
        //      t.interrupt();
        //      t = null;
        //}
        //t = new Thread(this);
        //t.start();
        synchronized (term.getLockObject())
        {
            if (this.windowMsgOnScreen)
            {
                return;
            }

            blankScreen(false);

            try
            {
                frame.setFont(font);

                // Any lingering text after a text we'll show partly transparent
                if (!term.getTelnet().isConnected())
                {
                    frame.setComposite(AC_50PCT);            
                }

                Page pg = term.getDisplayPage();
                // Possibly in startup or term re-init, or if some special "blank page" is in use
                if (pg != null && pg.displaySize() > 0)
                {
                    pg.buildFields(false);
                    log.finest("field count: " + pg.getFields().size());

                    int len = pg.displaySize();
                    for (int ix = 0; ix < len; ix++)
                    {
                        paintChar(pg.getChar(ix), ix);
                    }
                }
                    
                if (term.getTelnet().isConnected())
                {
                    paintCursor(term.getDisplayCursorPosition());
                }
                
                // Overlay images will typically only be used when disconnected to provide something
                // visual like branding logos etc
                Image overlayImg = term.getOverlayImage();
                log.fine("background logo: " + overlayImg);

                if (overlayImg != null)
                {
                    frame.setComposite(AC_75PCT);            
                    int x = (frameBuff.getWidth() - overlayImg.getWidth(null)) / 2;
                    int y = (frameBuff.getHeight() - overlayImg.getHeight(null)) / 2;            
                    frame.drawImage(overlayImg, x, y, null);
                }
                
                frame.setComposite(AC_OPAQUE);            
                paintStatusLine();
                
                checkChanges();
                fireScreenListeners();                
            }
            catch (NullPointerException e)
            {
                e.printStackTrace(System.out);
                log.severe("exception in JTerminalScreen.paintComponent: " + e.getMessage());
            }
        }
    }

    public void run()
    {
        // blinked is a toggle.  When true, the affected text is 'off'...
        boolean blinked = false;

        while (true)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                //e.printStackTrace();
                log.fine(e.getMessage());
            }

            refresh();
            blinked = !blinked;
            log.finer("blink!");
        }
    }

    public void setBackgroundColor(String c)
    {
        OpenTermConfig.setProp("color.background", c);
        currentBGColor = getColor(c);
        setBackground(currentBGColor);
        refresh();
    }

    public void setBoldColor(String c)
    {
        OpenTermConfig.setProp("color.bold", c);
        boldColor = getColor(c);
        refresh();
    }

    
    public void setFontSize(float size)
    {
       OpenTermConfig.setProp("font.size", Integer.toString((int) size));
       setFont(getFont().deriveFont(size));
    }
    
    /**
     * Sets the font used to draw the screen. Based on the given font metrics, changes screen size in case if the background rendering
     * buffer is not null.
     *
     * @param newFont the new font to use for rendering terminal screen.
     */
    public void setFont(Font newFont)
    {
        super.setFont(newFont);
        log.finer("new font: " + newFont);
        font = newFont;
        fontSize = font.getSize();

        if (frame != null)
        {
            FontRenderContext context = frame.getFontRenderContext();
            Rectangle2D bound = font.getStringBounds("M", context);
            char_width = (int) Math.round(bound.getWidth());
            char_height = (int) Math.round(bound.getHeight());
            char_ascent = Math.round(font.getLineMetrics("M", context).getAscent());

            int width = char_width * (term.getCols() + 5);
            int height = (char_height * (term.getRows() + 3));
            setSize(width, height);
            setPreferredSize(new Dimension(width, height));
            frameBuff = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            frame = frameBuff.createGraphics();

            if (log.isLoggable(Level.FINE))
            {
                log.fine("font metrics: " + char_width + " ; " + char_height);
            }

            refresh();
        }
    }

    public void setForegroundColor(String c)
    {
        OpenTermConfig.setProp("color.foreground", c);
        currentFGColor = getColor(c);
        refresh();
    }

    public void setWindowMessage(int msg)
    {
        messageNumber = msg;
        paintWindowMessage();
        repaint();
    }

    public void setWindowMessage(String msg)
    {
        windowMessage = msg;
        paintWindowMessage();
        repaint();
    }

    public void status(int status)
    {
        switch (status)
        {
            case TnAction.CONNECTING:
            {
                //setStatus("Connecting");
                break;
            }

            case TnAction.X_WAIT:
            {
                //setStatus("X-WAIT");
                break;
            }

            case TnAction.READY:
            {
                //setStatus("Ready");
                break;
            }

            case TnAction.DISCONNECTED:
            {
                //setStatus("Disconnected");
                break;
            }

            case TnAction.DISCONNECTED_BY_REMOTE_HOST:
            {
                setWindowMessage(MSG_CLOSED_REMOTE);
                //TODO - need to reinit/clear pages here probably (include when status handling reworked)
                break;
            }

            default:
                log.warning("status with id: " + status + " is not supported by JTerminalScreen");
        }
    }

    public boolean clearStatus()
    {
        if (windowMsgOnScreen)
        {
            windowMsgOnScreen = false;
            refresh();
            return true;
        }

        return false;
    }

    public void blankScreen(boolean paint)
    {
        frame.setColor(term.getBackgroundColor(currentBGColor));
        frame.fillRect(0, 0, char_width * (term.getCols() + 5), char_height * (term.getRows() + 1) + (char_height + char_ascent));
        
        if (paint)
        {
            repaint();
        }
    }

    /**
     * Paints a char on the terminal panel.
     *
     * @param c DOCUMENT ME!
     */
    protected void paintChar(TermChar c, int pos)
    {
        Color bgcolor = c.getBgColor(currentBGColor);
        Color fgcolor = c.getFgColor(currentFGColor);

        int row = pos / term.getCols();
        int col = pos % term.getCols();
        int fillX = (col * char_width) + char_width + 5;
        int fillY = (row * char_height) + 7;

        // We only apply monochrome video style transitions if the Terminal is not
        // doing specific color mapping itself
        if (!term.isColorMapping())
        {
            if (c.isAltIntensity())
            {
                if (fgcolor == currentFGColor)
                {
                    fgcolor = boldColor;
                }
            }

            //kludge for now to show fields for 3270 devices
            //if (this.term instanceof Term3270 && !c.isProtected()) 
            //{
            //    bgcolor = currentFieldBGColor;
            //}
            if (c.isReverse())
            {
                Color tmp = fgcolor;
                fgcolor = bgcolor;
                bgcolor = tmp;
            }
        }

        if (c.isStartField())
        {
            // This just blanks out the field char position. 
            frame.setColor(bgcolor);
            frame.fillRect(fillX, fillY, char_width, char_height);
            return;
        }

        frame.setFont(font);
        // We have to draw the background
        frame.setColor(bgcolor);
        frame.fillRect(fillX, fillY, char_width, char_height);

        byte[] ca = new byte[1];
        ca[0] = (byte) c.getDisplayChar();

        frame.setColor(fgcolor);
        frame.drawBytes(ca, 0, 1,
                        (col * char_width) + (char_width + 5),
                        (row * char_height) + char_ascent + 5);

        if (c.isUnderscore())
        {
            frame.drawLine(((col + 1) * char_width) + 5, (row * char_height) + 5 + char_height,
                           ((col + 2) * char_width) + 4, (row * char_height) + 5 + char_height);
        }
    }

    /**
     * Paints background buffered image on the given graphics context. Normally one would call
     * super.paintComponent, but in this case we're not really interested in L&F delegates or other things the
     * super class may take care of. We simply want the terminal screen rendered into a blank panel and nothing else that
     * calling super might trigger. The approach may also help in a headless rendering scenario - we will get a buffer
     * rendered here without exceptions being thrown.
     *
     * @param g DOCUMENT ME!
     */
    protected void paintComponent(Graphics g)
    {
        if (g != null)
        {
            g.drawImage(frameBuff, 0, 0, this);
        }
    }

    public void printScreen()
    {
        PrinterJob printJob = PrinterJob.getPrinterJob();
        printJob.setPrintable(this);
        if (printJob.printDialog())
        {
            try
            {
                printJob.print();
            }
            catch (PrinterException pe)
            {
                System.out.println("Error printing: " + pe);
            }
        }
    }

    public int print(Graphics graphics, PageFormat pageFormat,
                     int pageIndex) throws PrinterException
    {
        /*
    	 * we can only print the current 3270 terminal screen image. There's no such thing as
    	 * multiple pages, and we're not implementing a client side terminal printer.
         */
        if (pageIndex > 0)
        {
            return Printable.NO_SUCH_PAGE;
        }

        Graphics2D graphics2d = (Graphics2D) graphics;
        graphics2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        graphics2d.setFont(new Font("Monospaced", Font.PLAIN, 10));

        FontMetrics fontMetrics = graphics2d.getFontMetrics();

        char[] c = term.getDisplay();

        StringBuffer textBuffer = new StringBuffer(term.getCols());
        /*
    	 * center the display on the page.
         */
        float yPos = (float) (pageFormat.getImageableHeight() / 2)
                         - ((term.getRows() * fontMetrics.getHeight()) / 2);

        /*
    	 * we're using a fixed width font, so we can get the width
    	 * of any character. It should equal the width of every
    	 * other character.
         */
        float xPos = (float) (pageFormat.getImageableWidth() / 2)
                         - ((term.getCols() * fontMetrics.stringWidth(" ")) / 2) + (72 / 10);

        for (int i = 0; i < c.length; i++)
        {
            if (i % term.getCols() == (term.getCols() - 1))
            {
                textBuffer.append(c[i]);
                graphics2d.drawString(textBuffer.toString(), xPos, yPos);
                textBuffer.setLength(0);
                yPos += fontMetrics.getHeight();
            }
            else
            {
                textBuffer.append(c[i]);
            }
        }

        yPos += fontMetrics.getHeight();
        graphics2d.drawString(textBuffer.toString(), xPos, yPos);

        return Printable.PAGE_EXISTS;
    }
    
    
    // Note thread safety is assured here by CopyOnWriteArraySet iterator which 
    // takes a snapshot. However, listeners themselves have the potential to lock up
    // our handler thread. A pooled executor service could be used to avoud this if it 
    // becomes a problem.
    protected void fireScreenListeners()
    {
        for (ScreenImageListener listener : listeners)
        {
            listener.screenUpdated(this);
        }
    }
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS - Screen
    //////////////////////////////////////////////////
    
    // The Screen idea is based around the concept of remote/external viewers
    // such as the experimental VNC/RFB code. Supporting such tools means providing ways
    // to scrap screen images, and send in keyboard and mouse events
    
    public void addScreenListener(ScreenImageListener listener)
    {
        listeners.add(listener);
    }

    public void removeScreenListener(ScreenImageListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public BufferedImage getScreenBuffer()
    {
        return frameBuff;
    }

    @Override
    public int[] getScreenPixels()
    {
        return frameBuff.getRGB(0, 0, frameBuff.getWidth(), frameBuff.getHeight(), null, 0, frameBuff.getWidth());         
    }
    
    public void processScreenKey(KeyEvent evt)
    {
        if (this.kbdEnabled)
        {
            switch (evt.getID())
            {
                case KeyEvent.KEY_TYPED:
                    kHandler.getKeyListener().keyTyped(evt);
                    break;

                case KeyEvent.KEY_PRESSED:
                    kHandler.doKeyAction(KeyStroke.getKeyStroke(evt.getKeyCode(),evt.getModifiers()));
                    break;
            }
        }
        //TODO - beep??
        
    }
    
    public boolean isScreenInputEnabled()
    {
        return this.kbdEnabled;
    }
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS - MouseListener, MouseMotionListener
    //////////////////////////////////////////////////

    /**
     * Dispatches mouse event. In case if <code>BUTTON1</code> is clicked, moves terminal cursor to the character, on which the mouse event
     * occured.
     *
     * @param e DOCUMENT ME!
     */
    public void mouseClicked(MouseEvent e)
    {

        if (clearStatus() || term.getDisplayPage().isLocalEditMode())
        {
            return;
        }

        if (e.getButton() == MouseEvent.BUTTON1)
        {
            log.finer("mouse clicked at: (" + e.getX() + ", " + e.getY() + ")");

            double dx = e.getX() - MARGIN_LEFT;
            double dy = e.getY() - MARGIN_TOP;

            if ((dx >= 0) && (dy >= 0))
            {
                int newpos = (((int) Math.floor(dx / char_width)) + //TODO - check this use of literal 80
                              ((int) Math.floor(dy / char_height) * 80)) - 1;

                if (newpos >= 0 && newpos < (term.getCols() * term.getRows()))
                {
                    term.setCursorPosition((short) (newpos), true);
                }
            }
        }

        //requestFocus();
    }

    public void mousePressed(MouseEvent e)
    {
        /* Save the initial point for the rectangle */
        rectStartPoint.x = e.getX();
        rectStartPoint.y = e.getY();

    }

    public void mouseReleased(MouseEvent e)
    {
        refresh();
    }

    public void mouseDragged(MouseEvent e)
    {
        Rectangle rect = null;

        int eventY = e.getY();
        int eventX = e.getX();
        //TODO - ??
        renderScreen();

        frame.setColor(Color.WHITE);

        /* Quadrant IV */
        if ((rectStartPoint.x < eventX) && (rectStartPoint.y < eventY))
        {
            rect = new Rectangle(rectStartPoint.x, rectStartPoint.y,
                                 eventX - rectStartPoint.x, eventY - rectStartPoint.y);
        }
        /* Quadrant I */
        else if (rectStartPoint.x < eventX)
        {
            rect = new Rectangle(rectStartPoint.x, eventY,
                                 eventX - rectStartPoint.x, rectStartPoint.y - eventY);

        }
        else if (rectStartPoint.y < eventY)
        {
            /* Quadrant III */
            rect = new Rectangle(eventX, rectStartPoint.y,
                                 rectStartPoint.x - eventX, eventY - rectStartPoint.y);

        }
        else
        {
            /* Quadrant II */
            rect = new Rectangle(eventX, eventY,
                                 rectStartPoint.x - eventX, rectStartPoint.y - eventY);

        }
        frame.draw(rect);
        repaint();
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mouseMoved(MouseEvent e) {}

    //////////////////////////////////////////////////
    // PRIVATE METHODS
    //////////////////////////////////////////////////

    private int[] lastFrame = new int[0];
    
    private synchronized void checkChanges()
    {
        // This is only really diagnostic code, so skip if not logging
        if (!log.isLoggable(Level.FINEST))  { return; }

        int[] newFrame = frameBuff.getRGB(0, 0, frameBuff.getWidth(), frameBuff.getHeight(), null, 0, 
                                          // last param very confusing, scanline stride is width of image i.e. start of next row:
                                          // not always the same apparently!
                                          frameBuff.getWidth()); 
        
        int changedBytes = newFrame.length;
        int blankBytes = 0;
        
        if (lastFrame.length == newFrame.length)
        {
            for (int ix = 0; ix < newFrame.length; ix++)
            {
                if (newFrame[ix] == lastFrame[ix])
                {
                    changedBytes--;
                }
                
                if (newFrame[ix] == currentBGColor.getRGB())
                {
                    blankBytes++;
                }
            }
        }
        
        log.finest(String.format("*** newFrame size: %d, changed bytes: %d, background bytes: %d", 
                                         newFrame.length * Integer.BYTES,
                                         changedBytes * Integer.BYTES,
                                         blankBytes * Integer.BYTES
                            ));        
        
        lastFrame = newFrame;
    }
    
    
}
