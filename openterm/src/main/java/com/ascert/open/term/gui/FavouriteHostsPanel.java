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
package com.ascert.open.term.gui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.ascert.open.term.core.Host;

/**
 */
public class FavouriteHostsPanel extends JPanel
{

    private static final Logger log = Logger.getLogger(FavouriteHostsPanel.class.getName());

    
    public static void resizeColumns(JTable jTable1, float[] columnWidthPercentage) 
    {
        int tW = jTable1.getColumnModel().getTotalColumnWidth();
        TableColumn column;
        TableColumnModel jTableColumnModel = jTable1.getColumnModel();
        int cantCols = jTableColumnModel.getColumnCount();
        for (int i = 0; i < cantCols; i++) {
            column = jTableColumnModel.getColumn(i);
            int pWidth = Math.round(columnWidthPercentage[i] * tW);
            column.setPreferredWidth(pWidth);
        }
    }    
    
    
    private List<Host> hosts = new ArrayList<> ();
    private MyTableModel model;
    
    
    public FavouriteHostsPanel(List<Host> hosts)
    {
        super();
        setLayout(new GridLayout(1,0));
        
        this.hosts.addAll(hosts);
        model = new MyTableModel();

        final JTable tbl = new JTable(model);
 
        tbl.setShowGrid(true);
        tbl.setPreferredScrollableViewportSize(new Dimension(400, 150));
        tbl.setFillsViewportHeight(true);
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(tbl);
        //Add the scroll pane to this panel.
        add(scrollPane);        
        
        
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem menuItemRemove = new JMenuItem("Remove Current Row");
        menuItemRemove.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    int selectedRow = tbl.getSelectedRow();
                    model.removeRow(selectedRow);                    
                }
            });
        popupMenu.add(menuItemRemove);
        
        JMenuItem menuItemRemoveAll = new JMenuItem("Remove All Rows");        
        menuItemRemoveAll.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    model.clear();
                }
            });
        popupMenu.add(menuItemRemoveAll);        
        
        tbl.setComponentPopupMenu(popupMenu);   
        tbl.addMouseListener(new TableMouseListener(tbl));        
        
        tbl.doLayout();
        resizeColumns(tbl, new float[] {80.0f, 20.0f} );
    }

    
    public List<Host> getHosts()
    {
        return hosts;
    }
    
    
    public class MyTableModel extends AbstractTableModel {
        private String[] columnNames = new String[] { "Host", "Favourite" };

        public int getColumnCount() 
        {
            return columnNames.length;
        }

        public int getRowCount() 
        {
            return hosts.size();
        }

        public String getColumnName(int col) 
        {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) 
        {
            Host hst = hosts.get(row);
            switch (col)
            {
                case 0: return hst.getDisplayName();
                case 1: return hst.isFavourite();
            }
            
            return null;
        }

        public Class getColumnClass(int c) 
        {
            return getValueAt(0, c).getClass();
        }

        /*
         * Don't need to implement this method unless your table's
         * editable.
         */
        public boolean isCellEditable(int row, int col) 
        {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            if (col == 0) {
                return false;
            } else {
                return true;
            }
        }    
        
        public void setValueAt(Object value, int row, int col) 
        {
            if (col == 1)
            {
                Host hst = hosts.get(row);
                hst.setFavourite((Boolean) value);
                fireTableCellUpdated(row, col);
            }
        }     
        
        public void removeRow(int row)
        {
            if (row > -1 && row < hosts.size())
            {
                hosts.remove(row);
                this.fireTableRowsDeleted(row, row);
            }
        }
        
        public void clear()
        {
            int size = hosts.size();
            hosts.clear();
            this.fireTableRowsDeleted(0, size - 1);
        }
    }
    
    
    /**
     * A mouse listener for a JTable component.
     * @author www.codejava.neet
     *
     */
    public class TableMouseListener extends MouseAdapter 
    {

        private JTable table;

        public TableMouseListener(JTable table) 
        {
            this.table = table;
        }

        @Override
        public void mousePressed(MouseEvent event) 
        {
            // selects the row at which point the mouse is clicked
            Point point = event.getPoint();
            int currentRow = table.rowAtPoint(point);
            if (currentRow != -1)
            {
                table.setRowSelectionInterval(currentRow, currentRow);
            }
        }
    }    
    
    
}
