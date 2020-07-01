/*
 * Copyright (c) 2016, 2017 Ascert, LLC.
 * www.ascert.com
 *
 * Based on original code from FreeHost3270, copyright for derivations from original works remain:
 *  Copyright (C) 1998, 2001  Art Gillespie
 *  Copyright (2) 2005 the http://FreeHost3270.Sourceforge.net
 *
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
package com.ascert.open.term.i3270;

import java.awt.Color;
import java.util.EnumSet;

import com.ascert.open.term.core.AbstractTermChar;

/**
 * Represents a single 'character' in the client's data buffer/screen.
 *
 * <p>
 * Field attributes (Highlighting, Color, etc.) should be manipulated using the Term3270Field object, which can obtained from any
 * Term3270Char object by calling the <code>getField()</code> method. An array of Term3270Char objects representing the current 3270 screen
 * can be obtained by calling the
 * <code>RW3270.getDataBuffer()</code> method.
 * </p>
 *
 * @see com.ascert.open.term.i3270.Term3270Field
 * @see net.sf.freehost3270.client.RW3270
 */
//TODO - actually, I suspect we should have very little extension going on here.
//       A better model would be composition of device specifics into common class e.g.
//       inclusion of xxxAttributes and xxxKeyMap and xxxColorMap specific handling classes.
public class Term3270Char extends AbstractTermChar
{

    //Highlighting constants p 4.4.6.3
    public static final short HL_DEFAULT = 0x00;
    public static final short HL_NORMAL = 0xF0;
    public static final short HL_BLINK = 0xF1;
    public static final short HL_REVERSE = 0xF2;
    public static final short HL_UNDERSCORE = 0xF4;
    public static final short HL_INTENSIFY = 0xF8;

    //color constants p 4.4.6.4
    public static final short BGCOLOR_DEFAULT = 0xF0;
    public static final short FGCOLOR_DEFAULT = 0xF7;
    public static final short BLUE = 0xF1;
    public static final short RED = 0xF2;
    public static final short PINK = 0xF3;
    public static final short GREEN = 0xF4;
    public static final short TURQUOISE = 0xF5;
    public static final short YELLOW = 0xF6;
    public static final short BLACK = 0xF8;
    public static final short DEEP_BLUE = 0xF9;
    public static final short ORANGE = 0xFA;
    public static final short PURPLE = 0xFB;
    public static final short PALE_GREEN = 0xFC;
    public static final short PALE_TURQUOISE = 0xFD;
    public static final short GREY = 0xFE;
    public static final short WHITE = 0xFF;

    //4.4.6.6 Field Outlining (Internal use)
    public static final short OL_NONE = 0;
    public static final short OL_UNDER = 1;
    public static final short OL_RIGHT = 2;
    public static final short OL_OVER = 3;
    public static final short OL_LEFT = 4;
    public static final short OL_UNDER_RIGHT = 5;
    public static final short OL_UNDER_OVER = 6;
    public static final short OL_UNDER_LEFT = 7;
    public static final short OL_RIGHT_OVER = 8;
    public static final short OL_RIGHT_LEFT = 9;
    public static final short OL_OVER_LEFT = 10;
    public static final short OL_OVER_RIGHT_UNDER = 11;
    public static final short OL_UNDER_RIGHT_LEFT = 12;
    public static final short OL_OVER_LEFT_UNDER = 13;
    public static final short OL_OVER_RIGHT_LEFT = 14;
    public static final short OL_RECTANGLE = 15;

    private short attribute;
    private short background;
    private short foreground;
    private short highlighting;
    private short outlining;

    /**
     * Instanitates a new RW3270 character.
     *
     * <p>
     * Normally is called by RW3270. End-programmers have no reason to create RW3270Objects, as they are managed completely by the data
     * stream and session instantiation.
     * </p>
     *
     * @param position The position of this character in the buffer
     */
    protected Term3270Char(int position)
    {
        super(position);
        clear();
    }

