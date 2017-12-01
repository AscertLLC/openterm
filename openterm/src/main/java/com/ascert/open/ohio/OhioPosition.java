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
 * Holds row and column coordinates. An OhioPosition can be constructed using initial row and column coordinates or constructed with no
 * values and have the row and column set later.
 */
public class OhioPosition implements Comparable<OhioPosition>
{

    /**
     * Utility method to convert row, col to screen data array offset
     */
    public static int convertPositionToScreenOffset(int row, int col, int screenWidth)
    {
        return ((row - 1) * screenWidth) + (col - 1);
    }

    /**
     *
     * Utility method to convert position to screen data array offset
     */
    public static int convertPositionToScreenOffset(OhioPosition pos, int screenWidth)
    {
        return convertPositionToScreenOffset(pos.getRow(), pos.getColumn(), screenWidth);
    }

    /**
     * Utility method to convert screen data array offset to position
     */
    public static OhioPosition convertScreenOffsetToPosition(int offset, int screenWidth)
    {
        int row = (offset / screenWidth) + 1;
        int col = (offset % screenWidth) + 1;
        return new OhioPosition(row, col);
    }

    /**
     * holds the row
     */
    protected int row;
    /**
     * holds the column
     */
    protected int col;

    /**
     * Null constructor for OhioPosition.
     */
    public OhioPosition()
    {
        //TODO - aren't we supposed to be 1 based?? Maybe zero means not set
        this(0, 0);
    }

    /**
     * Constructor for OhioPosition.
     *
     * @param initRow The initial value for the row coordinate
     * @param initCol The initial value for the column coordinate
     */
    public OhioPosition(int initRow, int initCol)
    {
        setRow(initRow);
        setColumn(initCol);
    }

    /**
     * Returns the row coordinate.
     *
     * @return The row coordinate
     */
    public int getRow()
    {
        return row;
    }

    /**
     * Returns the column coordinate.
     *
     * @return The column coordinate
     */
    public int getColumn()
    {
        return col;
    }

    /**
     * Sets the row coordinate.
     *
     * @param newRow The new row coordinate
     */
    public void setRow(int newRow)
    {
        row = newRow;
    }

    /**
     * Sets the column coordinate.
     *
     * @param newCol The new column coordinate
     */
    public void setColumn(int newCol)
    {
        col = newCol;
    }

    //TODO - jUnit tests for these
    //////////////////////////////////////////////////
    // INTERFACE METHODS - Comparable
    //////////////////////////////////////////////////
    /**
     * Convert both positions using an arbitrary screen width, and compare as integers Note we don't call hashcode() here in case subclasses
     * alter the approach
     */
    public int compareTo(OhioPosition o)
    {

        int pThis = convertPositionToScreenOffset(this, 256);
        int pObj = convertPositionToScreenOffset(o, 256);

        return Integer.compare(pThis, pObj);
    }

    //////////////////////////////////////////////////
    // Contracts for Object
    //////////////////////////////////////////////////
    public String toString()
    {
        return new String(getRow() + "," + getColumn());
    }

    public boolean equals(Object o)
    {

        if (o instanceof OhioPosition && compareTo((OhioPosition) o) == 0)
        {
            return true;
        }

        return false;
    }

    /**
     * Use an abritrarily large screen width to compute a hashcode that will be same for all objects representing the same position. Note
     * that the value itself has no meaning and specifically does not represent a display array offset. It will be the same for positions
     * that are equal though, and also preserve numeric order comparison for those that are not.
     */
    public int hashcode()
    {
        return convertPositionToScreenOffset(this, 256);
    }

}
