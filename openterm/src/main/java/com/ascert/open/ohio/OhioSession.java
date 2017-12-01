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
 * A host session.
 */
public interface OhioSession
{

    // The configurationResource for this OhioSession object.
    String getConfigurationResource();

    // Indicates whether this OhioSession object is connected to a
    // host. True means connected, false means not connected.
    boolean isConnected();

    // The SessionName for this OhioSession object. The
    // SessionName is unique among all instances of OhioSession.
    String getSessionName();

    // The SessionType for this OhioSession object.
    Ohio.OHIO_TYPE getSessionType();

    // The OhioScreen object for this session.
    OhioScreen getScreen();

    // Starts the communications link to the host.
    void connect();

    // Stops the communications link to the host.
    void disconnect();

    //TODO - non standard OS_OHIO extensions, need to review   
    void addSessionListener(OhioSessionListener listener);

    void removeSessionListener(OhioSessionListener listener);

}
