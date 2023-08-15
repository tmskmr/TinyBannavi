package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * 文字色をつけられるセルのレンダラ。反転して背景色を変えることもできる。
 */
public class VWColorCharCellRenderer extends DefaultTableCellRenderer {
	
	private static final long serialVersionUID = 1L;
	
	private boolean invert = false;
	
	private JLabel renderer = null;
	private Font macFont = null;
	
	public VWColorCharCellRenderer() {
		this(JLabel.CENTER,false);
	}
	public VWColorCharCellRenderer(int align) {
		this(align,false);
	}
	public VWColorCharCellRenderer(int align, boolean invert) {
		super();
		this.invert = invert;
		
		renderer = new JLabel();
		renderer.setHorizontalAlignment(align);
		renderer.setOpaque(true);
	}
	
	public void setMacMarkFont() {
		Font f = super.getFont();
		macFont = new Font("Osaka",f.getStyle(),f.getSize());
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		
		Color c = Color.BLACK;
		String es = null;
		String val = (String)value;
		if ( val != null ) {
			if ( val.length() > 8 ) {
				String cs = val.substring(val.length()-8);
				if ( cs.startsWith("\0") && (c = CommonUtils.str2color(cs)) != null ) {
					es = val.substring(0,val.length()-8);
				}
			}
			else if ( val.length() == 8 ) {
				if ( val.startsWith("\0") ) {
					es = "";
				}
			}
			if ( es == null ) {
				es = val;
			}
		}
		else {
			es = "";
		}
		
		// 文字の設定
		renderer.setText(es);
		
		// フォントの設定
		Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		Font f = comp.getFont();
		if ( macFont != null ) {
			renderer.setFont(macFont.deriveFont((f.getStyle()|Font.BOLD) ^ Font.BOLD, f.getSize()));
		}
		else {
			renderer.setFont(f.deriveFont((f.getStyle()|Font.BOLD) ^ Font.BOLD, f.getSize()));
		}
		
		// セルレンダラの構成
		if ( invert ) {
			renderer.setBackground(c);
			
			if (isSelected) {
				renderer.setForeground(table.getSelectionBackground());
			}
			else {
				renderer.setForeground(getInvertedForeground(table));
			}
		}
		else {
			renderer.setForeground(c);
			
			// 選択・非選択時の「背景」色が他のセルと同期するようにする
			if (isSelected) {
				renderer.setBackground(table.getSelectionBackground());
			}
			else {
				renderer.setBackground(table.getBackground());
			}
		}
		
		return renderer;
	}
	
	protected Color getInvertedForeground(JTable table) { return table.getBackground(); }
	
}
