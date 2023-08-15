package tainavi;

import java.awt.Dimension;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;


public class JRadioButtonPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private ButtonGroup bg = null;
	private ArrayList<JRadioButton> jrblist = null;
	private JLabel jlabel = null;
	private JPanel jpanel = null;
	
	public JRadioButtonPanel(String s, int labelWidth) {
		this.setLayout(new BoxLayout(this,BoxLayout.LINE_AXIS));
		
		jlabel = new JLabel(s);
		Dimension d = jlabel.getPreferredSize();
		d.width = labelWidth;
		jlabel.setMaximumSize(d);
		this.add(jlabel);
		
		jpanel = new JPanel();
		jpanel.setLayout(new BoxLayout(jpanel,BoxLayout.PAGE_AXIS));
		this.add(jpanel);
		
		//this.add(Box.createRigidArea(new Dimension(10,d.height)));
		
		bg = new ButtonGroup();
		jrblist = new ArrayList<JRadioButton>();
	}

	public void add(String text, boolean selected) {
		JRadioButton jrb = new JRadioButton(text,selected);
		bg.add(jrb);
		jpanel.add(jrb);
		jrblist.add(jrb);
	}
	public JRadioButton getSelectedItem() {
		for ( int i=0; i<jrblist.size(); i++ ) {
			if (jrblist.get(i).isSelected()) {
				return jrblist.get(i);
			}
		}
		return null;
	}
	public void setSelectedItem(String text) {
		for ( int i=0; i<jrblist.size(); i++ ) {
			if (jrblist.get(i).getText().equals(text)) {
				jrblist.get(i).setSelected(true);
				return;
			}
		}
	}
	public int getSelectedIndex() {
		for ( int i=0; i<jrblist.size(); i++ ) {
			if (jrblist.get(i).isSelected()) {
				return i;
			}
		}
		return 0;
	}
	public void setSelectedIndex(int n) {
		if (jrblist.size() > n) {
			jrblist.get(n).setSelected(true);
		}
	}
	
	public void addItemListener(ItemListener l) {
		for ( JRadioButton b : jrblist ) {
			b.addItemListener(l);
		}
	}
	
	public void removeItemListener(ItemListener l) {
		for ( JRadioButton b : jrblist ) {
			b.removeItemListener(l);
		}
	}
}
