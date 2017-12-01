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
 * OhioScreen encapsulates the host presentation space. The presentation space is a virtual screen which contains all the characters and
 * attributes that would be seen on a traditional emulator screen. This virtual screen is the primary object for text-based interactions
 * with the host. The OhioScreen provides methods that manipulate text, search the screen, send keystrokes to the host, and work with the
 * cursor.
 *
 * An OhioScreen object can be obtained using the GetScreen() method on an instance of OhioSession.
 *
 * The raw presentation space data is maintained in a series of planes which can be accessed by various methods within this class. The text
 * plane contains the actual characters in the presentation space. Most of the methods in OhioScreen class work exclusively with the text
 * plane.
 *
 * The remaining planes contain the corresponding attributes for each character in the text plane. The color plane contains color
 * characteristics. The field plane contains the field attributes. The extended plane contains the extended field attributes. The color,
 * field, and extended planes are not interpreted by any of the methods in this class.
 *
 */
//TODO - non standard exensions from OS_OHIO, to be reviewed
/**
 *
 * Once an instance of OhioScreen has been obtained, an application can register for OhioScreen events using the addScreenListener() method.
 * OhioScreen events are sent to registered listeners whenever the virtual screen is changed for any reason, be it host or operator
 * initiated.
 */
public interface OhioScreen
{

    // The location of the cursor in the presentation space. The
    // row and column of the cursor is contained within the
    // OhioPosition object.
    OhioPosition getCursor();

    void setCursor(OhioPosition cursorPos);

    // The OhioOIA object associated with this presentation space.
    // This object can be used to query the status of the operator
    // information area.    
    OhioOIA getOIA();

    // The OhioFields object associated with this presentation
    // space. This provides another way to access the data in the
    // virtual screen. The OhioFields object contains a snapshot
    // of all the fields in the current virtual screen. Fields
    // provide methods for interpreting the data in the non-text
    // planes. Zero length fields (due to adjacent field
    // attributes) are not returned in the OhioFields collection.
    // For unformatted screens, the returned collection contains
    // only one OhioField that contains the whole virtual screen.    
    OhioFields getFields();

    // The number of rows in the presentation space.
    int getRows();

    // The number of columns in the presentation space.
    int getColumns();

    // The entire text plane of the virtual screen as a string.
    // All null characters and Field Attribute characters are
    // returned as blank space characters.
    String getString();

    // Returns a character array containing the data from the Text,
    // Color, Field or Extended plane of the virtual screen.
    //      start   The row and column where to start. The position
    //              is inclusive (for example, row 1, col 1 means that
    //              position 1,1 will be used as the starting location
    //              and 1,1 will be included in the data). "start"
    //              must be positionally less than "end".
    //      end     The row and column where to end. The position is
//                  inclusive (for example, row 1, col 1 means that
    //              position 1,1 will be used as the ending location
    //              and 1,1 will be included in the data). "end" must
    //              be positionally greater than "start".
    //      plane   A valid OHIO_PLANE value.
    //TODO - need to take care with planes that return attribute enums??
    char[] getData(OhioPosition start, OhioPosition end, Ohio.OHIO_PLANE plane);

    // Searches the text plane for the target string. If found,
    // returns an OhioPosition object containing the target
    // location. If not found, returns a null. The targetString
    // must be completely contained by the target area for the
    // search to be successful. Null characters in the text plane
    // are treated as blank spaces during search processing.
    //      targetString    The target string.
    //      startPos        The row and column where to start. The
    //                      position is inclusive (for example, row 1,
    //                      col 1 means that position 1,1 will be used as
    //                      the starting location and 1,1 will be included
    //                      in the search).
    //      length          The length from startPos to include in the
    //                      search.
    //      dir             An OHIO_DIRECTION value.
    //      ignoreCase      Indicates whether the search is case
    //                      sensitive. True means that case will be
    //                      ignored. False means the search will be case
    //                      sensitive.
    OhioPosition findString(String targetString, OhioPosition start, int length, Ohio.OHIO_DIRECTION dir, boolean ignoreCase);

    // The sendKeys method sends a string of keys to the virtual
    // screen. This method acts as if keystrokes were being typed
    // from the keyboard.
    //
    // The keystrokes will be sent to the location given. If no
    // location is provided, the keystrokes will be sent to the
    // current cursor location.
    //      text    The string of characters to be sent.
    void sendKeys(String text, OhioPosition location);

    // The sendAid method sends an "aid" keystroke to the virtual
    // screen. These aid keys can be though of as special
    // keystrokes, like the Enter key, the Tab key, or the Page Up
    // key. All the valid special key values are contained in the
    // OHIO_AID enumeration.
    //      aidKey      The aid key to send to the virtual screen.
    void sendAid(int aidKey);

    // The setString method sends a string to the virtual screen at
    // the specified location. The string will overlay only
    // unprotected fields, and any parts of the string which fall
    // over protected fields will be discarded.
    //      text        String to place in the virtual screen.
    //      location    Position where the string should be written.
    void setString(String text, OhioPosition location);

    //TODO - non standard OS_OHIO extensions, need to review
    void addScreenListener(OhioScreenListener listener);

    void removeScreenListener(OhioScreenListener listener);

}
