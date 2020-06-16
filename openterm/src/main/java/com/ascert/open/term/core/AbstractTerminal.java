/*
 * Copyright (cx) 2016, 2017 Ascert, LLC.
 * www.ascert.com
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
package com.ascert.open.term.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import com.ascert.open.ohio.Ohio;
import com.ascert.open.term.core.SimpleConfig.SyspropsConfig;
import com.ascert.open.term.gui.KeyHandler;
import com.ascert.open.term.i3270.KeyHandler3270;

/**
 *
 * @version 1,0 24-Apr-2017
 * @author srm

 */
public abstract class AbstractTerminal
    implements Terminal
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    private static final Logger log = Logger.getLogger(AbstractTerminal.class.getName());

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    
    protected Host host;
    protected TnAction client;
    protected TnStreamParser tnParser;

    protected Ohio.OHIO_AID aid; // current aid
    protected Ohio.OHIO_TYPE ohTermType;
    protected short cols;
    protected short rows;

    protected boolean keyboardLocked;

    protected RWTelnet tn;      // Telnet instance

    protected String termType;

    // Some terminals will only support a single page
    protected List<Page> pages = new ArrayList<>();
    // Not all terminals will have a status line. Leave null if not
    protected TermChar[] statusLine = null;

    // The active page is that used for host ix/o data 
    protected int activePage = 0;
    // The display page is that currently viewed and used for keyboard operations
    protected int displayPage = 0;

    // These are defaults for 3270 but may differ on other terminal types
    protected boolean allowCursorAnywhere = true;
    protected boolean autoFieldWrap = true;

    // Keyboad Lock listener handling - rather trivial implementation but workable for now
    protected final Set<KbdLockListener> listeners = new CopyOnWriteArraySet<>();

    protected KeyHandler keyHandler;

    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    public AbstractTerminal()
    {
        setClient(null);
    }

    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    /**
     * Returns a string representation of the current 3270 screen state.
     *
     * <p>
     * The returned string is a unique description of the terminal screen contents.
     * </p>
     *
     * @return DOCUMENT ME!
     *
     * @since 0.2
     */
    public String toString()
    {
        Vector fields = getFields();
        TermChar[] chars = getCharBuffer();
        StringBuffer rep = new StringBuffer("(screen \n"); // representation

        for (Enumeration e = fields.elements(); e.hasMoreElements();)
        {
            TermField field = (TermField) e.nextElement();
            rep.append(field.toString()).append("\n");
        }

        for (int i = 0; i < chars.length; i++)
        {
            rep.append(chars[i].toString()).append("\n");
        }

        rep.append(")");

        return rep.toString();
    }

    //////////////////////////////////////////////////
    // INTERFACE METHODS - Terminal
    //////////////////////////////////////////////////
    public void Fkey(int key)
    {
        Fkey(Ohio.OHIO_AID.getOhioAid(key, ohTermType));
    }

    /**
     * Presses the specified Fkey Key, as specified in the constants for this class. For example to press PF1, call
     * rw.Fkey(OHIO_AID.OHIO_AID_3270_PF1);
     *
     * @param key - the key to be pressed, as specified in the constants for this class.
     */
    public void Fkey(Ohio.OHIO_AID key)
    {
        if (isKeyboardLocked())
        {
            return;
        }

        aid = key;
        tnParser.Fkey(key);
    }

    public abstract void initTerm();

    // Very basic model for multi-page terminals. Just keep a list of each fieldset
    // Page numbering starts from 0
    public int getMaxPages()
    {
        return pages.size();
    }

    public Page getPage(int pg)
    {
        return (pg >= 0 && pg < pages.size()) ? pages.get(pg) : null;
    }

    public Page getActivePage()
    {
        return getPage(activePage);
    }

    public int getActivePageNumber()
    {
        return activePage;
    }

    public synchronized void setActivePage(int pgNum)
    {
        log.fine("setActivePage: " + pgNum);
        if (activePage == pgNum)
        {
            return;
        }     // do nothing!

        Page pg = getPage(pgNum);
        if (pg == null)
        {
            log.severe("Attempt to set invalid active page: " + pgNum);
            return;
        }

        activePage = pgNum;
    }

    public Page getDisplayPage()
    {
        return getPage(displayPage);
    }

    public int getDisplayPageNumber()
    {
        return displayPage;
    }

    public synchronized void setDisplayPage(int pgNum)
    {
        log.fine("setDisplayPage: " + pgNum);
        if (displayPage == pgNum)
        {
            return;
        }     // do nothing!

        Page pg = getPage(pgNum);
        if (pg == null)
        {
            log.severe("Attempt to set invalid display page: " + pgNum);
            return;
        }

        displayPage = pgNum;
        // Just for good measure
        pg.buildFields(true);
        getClient().refresh();
    }

    // TODO - actually should be able to cope with null client e.g. a do-nothing stub
    public void setClient(TnAction client)
    {

        if (client == null)
        {
            // Make sure we have a stub client
            client = new TnAction()
            {
            };
        }

        this.client = client;
    }

    public TnAction getClient()
    {
        return this.client;
    }

    public String getTermType()
    {
        return this.termType;
    }

    /**
     * Searches the current data buffer (screen) for the specified string.
     *
     * @param search The string to search the data buffer for.
     *
     * @return <code>true</code> if the string was found, <code>false</code> otherwise.
     */
    public boolean contains(String search)
    {
        return search != null ? (new String(getDisplay())).contains(search) : false;
    }

    public Ohio.OHIO_AID getAIDEnum()
    {
        return aid;
    }

    /**
     * Returns an array of Term3270Char objects representing the data buffer.
     *
     * @return the Term3270Char array
     */
    public TermChar[] getCharBuffer()
    {
        return getActivePage().getCharBuffer();
    }

    // Convenience method to get a String containing a specified number of chars from start position
    public String getCharString(int startBA, int len)
    {
        StringBuffer prompt = new StringBuffer();
        for (int ix = 0; ix < len; ix++)
        {
            prompt.append(getChar(startBA + ix).getChar());
        }

        return prompt.toString();
    }

    /**
     * This method is useful for retrieving the Term3270 character object at a particular position on the screen.
     *
     * @param i Screen position of the requested Term3270Char object
     *
     * @return Term3270Char object
     */
    public TermChar getChar(int i)
    {
        return getActivePage().getChar(i);
    }

    /**
     * DOCUMENT ME!
     *
     * @return The number of cols in the current screen
     */
    public int getCols()
    {
        return cols;
    }

    public int getCurrentRow()
    {
        int curPos = getCursorPosition();
        return (curPos / getCols()) + 1;
    }

    public int getNextRow(boolean wrap)
    {
        int row = getCurrentRow();
        if (row == getRows())
        {
            return wrap ? 1 : row;
        }

        return row + 1;
    }

    public int getPreviousRow(boolean wrap)
    {
        int row = getCurrentRow();
        if (row == 1)
        {
            return wrap ? getRows() : row;
        }

        return row - 1;
    }

    public int getCurrentCol()
    {
        int curPos = getCursorPosition();
        return (curPos % getCols()) + 1;
    }

    /**
     * DOCUMENT ME!
     *
     * @return The current cursor position
     */
    public int getCursorPosition()
    {
        return getDisplayPage().getCursorPosition();
    }

    /**
     * Gets the current screen's display characters. This method is useful for getting the characters necessary for display in the client
     * implementation. It automatically suppresses hidden (password, etc.) fields and null characters.
     *
     * Note - one of the main uses of this method is for the Ohio wrapper. It's not totally clear in this case whether status line
     * information should be included. It has been for now, but could be made suppress-able with an extra param. On some devices (e.g. NSK
     * 6530) the status line can contain application generated output. So this seems correct for now.
     *
     * @return The current screen as an array of characters.
     */
    public char[] getDisplay()
    {
        // Might want to make inclusion of status line optional
        TermChar[] chBuf = getDisplayPage().getCharBuffer(true);

        char[] displayChars = new char[chBuf.length];
        for (int ix = 0; ix < chBuf.length; ix++)
        {
            displayChars[ix] = chBuf[ix].getDisplayChar();
        }

        return displayChars;
    }

    /**
     * This method returns the RW3270Field object that the current cursor position is in.
     *
     * @return RW3270Field object.
     */
    public TermField getField()
    {
        return getField(getActivePage().getCursorPosition());
    }

    /**
     * This method returns the RW3270Field object at the specified buffer address
     *
     * @param i Screen position of the requested RW3270Field Object.
     *
     * @return RW3270Field object
     */
    public TermField getField(int i)
    {
        //Just in case
        buildFields();
        return getChar(i).getField();
    }

    /**
     * Returns a vector of fields representing the current screen.
     *
     * @return the field Vector
     */
    public Vector getFields()
    {
        return getActivePage().getFields();
    }

    /**
     * DOCUMENT ME!
     *
     * @return The number of rows in the current screen
     */
    public int getRows()
    {
        return rows;
    }

    /**
     * Allows access to low level screen and telnet handling classes
     *
     * @return the tnParser
     */
    public TnStreamParser getStreamParser()
    {
        return tnParser;
    }

    /**
     * Allows access to low level screen and telnet handling classes
     *
     * @return the tn
     */
    public RWTelnet getTelnet()
    {
        return tn;
    }

    /**
     * @param tn Allow alteration to default telnet class
     */
    public void setTelnet(RWTelnet tn)
    {
        this.tn = tn;
    }

    /**
     * Returns the first character position of the next unprotected field from the current cursor position.
     *
     * @param pos DOCUMENT ME!
     *
     * @return The next unprotected field's address, starting from the current cursor position.
     */
    public short getNextUnprotectedField(int pos)
    {
        Vector fields = getFields();

        Enumeration e = fields.elements();
        while (e.hasMoreElements())
        {
            TermField f = (TermField) e.nextElement();
            if (!f.isProtected() && (f.getBeginBA() >= pos))
            {
                return (short) (f.getBeginBA() + 1);
            }
        }
        //no next, get first unprotected (wrap)
        e = fields.elements();
        while (e.hasMoreElements())
        {
            TermField f = (TermField) e.nextElement();
            if (!f.isProtected())
            {
                return (short) (f.getBeginBA() + 1);
            }
        }
        return (short) pos;
    }

    /**
     * This method returns the first character position of the previous field from the current cursor position.
     *
     * @param pos DOCUMENT ME!
     *
     * @return The previous unprotected field's address, starting from the current cursor position
     */
    public short getPreviousUnprotectedField(int pos)
    {
        return getPreviousUnprotectedField(pos, false);
    }

    public short getPreviousUnprotectedField(int pos, boolean endpos)
    {
        TermField prevFld = null;
        TermField currFld = getChar(pos).getField();

        Enumeration e = getFields().elements();
        while (e.hasMoreElements())
        {
            TermField fld = (TermField) e.nextElement();

            if (fld == currFld && prevFld != null)
            {
                break;
            }

            if (!fld.isProtected())
            {
                // keep a rolling record of all unprotected fields we find
                prevFld = fld;
            }
        }

        // most likely started at or before 1st field, so return last unprotected field we found
        if (prevFld != null)
        {
            return (endpos) ? (short) prevFld.getEndBA() : (short) (prevFld.getBeginBA() + 1);
        }

        // give up!
        return (short) pos;
    }

    @Override
    public KeyHandler getKeyHandler()
    {
        return this.keyHandler;
    }

    /**
     * This method is designed to let implementations check to see if the 'keyboard' is currently locked.
     *
     * @return <code>true</code> if the screen is not currently accepting input, <code>false</code> if it is.
     */
    public boolean isKeyboardLocked()
    {
        return keyboardLocked;
    }

    public void setKeyboardLocked(boolean locked)
    {
        //TODO - need to better generalisation for status handling 
        getClient().status(locked ? TnAction.X_WAIT : TnAction.READY);
        keyboardLocked = locked;

        fireKbdLockListeners();
    }

    public void addKbdLockListener(KbdLockListener listener)
    {
        listeners.add(listener);
    }

    public void removeKbdLockListener(KbdLockListener listener)
    {
        listeners.remove(listener);
    }

    // Note thread safety is assured here by CopyOnWriteArraySet iterator which 
    // takes a snapshot. However, listeners themselves have the potential to lock up
    // our handler thread. A pooled executor service could be used to avoud this if it 
    // becomes a problem.
    protected void fireKbdLockListeners()
    {
        for (KbdLockListener listener : listeners)
        {
            listener.kbdLockChanged(this);
        }
    }

    /**
     * This method sets the cursor position.
     *
     * @param newCursorPos The new cursor position
     */
    public void setCursorPosition(int newCursorPos)
    {
        setCursorPosition(newCursorPos, false);
    }

    /**
     * This method sets the cursor position and updates client
     *
     * @param newCursorPos The new cursor position
     */
    public void setCursorPosition(int newCursorPos, boolean updateClient)
    {
        getDisplayPage().setCursorPosition((short) newCursorPos);

        if (updateClient)
        {
            client.refresh();
        }
    }

    //TODO - should probably abstract out some session handling object, with listener updates
    //       for objects needing status

    /**
     * This method implements connections directly to a host.
     *
     * @param host The hostname of the TN3270 host to connect to
     * @param port The port on which to connect to the TN3270 host
     *
     * @throws IOException          DOCUMENT ME!
     * @throws UnknownHostException DOCUMENT ME!
     */
    public void connect()
        throws IOException, UnknownHostException
    {
        tn.setEncryption(host.isEncryption());
        log.fine("connecting " + host + ":" + host.getPort());
        //Wipe down any possible old remnants from previous session
        initTerm();
        client.status(TnAction.CONNECTING);
        tn.connect(host.getHostName(), host.getPort());
        log.fine("connecting complete");
        client.refresh();
    }
    
    public void connect(InputStream is, OutputStream os)
    {
        log.fine("connecting (direct)");
        //Wipe down any possible old remnants from previous session
        initTerm();
        client.status(TnAction.CONNECTING);
        tn.connect(is, os);
        log.fine("connecting complete");
        client.refresh();
    }

    /**
     * Disconnects this RW3270 object from the current Session.
     */
    public void disconnect()
    {
        log.fine("disconnecting");
        client.status(TnAction.DISCONNECTED);
        tn.disconnect();
        // Update screen first, so can see status message changes
        client.refresh();
    }

    /**
     * Sets the encryption setting for the 3270 session...
     *
     * @param encryption will turn encryption on, false will turn encryption off
     *
     */
    public void setEncryption(boolean encryption)
    {
        tn.setEncryption(encryption);
    }

    /**
     * @return the writeBA
     */
    public int getWriteBA()
    {
        return getActivePage().getWriteBA();
    }

    /**
     * @param writeBA the writeBA to set
     */
    public void setWriteBA(int writeBA)
    {
        getActivePage().setWriteBA(writeBA);
    }

    public void buildFields(boolean force)
    {
        getActivePage().buildFields(force);
    }

    public void resetMDT()
    {
        this.getActivePage().resetMDT();
    }

    public void clearFields(int startBA, boolean all)
    {
        this.getActivePage().clearFields(startBA, all);
    }

    public void setFieldsChanged(boolean dirty)
    {
        this.getActivePage().setFieldsChanged(dirty);
    }
    
    public void setStatusChars(int off, String conv)
    {
        setStatusChars(off, conv, null);
    }

    public void setStatusChars(int off, String conv, TermChar.VideoAttribute vidAttr)
    {
        if (statusLine == null)
        {
            return;
        }

        for (int ix = 0; ix < conv.length(); ix++)
        {
            statusLine[off + ix].setChar(conv.charAt(ix));

            if (vidAttr != null)
            {
                statusLine[off + ix].setVideoAttribute(vidAttr, true);
            }
        }
    }

    public TermChar getStatusChar(int off)
    {
        return statusLine[off];
    }
    
    /**
     * @return the host
     */
    public Host getHost()
    {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(Host host)
    {
        this.host = host;
    }


    //////////////////////////////////////////////////
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////
    protected synchronized void initPages(int maxPages)
    {
        for (int ix = 0; ix < maxPages; ix++)
        {
            pages.add(getNewPage(ix));
        }

        displayPage = 0;
        activePage = 0;
    }

    // Various overrideable methods to allows for terminals with different Page or TermChar handling
    protected Page getNewPage(int pgNum)
    {
        return new Page(pgNum);
    }

    protected TermChar[] getTermCharPage()
    {
        // create the character buffer
        return initTermChars(new TermChar[rows * cols], 0);
    }

    protected abstract TermChar[] initTermChars(TermChar[] tch, int baOff);

    protected abstract TermField newTermField(TermChar currChar);

    //////////////////////////////////////////////////
    // PRIVATE INSTANCE METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // STATIC INNER CLASSES
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // NON-STATIC INNER CLASSES
    //////////////////////////////////////////////////
    // Page class is where the main cursor and field handling logic actually occurs
    public class Page
    {

        protected Vector<TermField> fields = new Vector<>();
        protected TermChar[] chars;

        protected int writeBA = 0;
        protected short cursorPosition = 0;
        // Just in case we need it!
        private final int myNum;

        private boolean fieldsChanged = true;

        public Page(int myNum)
        {
            this.myNum = myNum;
            this.chars = getTermCharPage();
        }

        public int displaySize()
        {
            // Most pages have contents. Devices that wish to suppress rendering due to 
            // special pages or circumstances can override this method
            return chars.length;
        }

        public int getCursorPosition()
        {
            return cursorPosition;
        }

        /**
         * @param newPosition the newPosition to set
         */
        public void setCursorPosition(int newPosition)
        {
            boolean backward = (newPosition < this.cursorPosition);
            buildFields(false);

            if (newPosition < 0)
            {
                newPosition += displaySize();
            }
            else if (newPosition >= displaySize())
            {
                newPosition -= displaySize();
            }

            if (allowCursorAnywhere)
            {
                this.cursorPosition = (short) newPosition;
                return;
            }

            TermField f = getField(newPosition);
            if (f != null && !f.isProtected())
            {
                // flipping ugly this!!!
                if (f.getBeginBA() == newPosition)
                {
                    this.cursorPosition = (backward) ? getPreviousUnprotectedField(newPosition, true)
                        : (short) (newPosition + 1);
                }
                else
                {
                    this.cursorPosition = (short) newPosition;
                }
            }
            else if (backward)
            {
                this.cursorPosition = getPreviousUnprotectedField(newPosition);
            }
            else
            {
                this.cursorPosition = getNextUnprotectedField(newPosition);
            }
        }

        public TermChar[] getCharBuffer()
        {
            return getCharBuffer(false);
        }

        public TermChar[] getCharBuffer(boolean includeStatus)
        {
            TermChar[] sts = getStatusLine();

            if (!includeStatus || sts == null)
            {
                return this.chars;
            }

            TermChar[] fullPage = new TermChar[chars.length + statusLine.length];
            System.arraycopy(chars, 0, fullPage, 0, chars.length);
            System.arraycopy(statusLine, 0, fullPage, chars.length, statusLine.length);
            return fullPage;
        }

        public TermChar getChar(int i)
        {
            return this.chars[i];
        }

        // Something of a quick and dirty, but various operations may need to know if a Page is processing some kind of local
        // edit handling e.g. local command buffering. One of the uses is to prevent operations that would cause issues e.g. mouse
        // click or other operations that can move the cursor.
        // Perhaps not a great abstraction, but will suffice for now
        public boolean isLocalEditMode()
        {
            return false;
        }

        public Vector getFields()
        {
            // Just in case
            buildFields(false);
            return this.fields;
        }

        /**
         * @return the writeBA
         */
        public int getWriteBA()
        {
            return writeBA;
        }

        /**
         * @param writeBA the writeBA to set
         */
        public void setWriteBA(int writeBA)
        {
            //TODO - cater for terminal types which do not support wrap around
            if (writeBA >= (chars.length))
            {
                writeBA %= chars.length;
            }

            this.writeBA = writeBA;
        }

        /**
         * This is a utility method that builds the field vector after data comes in by reading the data buffer and creating a Field Object
         * for each Start Field character.
         *
         * <p>
         * The Field objects merely 'point' to the corresponding Start Field character for conceptual ease for end-programmers. No data is
         * 'contained' in a field object
         */
        //TODO - preserve protected field at 1,1: - could in fact go in buildFields??
        /*
               !
               ! By default there is a protected field at (1,1)
               !
         */
        //TODO - check sync of this
        public void buildFields(boolean force)
        {
            synchronized (getLockObject())
            {
                //TODO - some devices (e.g. NSK 6530) have an implicit start field at position 1 (p3-44)
                if (!fieldsChanged && !force)
                {
                    return;
                }

                fields.removeAllElements();
                TermField lastField = null;
                TermChar vaChar = getDefaultVAChar();

                for (int ix = 0; ix < chars.length; ix++)
                {
                    TermChar currChar = chars[ix];
                    //ensure field pos correct - can change with some screen operations (del/ins line etc)
                    currChar.setPositionBA(ix);

                    if (currChar.hasVideoAttributes())
                    {
                        vaChar = currChar;
                    }

                    currChar.setVAChar(vaChar);

                    if (currChar.isStartField())
                    {
                        if (lastField != null)
                        {
                            lastField.setEndBA(ix - 1);
                        }

                        //since it's a Start Field FA, create a new field
                        TermField currField = newTermField(currChar);

                        //set it's begin point as the current counter position
                        currField.setBeginBA(ix);

                        //add it to the fields vector
                        fields.addElement(currField);

                        //move it to the last field variable, so we can set its
                        //end point.
                        lastField = currField;
                    }

                    currChar.setField(lastField);
                }

                // Have to find the end point for the last field.  Can't just set it to the last address in the buffer,
                // for fields that aren't terminated when auto-wrap is enabled e.g. 3270
                if (lastField != null)
                {
                    if (autoFieldWrap)
                    {
                        TermField firstField = fields.elementAt(0);
                        lastField.setEndBA((firstField.getBeginBA() == 0) ? chars.length - 1
                            : firstField.getBeginBA() - 1);

                        for (int cx = 0; cx < firstField.getBeginBA(); cx++)
                        {
                            chars[cx].setField(lastField);
                        }
                    }
                    else
                    {
                        lastField.setEndBA(chars.length - 1);
                    }
                }

                fieldsChanged = false;
            }
        }

        //TODO - check sync of this
        public void resetMDT()
        {
            synchronized (getLockObject())
            {
                Enumeration<TermField> e = fields.elements();

                while (e.hasMoreElements())
                {
                    try
                    {
                        TermField f = e.nextElement();
                        if (!f.isProtected())
                        {
                            f.setModified(false);
                        }
                    }
                    catch (IsProtectedException ipe)
                    {
                        //the field is protected, how can it be modified? Move on.
                        log.finer("the field is protected. pass it");
                    }
                }
            }
        }

        //TODO - check sync of this
        public void clearFields(int startBA, boolean all)
        {
            synchronized (getLockObject())
            {
                Enumeration<TermField> e = fields.elements();

                while (e.hasMoreElements())
                {
                    TermField f = e.nextElement();
                    if (f.getBeginBA() >= startBA && (!f.isProtected() || all))
                    {
                        f.fillChars(' ');
                    }
                }
            }
        }

        public void setFieldsChanged(boolean dirty)
        {
            synchronized (getLockObject())
            {
                this.fieldsChanged = dirty;
            }
        }
    }

}
