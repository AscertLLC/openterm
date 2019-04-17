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

import java.io.IOException;

import com.ascert.open.ohio.Ohio;

/**
 *
 * These are the basic key action methods. All have defaults, so only those required need to be implemented
 *
 * @version 1,0 22-Jun-2017
 * @author srm

 */
public interface InputCharHandler
{
    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////

    /**
     * Inserts the specified ASCII character at the current cursor position if the current field is unprotected, and advances the cursor
     * position by one. This is useful for implementations that accept keyboard input directly. For implementations that don't require
     * character-by-character input, use RW3270Field.setData(String data) instead.
     *
     * @param key keyboard/ASCII character corresponding to the key pressed.
     *
     * @throws IsProtectedException if the current field is protected.
     *
     * @see RW3270Field
     */
    default boolean type(char key) throws IsProtectedException, IOException
    {
        return type(key, true);
    }

    boolean type(char key, boolean updateDisplay) throws IsProtectedException, IOException;

}
