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
package com.ascert.open.term.i3270;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.KeyStroke;

import com.ascert.open.ohio.Ohio.OHIO_AID;
import com.ascert.open.term.core.IsProtectedException;
import com.ascert.open.term.core.Terminal;
import com.ascert.open.term.gui.AbstractKeyHandler;

/**
 *
 * @version 1,0 21-Apr-2017
 * @author rhw
 * @history 21-Apr-2017 rhw Created
 */
public class KeyHandler3270 extends AbstractKeyHandler
{

    public KeyHandler3270(Terminal term)
    {
        super(term);

        makeFkeyAction(KeyStroke.getKeyStroke("F1"), OHIO_AID.OHIO_AID_3270_PF1);
        makeFkeyAction(KeyStroke.getKeyStroke("F2"), OHIO_AID.OHIO_AID_3270_PF2);
        makeFkeyAction(KeyStroke.getKeyStroke("F3"), OHIO_AID.OHIO_AID_3270_PF3);
        makeFkeyAction(KeyStroke.getKeyStroke("F4"), OHIO_AID.OHIO_AID_3270_PF4);
        makeFkeyAction(KeyStroke.getKeyStroke("F5"), OHIO_AID.OHIO_AID_3270_PF5);
        makeFkeyAction(KeyStroke.getKeyStroke("F6"), OHIO_AID.OHIO_AID_3270_PF6);
        makeFkeyAction(KeyStroke.getKeyStroke("F7"), OHIO_AID.OHIO_AID_3270_PF7);
        makeFkeyAction(KeyStroke.getKeyStroke("F8"), OHIO_AID.OHIO_AID_3270_PF8);
        makeFkeyAction(KeyStroke.getKeyStroke("F9"), OHIO_AID.OHIO_AID_3270_PF9);
        makeFkeyAction(KeyStroke.getKeyStroke("F10"), OHIO_AID.OHIO_AID_3270_PF10);
        makeFkeyAction(KeyStroke.getKeyStroke("F11"), OHIO_AID.OHIO_AID_3270_PF11);
        makeFkeyAction(KeyStroke.getKeyStroke("F12"), OHIO_AID.OHIO_AID_3270_PF12);
        makeFkeyAction(KeyStroke.getKeyStroke("F13"), OHIO_AID.OHIO_AID_3270_PF13);
        makeFkeyAction(KeyStroke.getKeyStroke("F14"), OHIO_AID.OHIO_AID_3270_PF14);
        makeFkeyAction(KeyStroke.getKeyStroke("F15"), OHIO_AID.OHIO_AID_3270_PF15);
        makeFkeyAction(KeyStroke.getKeyStroke("F16"), OHIO_AID.OHIO_AID_3270_PF16);
        makeFkeyAction(KeyStroke.getKeyStroke("F17"), OHIO_AID.OHIO_AID_3270_PF17);
        makeFkeyAction(KeyStroke.getKeyStroke("F18"), OHIO_AID.OHIO_AID_3270_PF18);
        makeFkeyAction(KeyStroke.getKeyStroke("F19"), OHIO_AID.OHIO_AID_3270_PF19);
        makeFkeyAction(KeyStroke.getKeyStroke("F20"), OHIO_AID.OHIO_AID_3270_PF20);
        makeFkeyAction(KeyStroke.getKeyStroke("F21"), OHIO_AID.OHIO_AID_3270_PF21);
        makeFkeyAction(KeyStroke.getKeyStroke("F22"), OHIO_AID.OHIO_AID_3270_PF22);
        makeFkeyAction(KeyStroke.getKeyStroke("F23"), OHIO_AID.OHIO_AID_3270_PF23);
        makeFkeyAction(KeyStroke.getKeyStroke("F24"), OHIO_AID.OHIO_AID_3270_PF24);

        // shift variants
        makeFkeyAction(KeyStroke.getKeyStroke("shift F1"), OHIO_AID.OHIO_AID_3270_PF13);
        makeFkeyAction(KeyStroke.getKeyStroke("shift F2"), OHIO_AID.OHIO_AID_3270_PF14);
        makeFkeyAction(KeyStroke.getKeyStroke("shift F3"), OHIO_AID.OHIO_AID_3270_PF15);
        makeFkeyAction(KeyStroke.getKeyStroke("shift F4"), OHIO_AID.OHIO_AID_3270_PF16);
        makeFkeyAction(KeyStroke.getKeyStroke("shift F5"), OHIO_AID.OHIO_AID_3270_PF17);
        makeFkeyAction(KeyStroke.getKeyStroke("shift F6"), OHIO_AID.OHIO_AID_3270_PF18);
        makeFkeyAction(KeyStroke.getKeyStroke("shift F7"), OHIO_AID.OHIO_AID_3270_PF19);
        makeFkeyAction(KeyStroke.getKeyStroke("shift F8"), OHIO_AID.OHIO_AID_3270_PF20);
        makeFkeyAction(KeyStroke.getKeyStroke("shift F9"), OHIO_AID.OHIO_AID_3270_PF21);
        makeFkeyAction(KeyStroke.getKeyStroke("shift F10"), OHIO_AID.OHIO_AID_3270_PF22);
        makeFkeyAction(KeyStroke.getKeyStroke("shift F11"), OHIO_AID.OHIO_AID_3270_PF23);
        makeFkeyAction(KeyStroke.getKeyStroke("shift F12"), OHIO_AID.OHIO_AID_3270_PF24);

        // Alt variants
        makeFkeyAction(KeyStroke.getKeyStroke("alt F1"), OHIO_AID.OHIO_AID_3270_PA1);
        makeFkeyAction(KeyStroke.getKeyStroke("alt F2"), OHIO_AID.OHIO_AID_3270_PA2);
        makeFkeyAction(KeyStroke.getKeyStroke("alt F3"), OHIO_AID.OHIO_AID_3270_PA3);

        makeFkeyAction(KeyStroke.getKeyStroke("PAGE_UP"), OHIO_AID.OHIO_AID_3270_PA1);
        makeFkeyAction(KeyStroke.getKeyStroke("PAGE_DOWN"), OHIO_AID.OHIO_AID_3270_PA2);
        makeFkeyAction(KeyStroke.getKeyStroke("ENTER"), OHIO_AID.OHIO_AID_3270_ENTER);
        makeFkeyAction(KeyStroke.getKeyStroke("ESCAPE"), OHIO_AID.OHIO_AID_3270_CLEAR);

        makeFkeyAction(KeyStroke.getKeyStroke("control S"), OHIO_AID.OHIO_AID_3270_SYSREQ);
        makeFkeyAction(KeyStroke.getKeyStroke("control C"), OHIO_AID.OHIO_AID_3270_CLEAR);

        // Below are specific key binding alternatives from original Freehost3270 code. 
        // Presumably they map onto standard/common bindings that a 3270 emulator user would expect
        //TODO - no idea why F was mapped to TAB, but it means you cannot enter an 'F' which is useless
        //       and doesn't seem necessary since TAB works anyway
        //getInputMap().put(KeyStroke.getKeyStroke("F"), "TAB");
        getInputMap().put(KeyStroke.getKeyStroke("control F"), "shift TAB");
        getInputMap().put(KeyStroke.getKeyStroke("control E"), "DELETE");

        makeKeyAction("control M", new FieldMarkAction());
        makeKeyAction("control N", new NewLineAction());
        makeKeyAction("control R", new ResetAction());

// Not sure what this original key handling code was meant to do        
//                        case KeyEvent.VK_T: {
//                            if (evt.isControlDown()) {
//                                KeyboardFocusManager.getCurrentKeyboardFocusManager()
//                                                    .focusNextComponent(this);
//                            } else if (evt.isMetaDown() || evt.isAltDown()) {
//                                KeyboardFocusManager.getCurrentKeyboardFocusManager()
//                                                    .focusPreviousComponent(this);
//                            }
//                        }
    }

