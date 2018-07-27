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
package gnu.vnc;

import java.awt.image.BufferedImage;

/**
 *
 * @version 1,0 27-Jul-2018
 * @author srm
 * @history
 *      27-Jul-2018    srm        Created
 */
public interface ScreenImage
{

    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////

    public void addScreenListener(ScreenImageListener listener);

    public void removeScreenListener(ScreenImageListener listener);

    public BufferedImage getScreenBuffer();
    
    public int[] getScreenPixels();
        
}
