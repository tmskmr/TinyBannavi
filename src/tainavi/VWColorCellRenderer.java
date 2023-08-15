package tainavi;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JTable;

/**
 * 後方互換のため残してあるだけ
 * @see VWColorCharCellRenderer
 */
public class VWColorCellRenderer extends VWColorCharCellRenderer {

	private static final long serialVersionUID = 1L;

	public VWColorCellRenderer() {
		super(JLabel.CENTER,true);
	}
	
	@Override
	protected Color getInvertedForeground(JTable table) { return table.getForeground(); }
	
}