    //////////////////////////////////////////////////
    // INTERFACE METHODS - KeyHandler
    //////////////////////////////////////////////////
    public JButton[][] getFKeyButtons()
    {
        JButton[][] btns = new JButton[2][15];
        int ix = 0;
        btns[0][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF1);
        btns[0][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF2);
        btns[0][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF3);
        btns[0][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF4);
        btns[0][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF5);
        btns[0][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF6);
        btns[0][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF7);
        btns[0][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF8);
        btns[0][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF9);
        btns[0][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF10);
        btns[0][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF11);
        btns[0][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF12);
        btns[0][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PA1);
        btns[0][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PA2);
        btns[0][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PA3);

        ix = 0;
        btns[1][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF13);
        btns[1][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF14);
        btns[1][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF15);
        btns[1][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF16);
        btns[1][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF17);
        btns[1][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF18);
        btns[1][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF19);
        btns[1][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF20);
        btns[1][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF21);
        btns[1][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF22);
        btns[1][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF23);
        btns[1][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_PF24);
        btns[1][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_ENTER, "ENT");
        btns[1][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_CLEAR, "CLR");
        btns[1][ix++] = makeButton(OHIO_AID.OHIO_AID_3270_SYSREQ, "SYS");

        return btns;
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
    // These actions were all empty in the original Freehost3270 code.
    // But presumably they signified some part of the standard spec of 3270 handling, so have
    // been left in as markers/placeholders
    public class FieldMarkAction extends KeyAction
    {

        public boolean handle() throws IsProtectedException
        {
            return false;
        }
    }

    // This seems like it may have been a synonym for CR or LF??
    public class NewLineAction extends KeyAction
    {

        public boolean handle() throws IsProtectedException
        {
            return false;
        }
    }

    public class ResetAction extends KeyAction
    {

        public boolean handle() throws IsProtectedException
        {
            return false;
        }
    }

}
