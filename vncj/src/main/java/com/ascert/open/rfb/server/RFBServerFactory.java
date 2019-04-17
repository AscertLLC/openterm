/*
 * Copyright (c) 2018 Ascert, LLC.
 * www.ascert.com

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
package com.ascert.open.rfb.server;

import gnu.rfb.server.RFBAuthenticator;
import gnu.rfb.server.RFBServer;

import java.lang.reflect.InvocationTargetException;

/**
 *
 * @version 1,0 31-Jul-2018
 * @author srm

 *      31-Jul-2018    srm        Created
 */
public interface RFBServerFactory
{
    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////

    RFBServer getInstance(boolean newClientConnection) throws InstantiationException, IllegalAccessException, IllegalArgumentException,
                                                              InvocationTargetException;

    boolean isShareable();

    /**
     * @return the display
     */
    int getDisplay();

    /**
     * @return the displayName
     */
    String getDisplayName();

    /**
     * @return the authenticator
     */
    RFBAuthenticator getAuthenticator();


}
