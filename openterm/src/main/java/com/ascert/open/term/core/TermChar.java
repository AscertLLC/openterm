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

import java.awt.Color;
import java.util.EnumSet;
import java.util.Map;

/**
 *
 * @version 1,0 25-Apr-2017
 * @author srm
 * @history 25-Apr-2017 srm Created
 */
public interface TermChar
{
    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////

    // Hidden is classied as a video attribute for device types which allow attribute changed within fields
    public static enum VideoAttribute
    {
        ALT_INTENSITY, HIDDEN, REVERSE, UNDERSCORE, BLINK, FG_COLOR, BG_COLOR
    };

    // The NUMERIC field attribute is really a marker for 3270 fields. Other terminal types may have extended
    // data entry criteria, which can be handled by the TermField.isValidInput() method.
    public static enum FieldAttribute
    {
        START_FIELD, PROTECTED, MODIFIED, AUTO_TAB, NUMERIC
    };

    public Map<VideoAttribute, Object> getVideoAttributes();

    public Map<FieldAttribute, Object> getFieldAttributes();

    boolean hasVideoAttributes();

    boolean hasFieldAttributes();

    Object getVideoAttribute(VideoAttribute vidAttr);

    Object getFieldAttribute(FieldAttribute fldAttr);

    void setVideoAttribute(VideoAttribute vidAttr, Object attrVal);

    void setFieldAttribute(FieldAttribute fldAttr, Object attrVal);

    void clearVideoAttribute(VideoAttribute vidAttr);

    void clearFieldAttribute(FieldAttribute fldAttr);

    void setVAChar(TermChar ch);

    /**
     * Clears the this location in the character buffer
     */
    void clear();

    char getChar();

    /**
     * This method returns the actual Screen representation of an object If the character is hidden or null, this method returns ' '; Use
     * this method if you plan on displaying this character to the user.
     *
     * @return the 'display' character represented by this object
     */
    default char getDisplayChar()
    {
        if (isHidden() || (getChar() < 0x20))
        {
            return ' ';
        }
        else
        {
            return getChar();
        }
    }

    TermField getField();

    /**
     * DOCUMENT ME!
     *
     * @return the position of this character relative to the data buffer (screen)
     */
    int getPositionBA();

    //TODO nasty, but bulk moves can change this. Might look at better ways to handle
    void setPositionBA(int newPos);

    /**
     * DOCUMENT ME!
     *
     * @return True if this field should be rendered in alternate intensity (dim on some device types, bold on others)
     */
    boolean isAltIntensity();

    /**
     * DOCUMENT ME!
     *
     * @return True if this field should be hidden
     */
    boolean isHidden();

    boolean isBlink();

    boolean isModified();

    boolean isNumeric();

    /**
     * DOCUMENT ME!
     *
     * @return True if this character/field is protected. (Accessed through RW3270Field)
     */
    boolean isProtected();

    /**
     * DOCUMENT ME!
     *
     * @return True if this character marks the beginning of a 3270 Field.
     */
    boolean isStartField();

    /**
     * Sets the actual ASCII char stored in this object.
     *
     * @param c character stored in this buffer location
     */
    void setChar(char c);

    /**
     * Creates a pointer to the RW3270Field object that 'contains' this character.
     *
     * @param field the field in which this character is contained
     */
    void setField(TermField field);

    /**
     * Sets this position in the data buffer as a start field
     */
    void setStartField();

    // Color handling is a bit of a thrown in after thought at present.
    // Mapping between device-specifics, Ohio, and screen display needs better consideration really.
    /**
     * Returns the screen/display background color of this character/field. This will be a mapped value from the device-specific color
     * constants
     */
    Color getBgColor(Color dfltBg);

    /**
     * Returns the screen/display forground color of this character/field. This will be a mapped value from the device-specific color
     * constants
     *
     */
    Color getFgColor(Color dfltFg);

    // Mapped colour handling is for terminals that offer mapping of monochrome attributes to color
    default Color getMappedFgColor(Object fgcolor)
    {
        return null;
    }

    default Color getMappedBgColor(Object fgcolor)
    {
        return null;
    }

    /**
     * @return the isReverse
     */
    boolean isReverse();

    /**
     * @return the isUnderscore
     */
    boolean isUnderscore();

    /**
     * @return the isAutoTab
     */
    boolean isAutoTab();

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
