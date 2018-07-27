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

import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ascert.open.ohio.Ohio;
import com.ascert.open.ohio.OhioField;
import com.ascert.open.ohio.OhioFields;
import com.ascert.open.ohio.OhioOIA;
import com.ascert.open.ohio.OhioOIAListener;
import com.ascert.open.ohio.OhioPosition;
import com.ascert.open.ohio.OhioScreen;
import com.ascert.open.ohio.OhioScreenListener;
import com.ascert.open.term.gui.EmulatorPanel;

/**
 * Thin set of wrapper classes providing the more significant OHIO API bindings into the terminal emulation layer. Note that Java use of
 * OHIO doesn't seem to have ever become a real standard so the interface/API classes are really just the best available open source version
 * at the time of writing. It's unlikely that they'll serve as a real portability layer unless adopted by other emulators. Additional, event
 * listener interfaces were never part of the original OHIO model. Although they do serve a useful added value here, they were also not in
 * any way a standard. Regardless of these issues, the OHIO classes to provide an API abstraction, and one which follows a model used
 * elsewhere. So there is valued on building on them rather than re-inventing the wheel.
 *
 *
 * @version 1,0 25-Apr-2017
 * @author rhw
 * @history 25-Apr-2017 srm Created
 */
public class OhioScreenWrapper implements OhioScreen
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    Terminal term;
    OhioOIA oia;

    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    public OhioScreenWrapper(Terminal term)
    {
        this.term = term;
        this.oia = new FhOhioOIAWrapper();
    }

    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // INTERFACE METHODS - OhioScreen
    //////////////////////////////////////////////////
    @Override
    public OhioPosition getCursor()
    {
        return OhioPosition.convertScreenOffsetToPosition(term.getCursorPosition(), term.getCols());
    }

    @Override
    public void setCursor(OhioPosition cursorPos)
    {
        //TODO - should only update UI if we have one rendered (e.g. in emulator or ghost/replay mode), then we probably want to refresh the display here
        term.setCursorPosition((short) OhioPosition.convertPositionToScreenOffset(cursorPos, term.getCols()), true);
    }

    @Override
    public OhioOIA getOIA()
    {
        return this.oia;
    }

    @Override
    public OhioFields getFields()
    {
        return new FhOhioFields(term.getFields());
    }

    @Override
    public int getRows()
    {
        return term.getRows();
    }

    @Override
    public int getColumns()
    {
        return term.getCols();
    }

    @Override
    public String getString()
    {
        // Ensure nulls are returned as spaces as per spec for this method
        // (attributes etc already seem to be handled by freehost3270)
        return new String(term.getDisplay());
    }

    @Override
    public char[] getData(OhioPosition start, OhioPosition end, Ohio.OHIO_PLANE plane)
    {
        throw new UnsupportedOperationException("Plane not supported yet: " + plane);
    }

    @Override
    public OhioPosition findString(String targetString, OhioPosition start, int length, Ohio.OHIO_DIRECTION dir, boolean ignoreCase)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void sendKeys(String text, OhioPosition location)
    {
        if (location != null)
        {
            //TODO - technically, we possibly should not change cursor pos
            setCursor(location);
        }

        for (int ix = 0; ix < text.length(); ix++)
        {
            try
            {
                term.getCharHandler().type(text.charAt(ix));
            }
            catch (Exception ex)
            {
                //TODO - need to handle or re-throw this
                Logger.getLogger(EmulatorPanel.class.getName()).log(Level.SEVERE, "attempt to sendKeys in protected field", ex);
            }
        }
    }

    @Override
    public void sendAid(int aidKey)
    {
        term.Fkey(aidKey);
    }

    @Override
    public void setString(String text, OhioPosition location)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addScreenListener(OhioScreenListener listener)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeScreenListener(OhioScreenListener listener)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    //////////////////////////////////////////////////
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // PRIVATE INSTANCE METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // STATIC INNER CLASSES
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // NON-STATIC INNER CLASSES
    //////////////////////////////////////////////////
    public class FhOhioFields implements OhioFields
    {

        //TODO - improve to use something more up to date!!!
        Vector fields;

        private FhOhioFields(Vector fields)
        {
            this.fields = fields;
        }

        @Override
        public int getCount()
        {
            return fields.size();
        }

        @Override
        public OhioField item(int fieldIndex)
        {
            return new FhOhioField((TermField) fields.elementAt(fieldIndex));
        }

        @Override
        public void refresh()
        {
            fields = term.getFields();
        }

        @Override
        public OhioField findByString(String targeString, OhioPosition startPos, int length, Ohio.OHIO_DIRECTION dir, boolean ignoreCase)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public OhioField findByPosition(OhioPosition targetPosition)
        {
            short off = (short) OhioPosition.convertPositionToScreenOffset(targetPosition, term.getCols());
            TermField fld = term.getField(off);
            return fld != null ? new FhOhioField(fld) : null;
        }

    }

    //TODO - from 3270 - bold, fg color, bg color
    public class FhOhioField implements OhioField
    {

        TermField fld;

        private FhOhioField(TermField fld)
        {
            this.fld = fld;
        }

        @Override
        public OhioPosition getStart()
        {
            return OhioPosition.convertScreenOffsetToPosition(fld.getBeginBA(), term.getCols());
        }

        @Override
        public OhioPosition getEnd()
        {
            return OhioPosition.convertScreenOffsetToPosition(fld.getEndBA(), term.getCols());
        }

        @Override
        public int getLength()
        {
            return fld.size();
        }

        @Override
        public int getAttribute()
        {
            return fld.getFieldAttribute();
        }

        @Override
        public boolean isModified()
        {
            return fld.isModified();
        }

        @Override
        public boolean isProtected()
        {
            return fld.isProtected();
        }

        @Override
        public boolean isNumeric()
        {
            return fld.isNumeric();
        }

        @Override
        public boolean isHighIntensity()
        {
            return fld.getFAChar().isAltIntensity();
        }

        @Override
        public boolean isPenSelectable()
        {
            //TODO - from Ohio spec - PEN SELECTABLE
            return false;
        }

        @Override
        public boolean isHidden()
        {
            return fld.getFAChar().isHidden();
        }

        @Override
        public String getString()
        {
            return new String(fld.getDisplayChars()).replace('\0', ' ');
        }

        @Override
        public void setString(String text)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public char[] getData(Ohio.OHIO_PLANE targetPlane)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public String toString()
        {
            StringBuffer buf = new StringBuffer();

            buf.append("start: [" + getStart() + "], ");
            buf.append("end: [" + getEnd() + "], ");
            buf.append("len: [" + getLength() + "], ");
            buf.append("val: [" + getString() + "], ");
            // Mask out top 2 bits which just make it printable char
            buf.append("attr: [0x" + Integer.toHexString(getAttribute() & 0x3f) + " - ");

            if (isModified())
            {
                buf.append("MOD ");
            }
            if (isProtected())
            {
                buf.append("PROT ");
            }
            if (isNumeric())
            {
                buf.append("NUM ");
            }
            if (isHighIntensity())
            {
                buf.append("HI ");
            }
            if (isPenSelectable())
            {
                buf.append("PEN ");
            }
            if (isHidden())
            {
                buf.append("HID ");
            }
            buf.append("]");

            return buf.toString();
        }
    }

    public class FhOhioOIAWrapper implements OhioOIA
    {

        Map<OhioOIAListener, FhOhioOIAListenerWrapper> listenerMap = new ConcurrentHashMap<>();

        @Override
        public boolean isAlphanumeric()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isNumeric()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public int getCommCheckCode()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Ohio.OHIO_INPUTINHIBITED getInputInhibited()
        {
            //TODO - don't presently have way to detect sub-states
            return term.isKeyboardLocked() ? Ohio.OHIO_INPUTINHIBITED.OHIO_INPUTINHIBITED_SYSTEM_WAIT
                : Ohio.OHIO_INPUTINHIBITED.OHIO_INPUTINHIBITED_NOTINHIBITED;
        }

        @Override
        public int getMachineCheckCode()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Ohio.OHIO_OWNER getOwner()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public int getProgCheckCode()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void addOIAListener(OhioOIAListener listener)
        {
            FhOhioOIAListenerWrapper listenerWrap = listenerMap.put(listener, new FhOhioOIAListenerWrapper(listener));
        }

        @Override
        public void removeOIAListener(OhioOIAListener listener)
        {
            FhOhioOIAListenerWrapper listenerWrap = listenerMap.remove(listener);
            if (listenerWrap != null)
            {
                listenerWrap.destruct();
            }
        }

    }

    // bit f-ugly, but avoids creating a double listener chain where really it's a 1:1 mapping
    class FhOhioOIAListenerWrapper implements KbdLockListener
    {

        OhioOIAListener listener;

        public FhOhioOIAListenerWrapper(OhioOIAListener listener)
        {
            this.listener = listener;
            term.addKbdLockListener(this);
        }

        public void destruct()
        {
            term.removeKbdLockListener(this);
        }

        @Override
        public void kbdLockChanged(Terminal term)
        {
            listener.onOIAChanged(getOIA());
        }
    }

}