    public void clear()
    {
        super.clear();
        highlighting = HL_NORMAL;
        background = BGCOLOR_DEFAULT;
        foreground = FGCOLOR_DEFAULT;
        attribute = 0;
        outlining = 0;
    }

    /**
     * DOCUMENT ME!
     *
     * @return True if this field should be rendered in Bold type
     */
    public boolean isHigh()
    {
        return isAltIntensity();
    }

    /**
     * Returns the background color of this character/field.
     *
     * @return DOCUMENT ME!
     */
    public int getBackground()
    {
        return background;
    }

    /**
     * DOCUMENT ME!
     *
     * @return the byte representation of this Field Attribute (assuming it's a StartField.
     */
    public short getFieldAttribute()
    {

        // We no longer track modified in the attribute itself, and so have to overlay it
        if (isModified())
        {
            return attribute |= 1;
        }

        return attribute ^= 1;
    }

    /**
     * Returns the foreground (font) color of this character/field
     *
     * @return DOCUMENT ME!
     */
    public int getForeground()
    {
        return foreground;
    }

    /**
     * DOCUMENT ME!
     *
     * @return int corresponding to Outlining constants outlined below.
     */
    public int getOutlining()
    {
        return outlining;
    }

    /**
     * Returns the highlighting attributes for this character/startField (assuming it's a StartField.)
     *
     * @return DOCUMENT ME!
     */
    protected short getHighlighting()
    {
        return highlighting;
    }

    /**
     * Sets the background color of this character field.
     *
     * @param in DOCUMENT ME!
     */
    protected void setBackground(short in)
    {
        setVideoAttribute(VideoAttribute.BG_COLOR, translateColor(in));
        // Preserve the actual color attribute supplied in case we need it later
        background = in;
    }

    /**
     * Stores the short that represents the FA
     *
     * @param in DOCUMENT ME!
     */
    protected void setFieldAttribute(short in)
    {
        //System.out.println("FA - " + in + ", at: " + position);
        attribute = in;

        setFieldAttribute(FieldAttribute.PROTECTED, (attribute & 0x20) != 0);
        setFieldAttribute(FieldAttribute.NUMERIC, (attribute & 0x10) != 0);
        setFieldAttribute(FieldAttribute.MODIFIED, (attribute & 0x01) != 0);

        // Clean any existing video attributes
        setVideoAttribute(VideoAttribute.ALT_INTENSITY, false);
        setVideoAttribute(VideoAttribute.HIDDEN, false);

        // bits 4 & 5
        switch (in & 0x0C)
        {
            case 0x00:  // Normal, Non-detectable 
                break;
            case 0x04:  // Normal, Detectable 
                break;
            case 0x08:  // High Intensity, detectable
                setVideoAttribute(VideoAttribute.ALT_INTENSITY, true);
                break;
            case 0x0C:  // Non-Display, Non-Detect 
                setVideoAttribute(VideoAttribute.HIDDEN, true);
                break;
        }
    }

    /**
     * Sets the foreground (font) color for this character/field
     *
     * @param in DOCUMENT ME!
     */
    protected void setForeground(short in)
    {
        setVideoAttribute(VideoAttribute.FG_COLOR, translateColor(in));
        // Preserve the actual color attribute supplied in case we need it later
        foreground = in;
    }

    /**
     * Sets the highlighting attributes for this character/startField (assuming it's a StartField.)
     *
     * @param in DOCUMENT ME!
     */
    protected void setHighlighting(short in)
    {
        //System.out.println("HL - " + in + ", at: " + position);

        highlighting = in;

        if (in == HL_NORMAL || in == HL_DEFAULT)
        {
            // Need to actually record the presence of these, since they may cancel a previous attribute
            setVideoAttribute(VideoAttribute.BLINK, false);
            setVideoAttribute(VideoAttribute.REVERSE, false);
            setVideoAttribute(VideoAttribute.UNDERSCORE, false);
            setVideoAttribute(VideoAttribute.ALT_INTENSITY, false);
            return;
        }

        setVideoAttribute(VideoAttribute.BLINK, in == HL_BLINK);
        setVideoAttribute(VideoAttribute.REVERSE, in == HL_REVERSE);
        setVideoAttribute(VideoAttribute.UNDERSCORE, in == HL_UNDERSCORE);
        setVideoAttribute(VideoAttribute.ALT_INTENSITY, in == HL_INTENSIFY);
    }

