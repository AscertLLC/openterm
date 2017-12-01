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
 * The operator information area of a host session. This area is used to provide status information regarding the state of the host session
 * and location of the cursor. An OhioOIA object can be obtained using the GetOIA() method on an instance of OhioScreen.
 */
public interface OhioOIA
{

    // Indicates whether the field which contains the cursor is an
    // alphanumeric field. True if the cursor is in an
    // alphanumeric field, false otherwise.
    boolean isAlphanumeric();

    // The communication check code. If InputInhibited returns
    // OHIO_INPUTINHIBITED_COMMCHECK, this property will return the
    // communication check code.
    int getCommCheckCode();

    // Indicates whether or not input is inhibited. If input is
    // inhibited, SendKeys or SendAID calls to the OhioScreen are
    // not allowed. Why input is inhibited can be determined from
    // the value returned. If input is inhibited for more than one
    // reason, the highest value is returned.
    Ohio.OHIO_INPUTINHIBITED getInputInhibited();

    // The machine check code. If InputInhibited returns
    // OHIO_INPUTINHIBITED_MACHINECHECK, this property will return
    // the machine check code.    
    int getMachineCheckCode();

    // Indicates whether the field which contains the cursor is a
    // numeric-only field. True if the cursor is in a numeric-only
    // field, false otherwise
    boolean isNumeric();

    // Indicates the owner of the host connection.
    Ohio.OHIO_OWNER getOwner();

    // The program check code. If InputInhibited returns
    // OHIO_INPUTINHIBITED_PROGCHECK, this property will return the
    // program check code.
    int getProgCheckCode();

    //TODO - extensions in OS_OHIO, need to review
    void addOIAListener(OhioOIAListener listener);

    void removeOIAListener(OhioOIAListener listener);
}
