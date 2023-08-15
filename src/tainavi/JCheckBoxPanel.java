package tainavi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class JCheckBoxPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private JCheckBox jcheckbox = null;
	private JLabel jlabel = null;
	
	public JCheckBoxPanel(String s, int labelWidth) {
		this(s,labelWidth,false);
	}
	public JCheckBoxPanel(String s, int labelWidth, boolean rev) {
		if ( rev ) {
			_JCheckBoxPanelRev(s,labelWidth);
		}
		else {
			_JCheckBoxPanel(s,labelWidth);
		}
	}
	
	private void _JCheckBoxPanel(String s, int labelWidth) {
		this.setLayout(new BoxLayout(this,BoxLayout.LINE_AXIS));
		
		jlabel = new JLabel(s);
		Dimension d = jlabel.getPreferredSize();
		if ( labelWidth > 0 ) {
			d.width = labelWidth;
		}
		d.height = 100;
		jlabel.setMaximumSize(d);
		this.add(jlabel);
		
		this.add(jcheckbox = new JCheckBox());
		
		jlabel.addMouseListener(ml_labelClicked);
	}
	private void _JCheckBoxPanelRev(String s, int labelWidth) {
		this.setLayout(new BoxLayout(this,BoxLayout.LINE_AXIS));
		
		jcheckbox = new JCheckBox();
		this.add(jcheckbox);
		
		jlabel = new JLabel(s);
		Dimension d = jlabel.getPreferredSize();
		if ( labelWidth > 0 ) {
			d.width = labelWidth;
		}
		d.height = 100;
		jlabel.setMaximumSize(d);
		this.add(jlabel);
		
		jlabel.addMouseListener(ml_labelClicked);
	}
	
	public boolean isSelected() {
		return jcheckbox.isSelected();
	}
	public void setSelected(boolean b) {
		jcheckbox.setSelected(b);
	}
	public void setEnabled(boolean b) {
		if (this.jlabel != null) {
			this.jlabel.setEnabled(b);
			
			jlabel.removeMouseListener(ml_labelClicked);
			if ( b ) {
				jlabel.addMouseListener(ml_labelClicked);
			}
		}
		if (this.jcheckbox != null) {
			this.jcheckbox.setEnabled(b);
		}
	}
	public void setText(String s) {
		jlabel.setText(s);
	}
	
	public void addActionListener(ActionListener l) {
		this.jcheckbox.addActionListener(l);
	}
	public void removeActionListener(ActionListener l) {
		this.jcheckbox.removeActionListener(l);
	}
	
	public void addItemListener(ItemListener l) {
		this.jcheckbox.addItemListener(l);
	}
	public void removeItemListener(ItemListener l) {
		this.jcheckbox.removeItemListener(l);
	}

	public void setForeground(Color fg) {
		if (this.jlabel != null) {
			this.jlabel.setForeground(fg);
		}
	}
	
	
	//
	private MouseAdapter ml_labelClicked = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			if ( jcheckbox != null ) {
				jcheckbox.doClick();
			}
		}
	};
	
}