    /**
     * Sets the outlining attributes for this character/startField (assuming it's a StartField.)
     *
     * @param in DOCUMENT ME!
     */
    protected void setOutlining(short in)
    {
        if (in == 0)
        {
            outlining = OL_NONE;
        }
        else if ((in & 0x01) != 0)
        { //00000001
            outlining = OL_UNDER;
        }
        else if ((in & 0x02) != 0)
        { //00000010
            outlining = OL_RIGHT;
        }
        else if ((in & 0x04) != 0)
        { //00000100
            outlining = OL_OVER;
        }
        else if ((in & 0x08) != 0)
        { //00001000
            outlining = OL_LEFT;
        }
        else if ((in & 0x03) != 0)
        { //00000011
            outlining = OL_UNDER_RIGHT;
        }
        else if ((in & 0x05) != 0)
        { //00000101
            outlining = OL_UNDER_OVER;
        }
        else if ((in & 0x09) != 0)
        { //00001001
            outlining = OL_UNDER_LEFT;
        }
        else if ((in & 0x06) != 0)
        { //00000110
            outlining = OL_RIGHT_OVER;
        }
        else if ((in & 0x0A) != 0)
        { //00001010
            outlining = OL_RIGHT_LEFT;
        }
        else if ((in & 0x0C) != 0)
        { //00001100
            outlining = OL_OVER_LEFT;
        }
        else if ((in & 0x07) != 0)
        { //00000111
            outlining = OL_OVER_RIGHT_UNDER;
        }
        else if ((in & 0x0B) != 0)
        { //00001011
            outlining = OL_UNDER_RIGHT_LEFT;
        }
        else if ((in & 0x0D) != 0)
        { //00001101
            outlining = OL_OVER_LEFT_UNDER;
        }
        else if ((in & 0x0E) != 0)
        { //00001110
            outlining = OL_OVER_RIGHT_LEFT;
        }
        else if ((in & 0x0F) != 0)
        { //00001111
            outlining = OL_RECTANGLE;
        }
    }

    /**
     * Sets the validation attributes for this character/field.
     *
     * @param in DOCUMENT ME!
     */
    protected void setValidation(short in)
    {
        //TODO:  Probably won't implement this... it's a bitch.
    }

    /**
     * This translates the int colors stored in a Field Attribute into a java Color object
     *
     * @param c DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static Color translateColor(int c)
    {
        switch (c)
        {

            case BGCOLOR_DEFAULT:
            case FGCOLOR_DEFAULT:
                // these two cases we leave null so that user defaults will apply
                return null;

            case BLUE:
                return Color.BLUE;

            case RED:
                return Color.RED;

            case PINK:
                return Color.PINK;

            case GREEN:
                return Color.GREEN;

            case TURQUOISE:
                return Color.CYAN;

            case YELLOW:
                return Color.YELLOW;

            case BLACK:
                return Color.BLACK;

            case DEEP_BLUE:
                return Color.BLUE;

            case ORANGE:
                return Color.ORANGE;

            case PURPLE:
                return Color.BLUE;

            case PALE_GREEN:
                return Color.GREEN;

            case PALE_TURQUOISE:
                return Color.CYAN;

            case GREY:
                return new Color(180, 180, 180);

            case WHITE:
                return Color.WHITE;
        }

        // Use the terminal default if the specified color isn't known
        return null;
    }

}
