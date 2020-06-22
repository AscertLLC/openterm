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
 *
 * The Author can be contacted at agillesp@i-no.com or
 * 185 Captain Whitney Road (Becket)
 * Chester, MA  01011
 */
package com.ascert.open.term.i3270;

import java.awt.Color;
import java.io.IOException;

import com.ascert.open.term.core.AbstractTermField;

/**
 * Represents a Tn3270 'Field' instance.
 *
 * You can obtain an enumeration of Term3270Field objects by calling Term3270.getFields(). You can also get the 'current' field. (The field
 * that the current cursor position is located in) by calling Term3270.getField(). Finally, you can get the Term3270Field object at any
 * given cursor position by calling <code>Term3270.getField(int
 * cursorPosition)</code>.
 *
 * <p>
 * This class is useful for working with 3270 fields. For example, if you wanted to enter data from a GUI textField: <code></code>
 *
 * @since 0.1
 */
public class Term3270Field extends AbstractTermField
{

    /**
     * Not seen by end-programmers.
     *
     * The data stream handles the creation and destruction of field objects.
     */
    public Term3270Field(Term3270Char fa, Term3270 rw)
    {
        super(fa, rw);
    }

    public boolean isValidInput(char ch)
    {
        if (this.isNumeric())
        {
            // The old 3270 programmers reference description of a numeric field:
            // Fields defined as numeric will accept all uppercase
            // symbols and numerics from a data entry-type keyboard.
            return Character.isDigit(ch) || ch == '.' || ch == '-' ||
                   Character.isUpperCase(ch) || ch == ' ';
        }

        return true;
    }

    /**
     * @return the highlighting scheme for this field. (Corresponding to the highlighting constants defined in the Term3270Char class)
     */
    public int getHighlighting()
    {
        return ((Term3270Char) faChar).getHighlighting();
    }

    public int getFieldAttribute()
    {
        return ((Term3270Char) faChar).getFieldAttribute();
    }

    /**
     * Returns a string representation of the field.
     *
     * @since 0.2
     */
    public String toString()
    {
        StringBuffer rep = new StringBuffer("(field");
        rep.append(":begin ").append(getBeginBA()).append("\n");
        rep.append(":highlight ").append(getHighlighting()).append("\n");
        rep.append(":hidden-p ").append(isHidden()).append("\n");
        rep.append(":alt-intensity-p ").append(isAltIntensity()).append("\n");
        rep.append(":protected-p ").append(isProtected()).append("\n");
        rep.append(":numeric-p ").append(isNumeric());
        // autoskip, nondisplay, intensified display are missing
        // detectable as well
        rep.append(")");
        return rep.toString();
    }

}
