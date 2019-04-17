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

import java.awt.Component;

import com.ascert.open.term.gui.TermOptions;

/**
 *
 * @version 1,0 31-May-2017
 * @author srm

 */
public interface TerminalFactory
{

    default void init()
    {
    }

    String[] getTerminalTypes();

    default TermOptions getOptionsPanel(String termType)
    {
        return null;
    }

    boolean isSupported(String termType);

    Terminal getTerminal(String termType);
}
