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
package com.ascert.open.term.gui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.KeyStroke;

import com.ascert.open.ohio.Ohio;
import com.ascert.open.term.core.InputCharHandler;
import com.ascert.open.term.core.IsProtectedException;
import com.ascert.open.term.core.TermChar;
import com.ascert.open.term.core.TermField;
import com.ascert.open.term.core.Terminal;

/**
 *
 * @version 1,0 21-Apr-2017
 * @author rhw

 */
public abstract class AbstractKeyHandler implements KeyHandler
{
    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////

    private static final Logger log = Logger.getLogger(AbstractKeyHandler.class.getName());

    protected ActionMap actMap = new ActionMap();
    protected InputMap inpMap = new InputMap();
    protected Terminal term;

    private final MyKeyListener keyListener;

    protected InputCharHandler charHandler;

    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    public AbstractKeyHandler(Terminal term)
    {
        this.term = term;
        this.keyListener = new MyKeyListener();
        this.charHandler = new MyCharHandler();
        addCommonKeys();
    }

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    public void addCommonKeys()
    {
        // These are fairly standard bindings. They can be replaced or removed as needed in specific implementations
        makeKeyAction("UP", new UpAction());
        makeKeyAction("DOWN", new DownAction());
        makeKeyAction("LEFT", new LeftAction());
        makeKeyAction("RIGHT", new RightAction());
        makeKeyAction("HOME", new HomeAction());
        makeKeyAction("END", new EndAction());

        makeKeyAction("DELETE", new DeleteAction());
        makeKeyAction("TAB", new TabAction());
        makeKeyAction("shift TAB", new BacktabAction());
        makeKeyAction("BACK_SPACE", new BackspaceAction());
    }

    public Action getAction(final Ohio.OHIO_AID aid)
    {
        final String name = aid.getAidMnemonic().toUpperCase();
        Action act = actMap.get(name);
        if (act == null)
        {
            act = new AbstractAction(name)
            {
                public void actionPerformed(ActionEvent arg0)
                {
                    log.finest("AID/F-key pressed: " + aid);
                    term.Fkey(aid);
                    // Need to refresh in case lock changed
                    term.getClient().refresh();
                }
            };
            getActionMap().put(name, act);
        }
        return act;
    }

    public Action makeFkeyAction(KeyStroke kStroke, final Ohio.OHIO_AID aid)
    {
        Action act = getAction(aid);
        getInputMap().put(kStroke, act.getValue(Action.NAME));
        return act;
    }

    public Action makeKeyAction(String keyName, Action act)
    {
        act.putValue(Action.NAME, keyName);
        getActionMap().put(keyName, act);
        KeyStroke kStroke = KeyStroke.getKeyStroke(keyName);
        getInputMap().put(kStroke, keyName);
        return act;
    }

    //TODO - there's a focus issue with these buttons. Once clicked, keyboard entry is no longer handled properly
    public JButton makeButton(final Ohio.OHIO_AID aid, String text)
    {
        JButton button = makeButton(aid);
        button.setText(text);
        return button;
    }

    public JButton makeButton(Ohio.OHIO_AID aid)
    {
        JButton btn = new JButton(actMap.get(aid.getAidMnemonic().toUpperCase()));
        // Probably not ideal, but if button gets focus then keys seem to stop working (and SPACE key then triggers the focused
        // button action).
        btn.setFocusable(false);
        return btn;
    }

    //////////////////////////////////////////////////
    // INTERFACE METHODS - KeyHandler
    //////////////////////////////////////////////////
    public InputMap getInputMap(InputMap parent)
    {
        if (parent != null)
        {
            if (parent instanceof ComponentInputMap && !(this.inpMap instanceof ComponentInputMap))
            {
                // We can't simply return our map, we need to create a cloned copy of the correct type
                ComponentInputMap newMap = new ComponentInputMap(((ComponentInputMap) parent).getComponent());

                if (inpMap.size() > 0)
                {
                    for (KeyStroke key : inpMap.keys())
                    {
                        newMap.put(key, inpMap.get(key));
                    }
                }

                inpMap = newMap;
            }

            inpMap.setParent(parent);
        }

        return inpMap;
    }

    public InputMap getInputMap()
    {
        return getInputMap(null);
    }

    public ActionMap getActionMap(ActionMap parent)
    {
        if (parent != null)
        {
            actMap.setParent(parent);
        }
        return actMap;
    }

    public ActionMap getActionMap()
    {
        return getActionMap(null);
    }

