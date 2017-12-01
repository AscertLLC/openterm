/*
 * Java interfaces for Open Host Interface Objects (OHIO)
 *      https://tools.ietf.org/html/draft-ietf-tn3270e-ohio-01
 *
 * Copyright (C) 2016, Ascert LLC
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
package com.ascert.open.ohio;

/**
 * A field is the fundamental element of a virtual screen. A field includes both data and attributes describing the field. The OhioField
 * class encapsulates a virtual screen field and provides methods for accessing and manipulating field attributes and data. OhioField
 * objects can be accessed only through the OhioFields
 *
 * object.
 */
public interface OhioField
{

    // The starting position of the field. The position can range
    // from 1 to the size of the virtual screen. The starting
    // position of a field is the position of the first character
    // in the field.
    OhioPosition getStart();

    // The ending position of the field. The position can range
    // from 1 to the size of the virtual screen. The ending
    // position of a field is the position of the last character in
    // the field.
    OhioPosition getEnd();

    // The length of the field. A field�s length can range
    // from 1 to the size of thevirtual screen.
    int getLength();

    // The attribute byte for the field.
    int getAttribute();

    boolean isModified();

    boolean isProtected();

    boolean isNumeric();

    boolean isHighIntensity();

    boolean isPenSelectable();

    boolean isHidden();

    // The text plane data for the field. This is similar to the
    // getData() method using the OHIO_PLANE_TEXT parameter, except
    // the data is returned as a string instead of a character
    // array. When setting the String property, if the string is
    // shorter than the length of the field, the rest of the field
    // is cleared. If the string is longer than the field, the
    // text is truncated. A subsequent call to this property will
    // not reflect the changed text. To see the changed text, do a
    // refresh on the OhioFields collection and retrieve a new
    // OhioField object.   
    String getString();

    void setString(String text);

    // Returns data from the different planes (text, color,
    // extended) associated with the field. The data is returned
    // as a character array.
    // targetPlane An OHIO_PLANE value indicating from which
    // plane to retrieve the data.
    //TODO - Need to take care of planes that return attribute enums??
    char[] getData(Ohio.OHIO_PLANE targetPlane);

}
