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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @version 1,0 03-May-2017
 * @author srm

 */
public abstract class AbstractTermChar implements TermChar, Cloneable
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    private static final Logger log = Logger.getLogger(AbstractTermChar.class.getName());

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    public static boolean testBooleanAttribute(Object attrVal)
    {
        if (attrVal != null && attrVal instanceof Boolean)
        {
            return (Boolean) attrVal;
        }

        return false;
    }

    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    protected char character;
    protected int position;

    // Whetehr default char state is protected or unprotected. Varies by device type
    protected boolean dfltProtected = true;

    // Reference to any field this char is in (also then links to the field attribute char)
    protected TermField field;
    // Reference to any video preceding video attributes which apply to this char
    protected TermChar vaChar;

    // Some devices allow video attribute changes without starting a new field, so we model these separately
    protected Map<VideoAttribute, Object> vidAttrMap = new HashMap<>();
    protected Map<FieldAttribute, Object> fldAttrMap = new HashMap<>();

    ;
    
    // These methods return the actual attributes held on this character position (if any)
    // The isXXX methods can be used to find attributes that apply to this character position 
    // because of a preceding attribute character
    
    public Map<VideoAttribute, Object> getVideoAttributes()
    {
        return vidAttrMap;
    }

    public Map<FieldAttribute, Object> getFieldAttributes()
    {
        return fldAttrMap;
    }

    public boolean hasVideoAttributes()
    {
        return !vidAttrMap.isEmpty();
    }

    public boolean hasFieldAttributes()
    {
        return !fldAttrMap.isEmpty();
    }

    public Object getVideoAttribute(VideoAttribute vidAttr)
    {
        return vidAttrMap.get(vidAttr);
    }

    public Object getFieldAttribute(FieldAttribute fldAttr)
    {
        return fldAttrMap.get(fldAttr);
    }

    public void setVideoAttribute(VideoAttribute vidAttr, Object attrVal)
    {
        vidAttrMap.put(vidAttr, attrVal);
    }

    public void setFieldAttribute(FieldAttribute fldAttr, Object attrVal)
    {
        fldAttrMap.put(fldAttr, attrVal);
    }

    public void clearVideoAttribute(VideoAttribute vidAttr)
    {
        vidAttrMap.remove(vidAttr);
    }

    public void clearFieldAttribute(FieldAttribute fldAttr)
    {
        fldAttrMap.remove(fldAttr);
    }

    public void setVAChar(TermChar ch)
    {
        this.vaChar = ch;
    }

    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    public AbstractTermChar(int position)
    {
        this.position = position;
        clear();
    }

    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    /**
     * DOCUMENT ME!
     *
     * @return the representation of this character
     */
    public String toString()
    {
        StringBuffer rep = new StringBuffer("(char ");
        rep.append((short) getChar());
        rep.append(", position=").append(getPositionBA());
        //rep.append(":background ").append(getBackground());
        //rep.append(":foreground ").append(getForeground());
        //rep.append(":highlighting ").append(getHighlighting());
        //rep.append(":outlining ").append(getOutlining());
        rep.append(", alt-intensity=").append((isAltIntensity()));
        rep.append(", hidden=").append((isHidden()));
        rep.append(", numeric=").append((isNumeric()));
        rep.append(", protected=").append((isProtected()));
        rep.append(", start-field=").append(isStartField());
        rep.append(", modified=").append(isModified());
        rep.append(")");

        return rep.toString();

        //         char c = (character == 0) ? ' ' : character;
        //         return new Character(c).toString();
    }

    //////////////////////////////////////////////////
    // INTERFACE METHODS - Object 
    //////////////////////////////////////////////////
    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch (CloneNotSupportedException ex)
        {
            log.severe("Fatal error - TermChar clone() failed");
            throw new RuntimeException(ex);
        }
    }

    //////////////////////////////////////////////////
    // INTERFACE METHODS - TermChar
    //////////////////////////////////////////////////
    /**
     * Clears the this location in the character buffer ! Important Note: be very careful of using clear. It should typically only be used
     * when starting new fields, clearing display memory, or creating new char objects initially
     *
     */
    public void clear()
    {
        //System.out.println("** clear: " + position);
        character = 0;
        // Initially, we are our own video attribute character, which may later change.
        vaChar = this;
        field = null;

        vidAttrMap.clear();;
        fldAttrMap.clear();;
        // unless overridden, is the default for most devices
        setFieldAttribute(FieldAttribute.PROTECTED, dfltProtected);
    }

    /**
     * Returns the contents of hidden fields.
     *
     * <p>
     * <b>NOTE:</b> Do not use for display of characters, only for testing contents. For displaying the character, use
     * <code>getDisplayChar()</code>
     * </p>
     *
     * @return the character represented by this object.
     */
    public char getChar()
    {
        return character;
    }

    /**
     * DOCUMENT ME!
     *
     * @return the field in which this character is contained
     */
    public TermField getField()
    {
        return field;
    }

    /**
     * DOCUMENT ME!
     *
     * @return the position of this character relative to the data buffer (screen)
     */
    public int getPositionBA()
    {
        return position;
    }

    //TODO nasty, but bulk moves can change this. Might look at better ways to handle
    public void setPositionBA(int newPos)
    {
        position = newPos;
    }

    /**
     * DOCUMENT ME!
     *
     * @return True if this field should be rendered in alternate intensity e.g bold or dim
     */
    public boolean isAltIntensity()
    {
        return testBooleanAttribute(vaChar.getVideoAttribute(VideoAttribute.ALT_INTENSITY));
    }

    /**
     * DOCUMENT ME!
     *
     * @return True if this field should be hidden
     */
    public boolean isHidden()
    {
        return testBooleanAttribute(vaChar.getVideoAttribute(VideoAttribute.HIDDEN));
    }

    /**
     * @return the isReverse
     */
    public boolean isReverse()
    {
        return testBooleanAttribute(vaChar.getVideoAttribute(VideoAttribute.REVERSE));
    }

    /**
     * @return the isUnderscore
     */
    public boolean isUnderscore()
    {
        return testBooleanAttribute(vaChar.getVideoAttribute(VideoAttribute.UNDERSCORE));
    }

    public boolean isBlink()
    {
        return testBooleanAttribute(vaChar.getVideoAttribute(VideoAttribute.BLINK));
    }

    public boolean isModified()
    {
        return field != null ? field.isModified() : testBooleanAttribute(getFieldAttribute(FieldAttribute.MODIFIED));
    }

    // Numeric needs better handling really
    public boolean isNumeric()
    {
        return field != null ? field.isNumeric() : testBooleanAttribute(getFieldAttribute(FieldAttribute.NUMERIC));
    }

    /**
     * DOCUMENT ME!
     *
     * @return True if this character/field is protected. (Accessed through Term3270Field)
     */
    public boolean isProtected()
    {
        return field != null ? field.isProtected() : testBooleanAttribute(getFieldAttribute(FieldAttribute.PROTECTED));
    }

    /**
     * @return the isAutoTab
     */
    public boolean isAutoTab()
    {
        return field != null ? field.isAutoTab() : testBooleanAttribute(getFieldAttribute(FieldAttribute.AUTO_TAB));
    }

    /**
     * DOCUMENT ME!
     *
     * @return True if this character marks the beginning of a 3270 Field.
     */
    public boolean isStartField()
    {
        return testBooleanAttribute(getFieldAttribute(FieldAttribute.START_FIELD));
    }

    /**
     * Sets the actual ASCII char stored in this object.
     *
     * @param c character stored in this buffer location
     */
    public void setChar(char c)
    {
        character = c;
    }

    /**
     * Creates a pointer to the Term3270Field object that 'contains' this character.
     *
     * @param field the field in which this character is contained
     */
    public void setField(TermField field)
    {
        this.field = field;
    }

    /**
     * Sets this position in the data buffer as a start field
     */
    public void setStartField()
    {
        setFieldAttribute(FieldAttribute.START_FIELD, true);
    }

    @Override
    public Color getBgColor(Color dfltBg)
    {
        // check for specific color
        Object bgcolor = vaChar.getVideoAttribute(VideoAttribute.BG_COLOR);

        if (bgcolor == null || !(bgcolor instanceof Color))
        {
            // no specific color so look for attribute mapped color
            bgcolor = vaChar.getMappedBgColor(bgcolor);
        }

        return bgcolor != null && bgcolor instanceof Color ? (Color) bgcolor : dfltBg;
    }

    @Override
    public Color getFgColor(Color dfltFg)
    {
        // check for specific color
        Object fgcolor = vaChar.getVideoAttribute(VideoAttribute.FG_COLOR);

        if (fgcolor == null || !(fgcolor instanceof Color))
        {
            // no specific color so look for attribute mapped color
            fgcolor = vaChar.getMappedFgColor(fgcolor);
        }

        return fgcolor != null && fgcolor instanceof Color ? (Color) fgcolor : dfltFg;
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
}
