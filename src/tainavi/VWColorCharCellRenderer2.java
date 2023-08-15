package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * <P>番組表のタイトルの色強調をおこなうためのレンダラ
 * <P>[マーク]\0[テキスト1]\0[テキスト2(強調部分)]\0[テキスト3]
 * <P>強調色は{@link VWColorCharCellRenderer2#setMatchedKeywordColor(Color)}で指定する
 * <P>各項目は省略しても構わない
 */
public class VWColorCharCellRenderer2 extends DefaultTableCellRenderer {

	private static final long serialVersionUID = 1L;
	
	private final JPanel renderer = new JPanel();
	private final JLabel mark = new JLabel();
	private final JLabel t1 = new JLabel();
	private final JLabel t2 = new JLabel();
	private final JLabel t3 = new JLabel();
	
	private Color matchedc = Color.PINK;
	
	public void setMatchedKeywordColor(Color c) {
		matchedc = c;
	}
	
	public VWColorCharCellRenderer2() {
		//
		renderer.setLayout(new BoxLayout(renderer, BoxLayout.LINE_AXIS));
		renderer.setOpaque(true);
		//
		//
		//Font fm = mark.getFont();
	    mark.setForeground(Color.RED);
		mark.setHorizontalAlignment(JLabel.LEFT);
		//mark.setFont(fm.deriveFont(fm.getStyle() | Font.BOLD));
	    renderer.add(mark);
		//
		//Font f1 = t1.getFont();
	    t1.setForeground(Color.BLACK);
		t1.setHorizontalAlignment(JLabel.LEFT);
		//t1.setFont(f1.deriveFont((f1.getStyle()|Font.BOLD) ^ Font.BOLD));
	    renderer.add(t1);
		//
		//Font f2 = t2.getFont();
	    t2.setForeground(matchedc);
		t2.setHorizontalAlignment(JLabel.LEFT);
		//t2.setFont(f2.deriveFont(f2.getStyle() | Font.BOLD));
	    renderer.add(t2);
		//
		//Font f3 = t3.getFont();
	    t3.setForeground(Color.BLACK);
		t3.setHorizontalAlignment(JLabel.LEFT);
		//t3.setFont(f3.deriveFont((f3.getStyle()|Font.BOLD) ^ Font.BOLD));
	    renderer.add(t3);
	}
	
	@Override
	public void setForeground(Color c) {
		super.setForeground(c);
		if ( t1 != null ) {
			t1.setForeground(c);
			t3.setForeground(c);
		}
	};

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		
		String[] xs = ((String)value).split("\0",4);
		
		// 文字の設定
		mark.setText((xs.length>=1)?(xs[0]):(""));
		t1.setText((xs.length>=2)?(xs[1]):(""));
		t2.setText((xs.length>=3)?(xs[2]):(""));
		t3.setText((xs.length>=4)?(xs[3]):(""));
		
		// フォントの設定
		Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		Font f = comp.getFont();
		mark.setFont(f.deriveFont(f.getStyle()|Font.BOLD));
		t1.setFont(f.deriveFont((f.getStyle()|Font.BOLD) ^ Font.BOLD));
		t2.setFont(f.deriveFont(f.getStyle()|Font.BOLD));
		t3.setFont(f.deriveFont((f.getStyle()|Font.BOLD) ^ Font.BOLD));
		
		// 選択・非選択時の文字色が他のセルと同期するようにする
		if (isSelected) {
			t1.setForeground(table.getSelectionForeground());
			t3.setForeground(table.getSelectionForeground());
		}
		else {
			t1.setForeground(table.getForeground());
			t3.setForeground(table.getForeground());
		}
		
		// キーワードにマッチした箇所の強調色
		t2.setForeground(matchedc);
		
		return renderer;
	}
}
