
package tainavi;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class JSliderPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private JSlider jslider = null;
	private JLabel jlabel = null;
	private String labelstr = null;
	
	public JSliderPanel(String s, int labelWidth, int min, int max, int sliderWidth) {
		this(s, labelWidth, min, max, 1, sliderWidth);
	}
	public JSliderPanel(String s, int labelWidth, int min, int max, int step, int sliderWidth) {
		
		this.setLayout(new BoxLayout(this,BoxLayout.LINE_AXIS));
		
		Dimension d = null;
		
		jlabel = new JLabel(labelstr = s);
		d = jlabel.getPreferredSize();
		d.width = labelWidth;
		d.height = 100;
		jlabel.setMaximumSize(d);
		this.add(jlabel);

		final int stepby = (step>0)?(step):(1);
		max /= stepby;
		min /= stepby;
		
		jslider = new JSlider(min,max);
		d = jslider.getPreferredSize();
		d.width = sliderWidth;
		d.height = 100;
		jslider.setMaximumSize(d);
		jslider.setSnapToTicks(true);
		
		jslider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				jlabel.setText(labelstr+"["+(jslider.getValue()*stepby)+"]");
			}
		});
		setValue(min);
		this.add(jslider);
	}
	
	public int getValue() {
		return jslider.getValue();
	}
	public void setValue(int n) {
		jslider.setValue(n);
	}
	public void setEnabled(boolean b) {
		this.jlabel.setEnabled(b);
		this.jslider.setEnabled(b);
	}
	
	public void addChangeListener(ChangeListener l) {
		this.jslider.addChangeListener(l);
	}
	public void removeChangeListener(ChangeListener l) {
		this.jslider.removeChangeListener(l);
	}
}
