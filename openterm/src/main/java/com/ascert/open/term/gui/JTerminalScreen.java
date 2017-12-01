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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.MouseInputAdapter;

import com.ascert.open.ohio.Ohio.OHIO_AID;
import com.ascert.open.term.application.OpenTermConfig;
import com.ascert.open.term.core.AbstractTerminal.Page;

import com.ascert.open.term.core.IsProtectedException;
import com.ascert.open.term.core.SimpleConfig;

import com.ascert.open.term.core.TermField;
import com.ascert.open.term.core.TermChar;
import com.ascert.open.term.core.Terminal;
import com.ascert.open.term.core.TnAction;
import com.ascert.open.term.i3270.Term3270;

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
public class JTerminalScreen extends JPanel implements TnAction, Printable
{

    private static final Logger log = Logger.getLogger(JTerminalScreen.class.getName());

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

    /**
     * Default size of the screen font.
     */
    public static final int DEFAULT_FONT_SIZE = 14;

    /**
     * Default font used to draw terminal.
     */
    public static final Font DEFAULT_FONT = new Font("Monospaced", Font.PLAIN, DEFAULT_FONT_SIZE);

    /**
     * Default terminal foreground color.
     */
    public static final Color DEFAULT_FG_COLOR = Color.CYAN;

    /**
     * Default terminal background color.
     */
    public static final Color DEFAULT_BG_COLOR = Color.BLACK;
    /**
     * Default font color used to render bold text.
     */
    public static final Color DEFAULT_BOLD_COLOR = Color.WHITE;

    /**
     * Default color used to render cursor pointer on the terminal screen.
     */
    public static final Color DEFAULT_CURSOR_COLOR = Color.RED;

    /**
     * Default input field background color.
     */
    public static final Color DEFAULT_FIELD_BG_COLOR = new Color(45, 45, 45);

    // buffer used to draw the screen in background.
    private BufferedImage frameBuff;

    private Color boldColor = DEFAULT_BOLD_COLOR;
    private Color currentBGColor = DEFAULT_BG_COLOR;
    private Color currentFGColor = DEFAULT_FG_COLOR;
    private Color currentFieldBGColor = DEFAULT_FIELD_BG_COLOR;
    private Color cursorColor = DEFAULT_CURSOR_COLOR;
    private Font font = DEFAULT_FONT;
    private int fontsize = DEFAULT_FONT_SIZE;

    // graphics context of the background buffer. Use this context to
    // paint the components of the screen.
    private Graphics2D frame;
    private Terminal term;
    private KeyHandler kHandler;

    /**
     * Current status message.
     */
    private String statusMessage;
    private String windowMessage;
    private boolean windowMsgOnScreen;
    private int messageNumber;
    private boolean tooManyConnections;

    private String currentPosition;
    private int char_ascent;
    private int char_height;
    private int char_width;
    private Point rectStartPoint;

    JToolBar toolbar;
    private boolean firstInit = true;

    private InputMap origInputMap;
    private ActionMap origActionMap;

    /**
     * Construct a new GUI session with a terminalModel of 2, and a terminalType of 3279-E.
     */
    public JTerminalScreen(Terminal term)
    {
        super();

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

        if (this.getParent() == null || kHandler == null)
        {
            return;
        }

        if (toolbar == null)
        {
            toolbar = new JToolBar();
            //toolbar.setFloatable( false );
            this.getParent().add(toolbar, BorderLayout.PAGE_START);
        }
        else
        {
            toolbar.removeAll();
        }

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

    private void init()
    {

        kHandler = term.getKeyHandler();
        addKeyListener(kHandler.getKeyListener());

        this.setInputMap(WHEN_IN_FOCUSED_WINDOW, kHandler.getInputMap(origInputMap));
        this.setActionMap(kHandler.getActionMap(origActionMap));

        buildToolBar();

        frameBuff = new BufferedImage(term.getCols() * DEFAULT_WIDTH_PER_COL, term.getCols() * DEFAULT_HEIGHT_PER_ROW,
                                      BufferedImage.TYPE_INT_RGB);
        frame = frameBuff.createGraphics();
        windowMsgOnScreen = false;
        rectStartPoint = new Point();
        setBackground(currentBGColor);
        setFont(DEFAULT_FONT);

        if (firstInit)
        {
            firstInit = false;
            //addKeyListener(this);

            MouseInputAdapter mouseAdapter = (new MouseInputAdapter()
            {
                public void mouseClicked(MouseEvent e)
                {
                    JTerminalScreen.this.mouseClicked(e);
                }

                public void mousePressed(MouseEvent e)
                {
                    JTerminalScreen.this.mousePressed(e);
                }

                public void mouseReleased(MouseEvent e)
                {
                    JTerminalScreen.this.mouseReleased(e);
                }

                public void mouseDragged(MouseEvent e)
                {
                    JTerminalScreen.this.mouseDragged(e);
                }
            });
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);

            // originally, JPanel does not receive focus
            setFocusable(true);

            // to catch VK_TAB et al.
            setFocusTraversalKeysEnabled(false);
            setVisible(true);
            requestFocus();
        }
    }

    public void beep()
    {
        //TODO - option to silence
        Toolkit.getDefaultToolkit().beep();
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

            // Only display if enabled
            if (toolbar != null)
            {
                toolbar.setVisible("true".equals(OpenTermConfig.getProp("toolbar.fkey.enabled")));
            }

            repaint();
        }
    }

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

        requestFocus();
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
            frame.setFont(new Font("Helvetica", Font.PLAIN, fontsize));

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
                frame.setFont(new Font("Helvetica", Font.BOLD, fontsize));
                frame.drawString("Message From Your System Administrator:",
                                 char_width * 22, (char_height * 2) - 5);
            }

            frame.setFont(new Font("Helvetica", Font.PLAIN, fontsize - 2));
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
                paintStatusLine();
                paintCursor(term.getDisplayCursorPosition());
            }
            catch (NullPointerException e)
            {
                e.printStackTrace();
                log.severe("exception in RHPanel.paintComponent: " + e.getMessage());
            }
        }
    }

    public void run()
    {
        // blinked is a toggle.  When true,
        // the affected text is 'off'...
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

    public void setBackgroundColor(Color c)
    {
        currentBGColor = c;
        setBackground(c);
        refresh();
    }

    public void setBoldColor(Color c)
    {
        boldColor = c;
        refresh();
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
        fontsize = font.getSize();

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

    public void setForegroundColor(Color c)
    {
        currentFGColor = c;
        refresh();
    }

    /**
     * Sets current status message and calls components <code>repaint</code> method to make the new status message visible.
     *
     * @param statusMessage DOCUMENT ME!
     */
    public void setStatus(String statusMessage)
    {
        this.statusMessage = statusMessage;
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
                setStatus("Connecting");
                break;
            }

            case TnAction.X_WAIT:
            {
                setStatus("X-WAIT");
                break;
            }

            case TnAction.READY:
            {
                setStatus("Ready");
                break;
            }

            case TnAction.DISCONNECTED:
            {
                setStatus("Disconnected");
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
     * Paints backgoround buffered image on the given graphics context.
     *
     * @param g DOCUMENT ME!
     */
    protected void paintComponent(Graphics g)
    {
        g.drawImage(frameBuff, 0, 0, this);
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

}
