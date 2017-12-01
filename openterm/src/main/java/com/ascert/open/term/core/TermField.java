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

/**
 *
 * @version 1,0 25-Apr-2017
 * @author srm
 * @history 25-Apr-2017 srm Created
 */
public interface TermField
{

    TermChar getFAChar();

    void setFAChar(TermChar ch);

    char[] getDisplayChars();

    int size();

    int dataSize();

    /**
     * @return <code>true</code> if this field has been modified by the operator
     */
    boolean isModified();

    // Methods to allow terminal specific validation (e.g. numeric) and transformation (e.g. upshift)
    // based on field attributes
    default boolean isValidInput(char ch)
    {
        return true;
    }

    default char applyTransform(char ch)
    {
        return ch;
    }

    /**
     * @return True if the field only accepts numeric data, False if it will accept any alphanumeric input. Client implementations should
     *         test input if this method returns true and reject it if it is not numeric/
     */
    boolean isNumeric();

    /**
     * Checks the field to see if the current field is an 'input' field (unprotected)
     *
     * @return <code>true</code> if the field is protected, <code>false</code> otherwise.
     */
    boolean isProtected();

    boolean isAutoTab();

    /**
     * Sets the MDT for this field.
     *
     * <p>
     * If you change the data via the setData() method this method is called for you automatically. Otherwise, be sure to call this method
     * whenever you change the data in an unprotected (input) field.
     *
     * @param b <code>true</code> if the field has been modified.
     */
    void setModified(boolean b) throws IsProtectedException;

    void setProtected(boolean b);

    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////
    /**
     * Returns the data buffer address of the first character in this field.
     *
     * @return the data buffer address of the FA (first character) in this field.
     */
    int getBeginBA();

    /**
     * Returns the data buffer address of the last character in this field.
     *
     * @return the data buffer address of the last character in this field
     */
    int getEndBA();

    void setBeginBA(int i);

    /**
     * Sets the data buffer address of the last character in this field.
     *
     * (Not visible to end-programmers... handled by the data stream)
     */
    void setEndBA(int i);

    // Warning - at present this will be the device type specific, low level field attribute value
    int getFieldAttribute();

    // Color handling is a bit of a thrown in after thought at present.
    // Mapping between device-specifics, Ohio, and screen display needs better consideration really.
    Color getBgColor(Color dfltBg);

    Color getFgColor(Color dfltFg);

    /**
     * Gets an array of TermChar objects representing the contents of this field.
     *
     * <p>
     * This method is only useful if you need to operate on the individual Term3270Char objects. For display of data buffer (screen), use
     * <code>getDisplayChars()</code> instead.
     *
     * @return array of {@link TermChar} objects.
     * @see Term3270Char
     */
    TermChar[] getChars();

    default void fillChars(char c)
    {
        fillChars(1, c);
    }

    void fillChars(int off, char c);

}
