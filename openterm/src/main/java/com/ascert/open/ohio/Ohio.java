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

import java.util.EnumSet;

/**
 * Base interface for OHIO classes.
 *
 * Much of the terminology and types in OHIO are very 3270 specific, but can be leveraged and applied for other device types with a form
 * mode ot block mode capability e.g. screen interactions, field attributes etc. This class is a starting point to move towards allowing
 * other device types to be suppored through the OHIO. In the present form it is very much a quick derivation of the org.ohio classes, with
 * many parts still 3270 specific and abstractions such as the OHIO_AID enums which really need more thought.
 *
 * OHIO really started life as a C/C++ API abstraction for 3270 terminals. At some stage a set of roughly euiqvalent Java API classes
 * (org.ohio.*) were created although these do not seem to have become widely used or maintained. The original sources can be found in
 * various places e.g. https://sourceforge.net/projects/os-ohio
 * 
 * There are terminal specifics in here that ideally would be abstracted out into separate classes and/or packages e.g. 3270 and 6530 
 * F-key codes. It's also possible that OHIO itself is largely redundant these days, and we should just move to using direct open.term
 * interfaces.
 *
 */
public interface Ohio
{

    public enum OHIO_DIRECTION
    {
        OHIO_DIRECTION_FORWARD, // Forward (beginning towards end)
        OHIO_DIRECTION_BACKWARD     // Backward (end towards beginning)
    };

    public enum OHIO_TYPE
    {
        OHIO_TYPE_UNKNOWN, // Unknown host
        OHIO_TYPE_3270, // IBM 3270 host
        OHIO_TYPE_5250, // IBM 5250 host
        OHIO_TYPE_6530               // NSK 6530 host
    };

    public enum OHIO_STATE
    {
        OHIO_STATE_DISCONNECTED, // The communication link to the host is disconnected
        OHIO_STATE_CONNECTED        // The communication link to the host is connected.    
    };

    public enum OHIO_PLANE
    {
        OHIO_PLANE_TEXT, // Indicates Text Plane (character data)
        OHIO_PLANE_COLOR, // Indicates Color Plane (standard HLLAPI CGA color values)
        OHIO_PLANE_FIELD, // Indicates Field Attribute Plane (field attribute bytes)
        OHIO_PLANE_EXTENDED         // Indicates Extended Plane (extended attribute bytes)
    };

    // Colors need a mapped numeric value for use when decoding binary
    // data used in the Ohio Color Plane.
    // Note: literal values used are taken from OS-OHI project - unclear at thus stage
    // if those were in turn taken from HLLAPI, or derived elsewhere
    public enum OHIO_COLOR
    {
        OHIO_COLOR_BLACK(0),
        OHIO_COLOR_BLUE(1),
        OHIO_COLOR_GREEN(2),
        OHIO_COLOR_CYAN(3),
        OHIO_COLOR_RED(4),
        OHIO_COLOR_MAGENTA(5),
        OHIO_COLOR_WHITE(7),
        OHIO_COLOR_YELLOW(14);

        //TODO - need to review whether this should be more correctly a byte or char
        private final int colorValue;

        OHIO_COLOR(int colorValue)
        {
            this.colorValue = colorValue;
        }

        public int getColorValue()
        {
            return colorValue;
        }

        //TODO: not sure if we will need this 
        //public boolean equals(Object obj)
        //{
        //}
    };

