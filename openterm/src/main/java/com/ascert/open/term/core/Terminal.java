/*
 * Copyright (c) 2016, 2017 Ascert, LLC.
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

import java.awt.Color;
import java.awt.Image;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Vector;

import com.ascert.open.ohio.Ohio;

import com.ascert.open.term.core.AbstractTerminal.Page;
import com.ascert.open.term.gui.KeyHandler;

/**
 *
 * @version 1,0 24-Apr-2017
 * @author rhw
 * @history 24-Apr-2017 rhw Created
 */
public interface Terminal
{

    public static enum CursorStyle
    {
        NONE, BLOCK, SQUARE, VERTICAL, HORIZONTAL
    };

    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////
    void Fkey(Ohio.OHIO_AID key);

    void Fkey(int key);

    default InputCharHandler getCharHandler()
    {
        return getKeyHandler().getCharHandler();
    }

    TermChar getChar(int i);

    default Object getLockObject()
    {
        return this;
    }

    /**
     *
     * @return The current AID value, or 0x00 for none.
     */
    default short getAIDValue()
    {
        Ohio.OHIO_AID aid = getAIDEnum();
        return (aid != null) ? (short) aid.getAidValue() : 0;
    }

    Ohio.OHIO_AID getAIDEnum();

    /**
     * Searches the current data buffer (screen) for the specified string.
     *
     * @param search The string to search the data buffer for.
     *
     * @return <code>true</code> if the string was found, <code>false</code> otherwise.
     */
    boolean contains(String search);

    /**
     * DOCUMENT ME!
     *
     * @return The number of cols in the current screen
     */
    int getCols();

    /**
     * DOCUMENT ME!
     *
     * @return The current cursor position
     */
    int getCursorPosition();

    // In terminals with local type-ahead buffers, the local cursor display position may be different
    // to the last cursor position set by the host
    default int getDisplayCursorPosition()
    {
        return getCursorPosition();
    }

    default CursorStyle getCursorStyle()
    {
        return CursorStyle.BLOCK;
    }

    /**
     * Gets the current screen's display characters. This method is useful for getting the characters necessary for display in the client
     * implementation. It automatically suppresses hidden (password, etc.) fields and null characters.
     *
     * @return The current 3270 screen as an array of characters.
     */
    char[] getDisplay();

    /**
     * This method returns the RW3270Field object that the current cursor position is in.
     *
     * @return RW3270Field object.
     */
    TermField getField();

    /**
     * This method returns the RW3270Field object at the specified buffer address
     *
     * @param i Screen position of the requested RW3270Field Object.
     *
     * @return RW3270Field object
     */
    TermField getField(int i);

    /**
     * Returns a vector of fields representing the current screen.
     *
     * @return the field Vector
     */
    Vector getFields();

    /**
     * Returns the first character position of the next unprotected field from the current cursor position.
     *
     * @param pos DOCUMENT ME!
     *
     * @return The next unprotected field's address, starting from the current cursor position.
     */
    short getNextUnprotectedField(int pos);

    /**
     * This method returns the first character position of the previous field from the current cursor position.
     *
     * @param pos DOCUMENT ME!
     *
     * @return The previous unprotected field's address, starting from the current cursor position
     */
    short getPreviousUnprotectedField(int pos);

    /**
     * DOCUMENT ME!
     *
     * @return The number of rows in the current screen
     */
    int getRows();

    int getCurrentRow();

    int getNextRow(boolean wrap);

    int getPreviousRow(boolean wrap);

    public int getCurrentCol();

    /**
     * Allows access to low level screen and telnet handling classes
     *
     * @return the tnParser
     */
    TnStreamParser getStreamParser();

    /**
     * Allows access to low level screen and telnet handling classes
     *
     * @return the tn
     */
    RWTelnet getTelnet();

    /**
     * This method is designed to let implementations check to see if the 'keyboard' is currently locked.
     *
     * @return <code>true</code> if the screen is not currently accepting input, <code>false</code> if it is.
     */
    boolean isKeyboardLocked();

    void setKeyboardLocked(boolean locked);

    void addKbdLockListener(KbdLockListener listener);

    void removeKbdLockListener(KbdLockListener listener);

    // TODO - actually should be able to cope with null client e.g. a do-nothing stub
    void setClient(TnAction client);

    /**
     * This method sets the cursor position.
     *
     * @param newCursorPos The new cursor position
     */
    void setCursorPosition(int newCursorPos);

    /**
     * This method sets the cursor position and updates client
     *
     * @param newCursorPos The new cursor position
     */
    void setCursorPosition(int newCursorPos, boolean updateClient);

    /**
     * This method implements connections directly to a host
     *
     * @param host       The hostname of the TN3270 host to connect to
     * @param port       The port on which to connect to the TN3270 host
     * @param encryption
     *
     * @throws IOException          DOCUMENT ME!
     * @throws UnknownHostException DOCUMENT ME!
     */
    default void connect(String host, int port, boolean encryption) throws IOException, UnknownHostException
    {
        setHost(new Host(host, port, getTermType(), encryption));
        connect();
    }

    void connect() throws IOException, UnknownHostException;
    
    //TODO - should probably abstract out some session handling object, with listener updates
    //       for objects needing status

    /**
     * Disconnects this RW3270 object from the current Session.
     */
    void disconnect();

    /**
     * Sets the encryption setting for the 3270 session...
     *
     * @param encryption will turn encryption on, false will turn encryption off
     *
     */
    void setEncryption(boolean encryption);

    TnAction getClient();

    String getTermType();

    // Page handling Bit messy!
    int getMaxPages();

    Page getPage(int pg);

    Page getActivePage();

    int getActivePageNumber();

    void setActivePage(int pg);

    Page getDisplayPage();

    int getDisplayPageNumber();

    void setDisplayPage(int pg);

    /**
     * Returns an array of Term3270Char objects representing the data buffer.
     *
     * @return the Term3270Char array
     */
    TermChar[] getCharBuffer();

    public String getCharString(int startBA, int len);

    /**
     * @return the writeBA
     */
    int getWriteBA();

    /**
     * @param writeBA the writeBA to set
     */
    void setWriteBA(int writeBA);

    default void buildFields()
    {
        // default to force, in case no dirty marking/handling
        buildFields(true);
    }

    void buildFields(boolean force);

    // for implementations that prefer to build status line in 1 go e.g. on screen refresh
    // rather than on individual methods
    default TermChar[] getStatusLine()
    {
        return null;
    }

    void resetMDT();

    void clearFields(int startBA, boolean all);

    void setFieldsChanged(boolean dirty);

    // Possible this might move to some separate "gui" interface for a Terminal in future
    public KeyHandler getKeyHandler();

    // Mainly used for external control e.g. simulating a user pressing keys at the emulator
    default void doKeyPress(String keyName)
    {
        getKeyHandler().doKeyAction(keyName, true);
    }

    default TermChar getDefaultVAChar()
    {
        return null;
    }

    void setTelnet(RWTelnet tn);

    default boolean isColorMapping()
    {
        return false;
    }

    default Color getForegroundColor(Color dflt)
    {
        return dflt;
    }

    default Color getBackgroundColor(Color dflt)
    {
        return dflt;
    }

    default Color getStatusBorderColor(Color dflt)
    {
        return dflt;
    }

    default Image getOverlayImage()
    {
        return null;
    }
    
    /**
     * @return the host
     */
    public Host getHost();

    /**
     * @param host the host to set
     */
    public void setHost(Host host);
    
}
