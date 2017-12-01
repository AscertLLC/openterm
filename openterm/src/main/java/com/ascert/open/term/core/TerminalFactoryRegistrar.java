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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.ascert.open.term.i3270.Term3270Factory;

/**
 *
 * @version 1,0 02-Jun-2017
 * @author srm
 * @history 02-Jun-2017 srm Created
 */
public class TerminalFactoryRegistrar
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    private static List<TerminalFactory> factories = Collections.synchronizedList(new ArrayList<>());

    public static void initTermTypeFactories(String factoriesStr)
        throws Exception
    {
        // Always support our own 3270 terminal factory
        registerTerminalFactory(new Term3270Factory());

        if (factoriesStr != null)
        {
            // Scan for and add in any custom factories
            for (String factoryStr : factoriesStr.split(";"))
            {
                registerTerminalFactory(factoryStr);
            }
        }
    }

    public static void registerTerminalFactory(String clzFactoryName)
        throws Exception
    {
        Class clzFactory = Class.forName(clzFactoryName);
        TerminalFactory factory = (TerminalFactory) clzFactory.newInstance();
        registerTerminalFactory(factory);
    }

    public static void registerTerminalFactory(TerminalFactory factory)
    {
        factory.init();
        factories.add(factory);
    }

    public static Collection<TerminalFactory> getTerminalFactories()
    {
        return new ArrayList(factories);
    }

    public static Terminal createTerminal(String termType)
        throws Exception
    {
        for (TerminalFactory factory : factories)
        {
            if (factory.isSupported(termType))
            {
                return factory.getTerminal(termType);
            }
        }

        throw new Exception("Unable to initialise terminal, unsupported terminal type: " + termType);
    }

    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////
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