    // These values are returned in the Ohio Extended Field Plane. As with OHIO_COLOR,
    // they need numeric values for use when decoding the returned data.
    //TODO - seems to be a mix of masks and values. Need to revisit this!
    public enum OHIO_EXTENDED
    {
        OHIO_EXTENDED_HILITE(0xC0), // Bitmask for Highlighting Bits
        OHIO_EXTENDED_COLOR(0x38), // Bitmask for Color Bits
        OHIO_EXTENDED_RESERVED(0x07), // Bitmask for Reserved Bits
        OHIO_EXTENDED_HILITE_NORMAL(0), // Normal highlighting
        OHIO_EXTENDED_HILITE_BLINK(1), // Blinking highlighting
        OHIO_EXTENDED_HILITE_REVERSEVIDEO(2), // Reverse Video highlighting
        OHIO_EXTENDED_HILITE_UNDERSCORE(3), // Underscore highlighting
        //TODO - feels like these should be shifted??
        OHIO_EXTENDED_COLOR_DEFAULT(0),
        OHIO_EXTENDED_COLOR_BLUE(1),
        OHIO_EXTENDED_COLOR_RED(2),
        OHIO_EXTENDED_COLOR_PINK(3),
        OHIO_EXTENDED_COLOR_GREEN(4),
        OHIO_EXTENDED_COLOR_TURQUOISE(5),
        OHIO_EXTENDED_COLOR_YELLOW(6),
        OHIO_EXTENDED_COLOR_WHITE(7);
        //TODO - non standard, left in but commented out for now until 
        //       we have real 5250 emulation to work from. Also unclear
        //       what correct mechanism is for non-standard extensions.
        //OHIO_EXTENDED_5250_REVERSE(0x80),
        //OHIO_EXTENDED_5250_UNDERLINE(0x40),
        //OHIO_EXTENDED_5250_BLINK(0x20),
        //OHIO_EXTENDED_5250_COL_SEP(0x10)

        //TODO - need to review whether this should be more correctly a byte or char
        private final int extAttrVal;

        OHIO_EXTENDED(int extAttrVal)
        {
            this.extAttrVal = extAttrVal;
        }

        public int getExtAttrValue()
        {
            return extAttrVal;
        }

    };

    // These values are returned in the Ohio Field Attributes. As with OHIO_COLOR,
    // they need numeric values for use when decoding the returned data.
    //TODO - seems to be a mix of masks and values. Need to revisit this!
    public enum OHIO_FIELD
    {
        OHIO_FIELD_ATTRIBUTE(0xC0), // Bitmask for field attribute
        OHIO_FIELD_PROTECTED(0x20), // Protected field
        OHIO_FIELD_NUMERIC(0x10), // Numeric field
        OHIO_FIELD_PEN_SELECTABLE(0x08), // Pen selectable field      
        OHIO_FIELD_HIGH_INTENSITY(0x04), // High intensity field
        OHIO_FIELD_HIDDEN(0x0C), // Hidden field
        OHIO_FIELD_RESERVED(0x02), // Reserved field
        OHIO_FIELD_MODIFIED(0x01);          // Modified field

        //TODO - need to review whether this should be more correctly a byte or char
        private final int attrVal;

        OHIO_FIELD(int attrVal)
        {
            this.attrVal = attrVal;
        }

        public int getAttrValue()
        {
            return attrVal;
        }

    };

    public enum OHIO_UPDATE
    {
        OHIO_UPDATE_CLIENT, // Update initiated by client
        OHIO_UPDATE_HOST                // Update initiated by host
    };

    //TODO - no idea what these values mean!
    public enum OHIO_OWNER
    {
        OHIO_OWNER_UNKNOWN, // Uninitialized
        OHIO_OWNER_APP, // Application or 5250 host
        OHIO_OWNER_MYJOB, // 3270 - Myjob
        OHIO_OWNER_NVT, // 3270 in NVT mode
        OHIO_OWNER_UNOWNED, // 3270 - Unowned
        OHIO_OWNER_SSCP                 // 3270 - SSCP
    };

    public enum OHIO_INPUTINHIBITED
    {
        OHIO_INPUTINHIBITED_NOTINHIBITED, // Input not inhibited
        OHIO_INPUTINHIBITED_SYSTEM_WAIT, // Input inhibited by a System Wait state ("X SYSTEM" or "X []")
        OHIO_INPUTINHIBITED_COMMCHECK, // Input inhibited by a communications check state ("X COMMxxx")
        OHIO_INPUTINHIBITED_PROGCHECK, // Input inhibited by a program check state ("X PROGxxx")
        OHIO_INPUTINHIBITED_MACHINECHECK, // Input inhibited by a machine check state ("X MACHxxx")
        OHIO_INPUTINHIBITED_OTHER           // Input inhibited by something other than above states
    };

