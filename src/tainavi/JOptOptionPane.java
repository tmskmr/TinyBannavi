package tainavi;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;


public class JOptOptionPane extends JOptionPane {
	
	private static final long serialVersionUID = 1L;
	
	private static JCheckBoxPanel jcheckbox = null;

	public static int showConfirmDialog(Component parentComponent, String message, String notice, String description, String title, int optionType) {
		JPanel panel = new JPanel();
		BoxLayout layout = new BoxLayout(panel,BoxLayout.Y_AXIS);
		panel.setLayout(layout);
		
		JLabel jlabel = new JLabel(message,JLabel.CENTER);
		jlabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(jlabel);
		
		panel.add(new JLabel(" "));	// Vgap
		
		jcheckbox = new JCheckBoxPanel(notice, 0, true);
		jcheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(jcheckbox);
		
		if ( description != null && description.length() > 0 ) {
			JLabel desc = new JLabel(description,JLabel.CENTER);
			desc.setAlignmentX(Component.LEFT_ALIGNMENT);
			desc.setForeground(Color.RED);
			panel.add(desc);
		}
		
		return showConfirmDialog(parentComponent, panel, title, optionType);
	}
	
	public static boolean isSelected() {
		return (jcheckbox != null && jcheckbox.isSelected());
	}

}
