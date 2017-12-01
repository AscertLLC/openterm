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
 * OhioFields contains a collection of the fields in the virtual screen. It provides methods to iterate through the fields, find fields
 * based on location, and find fields containing a given string. Each element of the collection is an instance of OhioField.
 *
 * OhioFields can only be accessed through OhioScreen using the Fields property. OhioFields is a static view of the virtual screen and does
 * not reflect changes made to the virtual screen after its construction. The field list can be updated with a new view of the virtual
 * screen using the Refresh() method.
 *
 * Note: All OhioField objects returned by methods in this class are invalidated when Refresh() is called.
 */
public interface OhioFields
{

    // Returns the number of OhioField objects contained in this
    // collection.
    int getCount();

    // Returns the OhioField object at the given index. "One
    // based" indexing is used in all Ohio collections. For
    // example, the first OhioField in this collection is at
    // index 1.
    OhioField item(int fieldIndex);

    // Updates the collection of OhioField objects. All OhioField
    // objects in the current virtual screen are added to the
    // collection. Indexing of OhioField objects will not be
    // preserved across refreshes.
    void refresh();

    // Searches the collection for the target string and returns
    // the OhioField object containing that string. The string
    // must be totally contained within the field to be considered
    // a match. If the target string is not found, a null will be
    // returned.
    //      targetString    The target string.
    //      startPos        The row and column where to start. The
    //                      position is inclusive (for example, row 1,
    //                      col 1 means that position 1,1 will be used
    //                      as the starting location and 1,1 will be
    //                      included in the search).
    //      length          The length from startPos to include in the
    //                      search.
    //      dir             An OHIO_DIRECTION value.
    //      ignoreCase      Indicates whether the search is case
    //                      sensitive. True means that case will be
    //                      ignored. False means the search will be
    //                      case sensitive.    
    OhioField findByString(String targeString, OhioPosition startPos, int length, Ohio.OHIO_DIRECTION dir, boolean ignoreCase);

    // Searches the collection for the target position and returns
    // the OhioField object containing that position. If not
    // found, returns a null.
    //      targetPosition      The target row and column.    
    OhioField findByPosition(OhioPosition targetPosition);

}