    //OHIO_AID - not in original IETF spec, but necessary
    //           http://www.ingenuityworking.com/knowledge/w/knowledgebase/ibm-3270-emulation-data-flow.aspx
    // DO NOT CHANGE THE MNEMONICS - other areas of code may rely on them for lookup and/or translation
    public enum OHIO_AID
    {
        OHIO_AID_3270_SYSREQ(0xF0, "sysreq"),
        OHIO_AID_3270_PF1(0xF1, "pf1"),
        OHIO_AID_3270_PF2(0xF2, "pf2"),
        OHIO_AID_3270_PF3(0xF3, "pf3"),
        OHIO_AID_3270_PF4(0xF4, "pf4"),
        OHIO_AID_3270_PF5(0xF5, "pf5"),
        OHIO_AID_3270_PF6(0xF6, "pf6"),
        OHIO_AID_3270_PF7(0xF7, "pf7"),
        OHIO_AID_3270_PF8(0xF8, "pf8"),
        OHIO_AID_3270_PF9(0xF9, "pf9"),
        OHIO_AID_3270_PF10(0x7A, "pf10"),
        OHIO_AID_3270_PF11(0x7B, "pf11"),
        OHIO_AID_3270_PF12(0x7C, "pf12"),
        OHIO_AID_3270_PF13(0xC1, "pf13"),
        OHIO_AID_3270_PF14(0xC2, "pf14"),
        OHIO_AID_3270_PF15(0xC3, "pf15"),
        OHIO_AID_3270_PF16(0xC4, "pf16"),
        OHIO_AID_3270_PF17(0xC5, "pf17"),
        OHIO_AID_3270_PF18(0xC6, "pf18"),
        OHIO_AID_3270_PF19(0xC7, "pf19"),
        OHIO_AID_3270_PF20(0xC8, "pf20"),
        OHIO_AID_3270_PF21(0xC9, "pf21"),
        OHIO_AID_3270_PF22(0x4A, "pf22"),
        OHIO_AID_3270_PF23(0x4B, "pf23"),
        OHIO_AID_3270_PF24(0x4C, "pf24"),
        OHIO_AID_3270_PA1(0x6C, "pa1"),
        OHIO_AID_3270_PA2(0x6E, "pa2"),
        OHIO_AID_3270_PA3(0x6B, "pa3"),
        OHIO_AID_3270_CLEAR(0x6D, "clear"),
        OHIO_AID_3270_CLEAR_PARTITION(0x6A, "clrptn"),
        OHIO_AID_3270_ENTER(0x7D, "enter"),
        // TODO - No inclusion yet of AIDs for IBM 5250.

