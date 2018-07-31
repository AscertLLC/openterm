/*
 * Copyright (c) 2000-2018 Ascert, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Ascert, LLC. ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with the terms
 * of the license agreement you entered into with Ascert.
 *
 * ASCERT MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
 * OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. ASCERT SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */
package gnu.rfb.server;

import java.lang.reflect.InvocationTargetException;

/**
 *
 * @version 1,0 31-Jul-2018
 * @author srm
 * @history
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
