package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;


public class JNETable extends JTable {

	private static final long serialVersionUID = 1L;

	// 編集禁止
	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	// 行ごとに色を変える
	protected boolean isSepRowColor = true;
	public void setSepRowColor(boolean b) { isSepRowColor = b; }

	protected static final Color evenColor = new Color(240,240,255);

	@Override
	public Component prepareRenderer(TableCellRenderer tcr, int row, int column) {
		Component c = null;
		try{
			c = super.prepareRenderer(tcr, row, column);
		}
		catch(ArrayIndexOutOfBoundsException e){
			return null;
		}

		Color fgColor = null;
		Color bgColor = null;
		if(isRowSelected(row)) {
			fgColor = this.getSelectionForeground();
			bgColor = this.getSelectionBackground();
		}
		else {
			fgColor = this.getForeground();
			bgColor = (isSepRowColor && row%2 == 1)?(evenColor):(super.getBackground());
		}
		if ( ! (tcr instanceof VWColorCharCellRenderer) && ! (tcr instanceof VWColorCharCellRenderer2) && ! (tcr instanceof VWColorCellRenderer)) {
			c.setForeground((this.isEnabled()) ? fgColor : Color.GRAY);
		}
		if ( ! (tcr instanceof VWColorCellRenderer)) {
			c.setBackground(bgColor);
		}
		return c;
	}

	// 列の表示・非表示
	private class ColumnData {
		TableColumn column;
		boolean visible;
	}

	private ColumnData[] colDat = null;

	public void setColumnVisible(String name, boolean b) {

		if ( name == null ) {
			return;
		}

		// 初回は情報を蓄える
		if ( colDat == null ) {
			colDat = new ColumnData[this.getColumnModel().getColumnCount()];
			for ( int i=0; i<colDat.length; i++ ) {
				ColumnData cdat = new ColumnData();
				cdat.column = this.getColumnModel().getColumn(i);
				cdat.visible = true;
				colDat[i] = cdat;
			}
		}

		// 列を検索する
		int idx = -1;
		for ( int i=0; i<colDat.length; i++ ) {
			if ( name.equals((String)colDat[i].column.getHeaderValue()) ) {
				if ( colDat[i].visible != b ) {
					idx = i;
				}
				break;
			}
		}
		if ( idx == -1 ) {
			return;
		}

		if ( ! b ) {
			// 削除する
			colDat[idx].visible = false;
			this.removeColumn(colDat[idx].column);
		}
		else {
			// 一旦全て削除して
			for ( int i=0; i<colDat.length; i++ ) {
				if ( colDat[i].visible == true ) {
					this.removeColumn(colDat[i].column);
				}
			}
			// 有効列のみ追加しなおす
			colDat[idx].visible = true;
			for ( int i=0; i<colDat.length; i++ ) {
				if ( colDat[i].visible == true ) {
					this.addColumn(colDat[i].column);
				}
			}
		}
	}


    // getToolTipText()をオーバーライド
	@Override
    public String getToolTipText(MouseEvent e){
    	int row = rowAtPoint(e.getPoint());
    	int column = columnAtPoint(e.getPoint());
    	if (row == -1 || column == -1)
    		return null;

    	try{
	    	String text = (String)getValueAt(row, column);
	    	if (text == null)
	    		return null;

			String[] xs = text.split("\0", 4);
			if (xs.length <= 0 || xs[0] == null)
				return null;

	    	Insets i = getInsets();
	        Rectangle rect = getCellRect(row, column, false);
	        FontMetrics fm = getFontMetrics(getFont());

	        int width = fm.stringWidth(xs[0]);

	        if (width > rect.width-(i.left+i.right))
	        	return xs[0];
	        else
	        	return null;
    	}
    	catch(ClassCastException ex){
    		return null;
    	}
    }

	/*
	 * コンストラクタ
	 */
	public JNETable(boolean multi_select) {
		if (multi_select) {
			this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		}
		else {
			this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		this.getTableHeader().setReorderingAllowed(false);

		// フォントサイズ変更にあわせて行の高さを変える
		this.addPropertyChangeListener("font", new RowHeightChangeListener(4));

		// 行の高さの初期値の設定
		this.firePropertyChange("font", "old", "new");
	}

	public JNETable(TableModel d, boolean b) {
		this(b);
		this.setModel(d);
	}
}
