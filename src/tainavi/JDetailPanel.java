package tainavi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;


public class JDetailPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	JScrollPane jscrollpane = null;
	private JPanel jpanel_title = null;
	private JTextAreaWithPopup jta = null;
	private JLabel jlabel_time = null;
	private JPanel jpanel_mtitle = null;
	private JLabel jlabel_mark = null;
	private JLabel jlabel_title = null;

	private final int titleFontSize = 20;
	//private int textAreaRows = 4;

	public JDetailPanel() {

		this.setLayout(new BorderLayout());

		jpanel_title = new JPanel();
		jpanel_title.setLayout(new BorderLayout());
		this.add(jpanel_title, BorderLayout.PAGE_START);

		Font f = null;

		jlabel_time = new JLabel();
		f = jlabel_time.getFont();
		jlabel_time.setFont(f.deriveFont(f.getStyle() | Font.BOLD, titleFontSize));
		jpanel_title.add(jlabel_time,BorderLayout.LINE_START);

		jpanel_mtitle = new JPanel();
		jpanel_mtitle.setLayout(new BorderLayout());
		jpanel_title.add(jpanel_mtitle, BorderLayout.CENTER);

		jlabel_mark = new JLabel();
		f = jlabel_mark.getFont();
		jlabel_mark.setFont(f.deriveFont(f.getStyle() | Font.BOLD, titleFontSize));
		jlabel_mark.setForeground(Color.RED);
		jpanel_mtitle.add(jlabel_mark, BorderLayout.LINE_START);
		jlabel_mark.setText(" ");

		jlabel_title = new JLabel();
		f = jlabel_title.getFont();
		jlabel_title.setFont(f.deriveFont(f.getStyle() | Font.BOLD, titleFontSize));
		jlabel_title.setForeground(Color.BLUE);
		jpanel_mtitle.add(jlabel_title,BorderLayout.CENTER);
		jlabel_title.setText(" ");

		jta = CommonSwingUtils.getJta(this,4,0);

		jscrollpane = new JScrollPane(jta,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		jscrollpane.setBorder(new EmptyBorder(0,0,0,0));
		this.add(jscrollpane,BorderLayout.CENTER);
	}

	public void setLabel(String s, String e, String t, String m) {
		if (s == null || s.length() == 0 || e == null || e.length() == 0) {
			jlabel_time.setText(" ");
		}
		else {
			jlabel_time.setText(s+"～"+e+"　");
		}

		jlabel_title.setText(t);

		if (m != null){
			String [] ms = m.split("\0", 2);

			jlabel_mark.setText(ms.length > 0 ? ms[0] : "");
			jlabel_mark.setForeground(ms.length > 1 ? CommonUtils.str2color(ms[1]) : Color.RED);
		}
		else
			jlabel_mark.setText(m);
	}
	public String getText() {
		return jta.getText();
	}
	public void setText(String s) {
		jta.setText(s);
		jta.setCaretPosition(0);
	}

	public void clear() {
		setLabel(null,null,null,null);
		setText("");
	}

	public int getRows() {
		return jta.getRows();
	}

	public void setRows(int rows) {
//		jta.setRows(rows);
	}

	/*
	 * 行数を指定して高さを取得する
	 */
	public int getHeightFromRows(int rows){
		int hl = jta.getFont().getSize();

		return CommonUtils.getPixelFromPoint(hl*rows+titleFontSize);
	}

	@Override
	public void addMouseListener(MouseListener l) {
		jlabel_time.addMouseListener(l);
		jlabel_title.addMouseListener(l);
		jta.addMouseListener(l);
	}

	@Override
	public void removeMouseListener(MouseListener l) {
		jlabel_time.removeMouseListener(l);
		jlabel_title.removeMouseListener(l);
		jta.removeMouseListener(l);
	}
}