        OHIO_FKEY_6530_F1(0x40, "F1"),
        OHIO_FKEY_6530_F2(0x41, "F2"),
        OHIO_FKEY_6530_F3(0x42, "F3"),
        OHIO_FKEY_6530_F4(0x43, "F4"),
        OHIO_FKEY_6530_F5(0x44, "F5"),
        OHIO_FKEY_6530_F6(0x45, "F6"),
        OHIO_FKEY_6530_F7(0x46, "F7"),
        OHIO_FKEY_6530_F8(0x47, "F8"),
        OHIO_FKEY_6530_F9(0x48, "F9"),
        OHIO_FKEY_6530_F10(0x49, "F10"),
        OHIO_FKEY_6530_F11(0x4a, "F11"),
        OHIO_FKEY_6530_F12(0x4b, "F12"),
        OHIO_FKEY_6530_F13(0x4c, "F13"),
        OHIO_FKEY_6530_F14(0x4d, "F14"),
        OHIO_FKEY_6530_F15(0x4e, "F15"),
        OHIO_FKEY_6530_F16(0x4f, "F16"),
        OHIO_FKEY_6530_UP(0x50, "RollUp"),
        OHIO_FKEY_6530_DOWN(0x51, "RollDown"),
        OHIO_FKEY_6530_PGDOWN(0x52, "NextPage"),
        OHIO_FKEY_6530_PGUP(0x53, "PrevPage"),
        OHIO_FKEY_6530_INS(0x54, "InsDel"),
        OHIO_FKEY_6530_ENTER(0x56, "Return"),
        OHIO_FKEY_6530_EOF(0x57, "CtlY"),
        OHIO_FKEY_6530_SF1(0x60, "SF1"),
        OHIO_FKEY_6530_SF2(0x61, "SF2"),
        OHIO_FKEY_6530_SF3(0x62, "SF3"),
        OHIO_FKEY_6530_SF4(0x63, "SF4"),
        OHIO_FKEY_6530_SF5(0x64, "SF5"),
        OHIO_FKEY_6530_SF6(0x65, "SF6"),
        OHIO_FKEY_6530_SF7(0x66, "SF7"),
        OHIO_FKEY_6530_SF8(0x67, "SF8"),
        OHIO_FKEY_6530_SF9(0x68, "SF9"),
        OHIO_FKEY_6530_SF10(0x69, "SF10"),
        OHIO_FKEY_6530_SF11(0x6A, "SF11"),
        OHIO_FKEY_6530_SF12(0x6B, "SF12"),
        OHIO_FKEY_6530_SF13(0x6C, "SF13"),
        OHIO_FKEY_6530_SF14(0x6D, "SF14"),
        OHIO_FKEY_6530_SF15(0x6E, "SF15"),
        OHIO_FKEY_6530_SF16(0x6F, "SF16"),
        OHIO_FKEY_6530_Sh_UP(0x70, "ShiftRollUp"),
        OHIO_FKEY_6530_Sh_DOWN(0x71, "ShiftRollDown"),
        OHIO_FKEY_6530_Alt_PGDOWN(0x72, "ShiftNextPage"),
        OHIO_FKEY_6530_Alt_PGUP(0x73, "ShiftPrevPage"),
        OHIO_FKEY_6530_DEL(0x74, "ShiftInsDel"),
        OHIO_FKEY_6530_Sh_ENTER(0x76, "ShiftReturn");

        private final int aidValue;
        private final String aidMnemonic;

        OHIO_AID(int aidValue, String aidMnemonic)
        {
            this.aidValue = aidValue;
            this.aidMnemonic = aidMnemonic;
        }

        public int getAidValue()
        {
            return aidValue;
        }

        public String getAidMnemonic()
        {
            return aidMnemonic;
        }

        public static EnumSet<OHIO_AID> getAidSet(OHIO_TYPE typ)
        {
            switch (typ)
            {
                case OHIO_TYPE_3270:
                    return EnumSet.range(OHIO_AID_3270_SYSREQ, OHIO_AID_3270_ENTER);
                case OHIO_TYPE_6530:
                    return EnumSet.range(OHIO_FKEY_6530_F1, OHIO_FKEY_6530_Sh_ENTER);
            }

            return EnumSet.noneOf(OHIO_AID.class);
        }

        public static OHIO_AID getOhioAid(int aidValue, OHIO_TYPE typ)
        {
            EnumSet<OHIO_AID> aidSet = getAidSet(typ);

            for (Ohio.OHIO_AID aid : aidSet)
            {
                if (aid.getAidValue() == aidValue)
                {
                    return aid;
                }
            }

            return null;
        }
    }

    /**
     * Returns the OHIO version level of this implementation.
     *
     * @return The OHIO version level.
     *
     */
    default String getOhioVersion()
    {
        return "OHIO 1.10";
    }

    /**
     * Returns the name of the vendor providing this OHIO implementation.
     *
     * @return The OHIO vender name
     */
    default String getVendorName()
    {
        return "Ascert LLC";
    }

    /**
     * Returns the vendor product version that is providing the OHIO implementation.
     *
     * @return The vender procuct version
     */
    default String getVendorProductVersion()
    {
        return "com.ascert.open.ohio 1.10";
    }

    /**
     * The vendor object that provides non-standard, vendor specific extensions to the OHIO object.
     *
     * @return the vendor specific implementation of this object.
     */
    default Object getVendorObject()
    {
        return null;
    }

    // Create an OhioPosition object.
    //       row     The row coordinate.
    //       col     The column coordinate.
    default OhioPosition CreateOhioPosition(int row, int col)
    {
        return new OhioPosition(row, col);
    }
}