    public KeyListener getKeyListener()
    {
        return this.keyListener;
    }

    public void doKeyAction(String keyName, boolean clientRefresh, boolean observeKbdLock)
    {
        doKeyAction(KeyStroke.getKeyStroke(keyName), clientRefresh, observeKbdLock);
    }
    
    public void doKeyAction(KeyStroke keyStroke, boolean clientRefresh, boolean observeKbdLock)
    {
        Object kObj = inpMap.get(keyStroke);
        
        if (keyStroke == null || kObj == null)
        {
            return;
        }
        
        Action act = actMap.get(kObj);
        
        if (act == null)
        {
            return;
        }

        if (act instanceof KeyAction)
        {
            ((KeyAction) act).doAction(clientRefresh, observeKbdLock);
            return;
        }

        ActionEvent actEvt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, keyStroke.toString());
        act.actionPerformed(actEvt);
    }
    

    /**
     * @return the charHandler
     */
    public InputCharHandler getCharHandler()
    {
        return charHandler;
    }

    //////////////////////////////////////////////////
    // PRIVATE INSTANCE METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // STATIC INNER CLASSES
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // NON-STATIC INNER CLASSES
    //////////////////////////////////////////////////
    public class MyKeyListener extends KeyAdapter
    {

        public void keyTyped(KeyEvent evt)
        {
            char typedChar = evt.getKeyChar();

            if (term.getClient().clearStatus())
            {
                evt.consume();
            }

            // Not sure if there is a better way to skip non-character keys
            if ((typedChar != KeyEvent.CHAR_UNDEFINED) && (!evt.isControlDown() && !evt.isAltDown() && !evt.isMetaDown()) && !(typedChar
                                                                                                                               == KeyEvent.VK_TAB)
                && !(typedChar == KeyEvent.VK_BACK_SPACE) && !(typedChar == KeyEvent.VK_DELETE) && !(typedChar == KeyEvent.VK_ENTER)
                && !(typedChar == KeyEvent.VK_ESCAPE))
            {
                // the typed key generated some character
                try
                {
                    log.finest("typed char: " + typedChar);
                    if (getCharHandler().type(typedChar));
                    {
                        term.getClient().refresh();
                    }
                    evt.consume();
                }
                catch (Exception e)
                {
                    log.warning(e.getMessage());
                }
            }
            else
            {
                log.finest("unhandled key: " + evt);
            }
        }
    }

    public class MyCharHandler implements InputCharHandler
    {

        /**
         * Inserts the specified ASCII character at the current cursor position if the current field is unprotected, and advances the cursor
         * position by one. This is useful for implementations that accept keyboard input directly. For implementations that don't require
         * character-by-character input, use RW3270Field.setData(String data) instead.
         *
         * @param key keyboard/ASCII character corresponding to the key pressed.
         *
         * @throws IsProtectedException if the current field is protected.
         *
         * @see RW3270Field
         */
        public boolean type(char key, boolean updateDisplay) throws IsProtectedException, IOException
        {
            if (term.isKeyboardLocked())
            {
                return false;
            }

            int oldPos = term.getCursorPosition();
            TermField f = term.getField(oldPos);
            TermChar ch = term.getChar(oldPos);

            if (ch.isStartField() || f == null || f.isProtected())
            {
                throw new IsProtectedException();
            }

            if (!f.isValidInput(key))
            {
                term.getClient().beep();
                return false;
            }

            ch.setChar(f.applyTransform(key));
            f.setModified(true);

            int newPos = oldPos + 1;
            if (newPos > f.getEndBA() && !f.isAutoTab())
            {
                term.getClient().beep();
                return true;
            }

            term.setCursorPosition(newPos);
            return true;
        }

    }

    public class KeyAction extends AbstractAction
    {

        boolean refresh;
        boolean observeKbdLock;

        public KeyAction()
        {
            this(true, true);
        }

        public KeyAction(boolean refresh, boolean observeKbdLock)
        {
            this.refresh = refresh;
            this.observeKbdLock = observeKbdLock;
        }

        @Override
        public void actionPerformed(ActionEvent evt)
        {
            log.finest("key action: " + evt + ", at pos:  " + term.getCursorPosition());
            doAction(refresh, observeKbdLock);
        }

        public void doAction(boolean withRefresh, boolean observeKbdLock)
        {
            try
            {
                if (observeKbdLock && term.isKeyboardLocked())
                {
                    //TODO - one possible central place to add type-ahead handling for bound keys when locked
                    return;
                }

                if (handle())
                {
                    if (withRefresh)
                    {
                        term.getClient().refresh();
                    }
                }
            }
            catch (Exception ex)
            {
                log.warning("Exception handling action: " + ex);
                log.log(Level.FINE, "", ex);
            }
        }

        public boolean handle() throws Exception
        {
            return false;
        }
    }

    /**
     * Moves the cursor position 'up' one row from it's current position.
     * <p>
     * For example, in an 80-column screen, calling the <code>up()</code> mehtod will decrease the cursor position by 80. This method will
     * 'wrap' from the first row to the last row
     * </p>
     */
    public class UpAction extends KeyAction
    {

        public boolean handle()
        {
            term.setCursorPosition(term.getCursorPosition() - term.getCols());
            return true;
        }
    }

    /**
     * Moves the cursor position 'down' one row from it's current position.
     *
     * <p>
     * For example, in an 80-column screen, calling the <code>down()</code> mehtod will increase the cursor position by 80. This method will
     * 'wrap' from the last row to the first row
     * </p>
     */
    public class DownAction extends KeyAction
    {

        public boolean handle()
        {
            term.setCursorPosition(term.getCursorPosition() + term.getCols());
            return true;
        }
    }

    /**
     * Moves the cursor position one character to the left, wrapping when necessary. Returns without moving the cursor if the terminal is
     * currently locked.
     */
    public class LeftAction extends KeyAction
    {

        public boolean handle()
        {
            term.setCursorPosition(term.getCursorPosition() - 1);
            return true;
        }
    }

    /**
     * Moves the cursor position one character to the right, wrapping when necessary.
     *
     * <p>
     * Returns without moving the cursor if the terminal is currently locked.
     * </p>
     */
    public class RightAction extends KeyAction
    {

        public boolean handle()
        {
            term.setCursorPosition(term.getCursorPosition() + 1);
            return true;
        }
    }

    /**
     * This method moves the cursor to the first character of the first unprotected field in the data buffer.
     */
    public class HomeAction extends KeyAction
    {

        public boolean handle()
        {
            //TODO - check handling when no fields
            int pos = term.getNextUnprotectedField(0);
            term.setCursorPosition(pos);
            return true;
        }
    }

    /**
     * This method moves the cursor to the first character of the last unprotected field in the data buffer.
     */
    public class EndAction extends KeyAction
    {

        public boolean handle()
        {
            //TODO - check handling when no fields
            int pos = term.getPreviousUnprotectedField(0);
            term.setCursorPosition(pos);
            return true;
        }
    }

    /**
     * Advances the cursor position to the first character position of the next unprotected field.
     */
    public class TabAction extends KeyAction
    {

        public boolean handle()
        {
            term.setCursorPosition(term.getNextUnprotectedField(term.getCursorPosition()));
            return true;
        }
    }

    /**
     * This method sets the cursor position to the first character position of the last unprotected field.
     */
    public class BacktabAction extends KeyAction
    {

        public boolean handle()
        {
            term.setCursorPosition(term.getPreviousUnprotectedField(term.getCursorPosition()));
            return true;
        }
    }

    /**
     * Deletes the current character (by setting it to ' ') and decrements the cursor position by one.
     *
     * @throws IsProtectedException if the backspace will go into a protected field
     */
    public class BackspaceAction extends KeyAction
    {

        public boolean handle() throws IsProtectedException
        {
            int newPos = term.getCursorPosition() - 1;
            TermChar ch = term.getChar(newPos);
            if (ch.getField().isProtected() || ch.isStartField())
            {
                throw new IsProtectedException();
            }

            term.setCursorPosition(newPos);
            ch.getField().setModified(true);
            ch.setChar(' ');    //TODO - wonder if we should use clear to reset any video attributes??
            return true;
        }
    }

    /**
     * Deletes the current character (by setting it to ' ').
     *
     * @throws IsProtectedException if the current field is protected.
     */
    public class DeleteAction extends KeyAction
    {

        public boolean handle() throws IsProtectedException
        {
            int pos = term.getCursorPosition();
            TermField fld = term.getChar(pos).getField();
            if (fld.isProtected())
            {
                throw new IsProtectedException();
            }

            for (int ix = pos; ix < fld.getEndBA(); ix++)
            {
                term.getChar(ix).setChar(term.getChar(ix + 1).getChar());
            }

            TermChar ch = term.getChar(fld.getEndBA());
            ch.setChar(' ');
            ch.getField().setModified(true);
            return true;
        }
    }

}
