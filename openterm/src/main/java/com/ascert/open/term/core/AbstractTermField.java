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
import java.io.IOException;

import static com.ascert.open.term.core.AbstractTermChar.testBooleanAttribute;

/**
 *
 * @version 1,0 03-May-2017
 * @author srm
 * @history 03-May-2017 srm Created
 */
public abstract class AbstractTermField implements TermField
{

    protected int begin;
    protected int end;
    protected Terminal term;
    // For most devices, the field attribute char will also hold the video attributes for the field
    // Some devices may allow video attribute changes mid-field however
    protected TermChar faChar;

    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    public AbstractTermField(TermChar fa, Terminal term)
    {
        //pointer to the TermChar object that contains
        //the properties for this field (Field Attribute = faChar).
        this.faChar = fa;

        //pointer to the Terminal, so we can access the data
        //buffer when necessary
        this.term = term;
        begin = fa.getPositionBA();
    }

    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // INTERFACE METHODS - TermField
    //////////////////////////////////////////////////
    public TermChar getFAChar()
    {
        return faChar;
    }

    public void setFAChar(TermChar fa)
    {
        this.faChar = fa;
    }

    /**
     * Checks the field to see if the current field is an 'input' field (unprotected)
     *
     * @return <code>true</code> if the field is protected, <code>false</code> otherwise.
     */
    public boolean isProtected()
    {
        return testBooleanAttribute(getFAChar().getFieldAttribute(TermChar.FieldAttribute.PROTECTED));
    }

    public void setProtected(boolean b)
    {
        getFAChar().setFieldAttribute(TermChar.FieldAttribute.PROTECTED, b);
    }

    public void setBeginBA(int i)
    {
        begin = i;
    }

    /**
     * Returns the data buffer address of the first character in this field.
     *
     * @return the data buffer address of the FA (first character) in this field.
     */
    public int getBeginBA()
    {
        return begin;
    }

    /**
     * Sets the data buffer address of the last character in this field.
     *
     * (Not visible to end-programmers... handled by the data stream)
     */
    public void setEndBA(int i)
    {
        end = i;
    }

    /**
     * Returns the data buffer address of the last character in this field.
     *
     * @return the data buffer address of the last character in this field
     */
    public int getEndBA()
    {
        return end;
    }

    /**
     * @return <code>true</code> if this field has been modified by the operator
     */
    public boolean isModified()
    {
        return testBooleanAttribute(getFAChar().getFieldAttribute(TermChar.FieldAttribute.MODIFIED));
    }

    /**
     * Sets the MDT for this field.
     *
     * <p>
     * If you change the data via the setData() method this method is called for you automatically. Otherwise, be sure to call this method
     * whenever you change the data in an unprotected (input) field.
     *
     * @param b <code>true</code> if the field has been modified.
     */
    public void setModified(boolean b) throws IsProtectedException
    {
        if (getFAChar().isProtected())
        {
            throw new IsProtectedException();
        }

        getFAChar().setFieldAttribute(TermChar.FieldAttribute.MODIFIED, b);
    }

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
    public TermChar[] getChars()
    {
        int adjEnd = end;

        //TODO - need to cater for terminals which do not support wrap-around. 
        //in case the field wraps around, adjust the end to be the data buffer
        //size + n
        if (end < begin)
        {
            adjEnd += term.getCharBuffer().length;
        }

        TermChar[] ret = new TermChar[(adjEnd - begin) + 1];

        //
        int c = begin;

        for (int i = 0; i < ret.length; i++, c++)
        {
            if (c == term.getCharBuffer().length)
            {
                c = 0;
            }

            ret[i] = term.getChar(c);
        }

        return ret;
    }

    /**
     * Gets an array of characters containing the display representation of this field.
     * <p>
     * Specifically, hidden and null characters are represented by an ' '
     *
     * @return array of characters.
     */
    public char[] getDisplayChars()
    {
        int adjEnd = end;
        TermChar[] display = term.getCharBuffer();

        //in case the field wraps around, adjust the end to be the data buffer
        //size + n
        if (end < begin)
        {
            adjEnd += display.length;
        }

        char[] ret = new char[(adjEnd - begin) + 1];
        int c = begin;

        for (int i = 0; i < ret.length; i++, c++)
        {
            if (c == display.length)
            {
                c = 0;
            }

            ret[i] = display[c].getDisplayChar();
        }

        return ret;
    }

    /**
     * If the field is an input field (unprotected), this method will set the field to the specified String.
     *
     * If the string is longer than the field, this method will throw an IOException.
     *
     * @param s The string to insert into this field
     * @exception IOException          thrown if the string is longer than the field
     * @exception IsProtectedException thrown if the current field is protected
     */
    public void setData(String s) throws IOException, IsProtectedException
    {
        if (isProtected())
        {
            throw new IsProtectedException();
        }

        char[] b = s.toCharArray();
        int size = (end > begin) ? (end - begin)
            : ((term.getRows() * term.getCols()) - begin + end);

        if (b.length > size)
        {
            throw new IOException();
        }

        try
        {
            setModified(true);
        }
        catch (Exception e)
        {
        }

        int offset = begin + 1;

        for (int i = 0; i < b.length; i++, offset++)
        {
            term.getChar(offset).setChar(b[i]);
        }
    }

    /**
     * This method returns the size, in characters of the current field.
     *
     * <p>
     * Be aware that this includes the Field Attribute for the field, as well as the characters
     *
     * @return The size (in characters) of this Field object
     */
    public int size()
    {
        if (end > begin)
        {
            return (end - begin) + 1;
        }

        return (term.getCharBuffer().length - begin) + end + 1;
    }

    public int dataSize()
    {
        String val = new String(getDisplayChars());
        return val.trim().length();
    }

    public boolean isAutoTab()
    {
        return testBooleanAttribute(getFAChar().getFieldAttribute(TermChar.FieldAttribute.AUTO_TAB));
    }

    /**
     * @return <code>true</code> if the field is hidden (i.e. password field), <code>dalse</code> otherwise.
     */
    public boolean isHidden()
    {
        return getFAChar().isHidden();
    }

    /**
     * @return True if the field only accepts numeric data, False if it will accept any alphanumeric input. Client implementations should
     *         test input if this method returns true and reject it if it is not numeric/
     */
    public boolean isNumeric()
    {
        return testBooleanAttribute(getFAChar().getFieldAttribute(TermChar.FieldAttribute.NUMERIC));
    }

    public boolean isReverse()
    {
        return getFAChar().isReverse();
    }

    public boolean isUnderscore()
    {
        return getFAChar().isUnderscore();
    }

    public boolean isAltIntensity()
    {
        return getFAChar().isAltIntensity();
    }

    // Color handling is a bit of a thrown in after thought at present.
    // Mapping between device-specifics, Ohio, and screen display needs better consideration really.
    public Color getFgColor(Color dfltFg)
    {
        return getFAChar().getFgColor(dfltFg);
    }

    public Color getBgColor(Color dfltBg)
    {
        return getFAChar().getBgColor(dfltBg);
    }

    // Some of this logic is rather 6530 specific!
    public void fillChars(int off, char c)
    {
        //TODO - test what happens to video attribs
        for (int ix = begin + off; ix <= end; ix++)
        {
            term.getChar(ix).setChar(c);
        }
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
