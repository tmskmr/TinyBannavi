package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class JTableRowHeader extends JTable {

	private static final long serialVersionUID = 1L;

	// 新バージョン
	public JTableRowHeader(final RowItemList rowData) {
		super();
		
		String[] colname = {""};
		
		DefaultTableModel tableModel_rowheader = new DefaultTableModel(colname,0) {

			private static final long serialVersionUID = 1L;

			@Override
			public Object getValueAt(int row, int column) {
				return row+1;
			}
			@Override
			public int getRowCount() {
				return rowData.size();
			}
		}; 
		
		_JTableRowHeader(tableModel_rowheader);
	}
	
	// 旧バージョン
	public JTableRowHeader(final ArrayList<String[]> rowData) {
		super();
		
		String[] colname = {""};
		
		DefaultTableModel tableModel_rowheader = new DefaultTableModel(colname,0) {

			private static final long serialVersionUID = 1L;

			@Override
			public Object getValueAt(int row, int column) {
				return row+1;
			}
			@Override
			public int getRowCount() {
				return rowData.size();
			}
		}; 
		
		_JTableRowHeader(tableModel_rowheader);
	}
		
	private void _JTableRowHeader(DefaultTableModel tableModel_rowheader) {
		
		int colwidth = 35;
		
		this.setModel(tableModel_rowheader);
		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.setRowSelectionAllowed(false);
		this.setEnabled(false);
		
		DefaultTableCellRenderer r = new DefaultTableCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus, int row, int column) {
				//
				JLabel label = new JLabel();
				label.setHorizontalAlignment(JLabel.CENTER);
				//
				int no = (Integer)value;
				label.setText(String.valueOf(no));
				//
				label.setOpaque(true);
				label.setBackground(table.getTableHeader().getBackground());
				//
				Font f = table.getFont();
				int sPlane = (f.getStyle() | Font.BOLD) ^ Font.BOLD;
				int sBold = (f.getStyle() | Font.BOLD);
				if (no % 10 == 0) {
					label.setFont(new Font(f.getFontName(),sBold,f.getSize()));
					label.setForeground(Color.RED);
				}
				else {
					label.setFont(new Font(f.getFontName(),sPlane,f.getSize()));
					label.setForeground(Color.GRAY);
				}
				return label;
			}
		};
		
		DefaultTableColumnModel columnModel = (DefaultTableColumnModel)this.getColumnModel();
		TableColumn column = columnModel.getColumn(0);
		column.setPreferredWidth(colwidth);
		column.setCellRenderer(r);
		
		// フォントサイズ変更にあわせて行の高さを変える
		this.addPropertyChangeListener("font", new RowHeightChangeListener(4));

		// 行の高さの初期値の設定
		this.firePropertyChange("font", "old", "new");
	}
}
