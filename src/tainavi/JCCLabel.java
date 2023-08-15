package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;


public class JCCLabel extends JLabel {

	private static final long serialVersionUID = 1L;

	private boolean invert = true;
	
	private MouseAdapter mLsr = null;
	
	/**
	 * @param invert true:背景色を選択する、false:文字色を選択する
	 */
	public JCCLabel(String s, Color c, boolean invert, final Component parent, final VWColorChooserDialog ccwin) {
		super(s,SwingConstants.CENTER);
		this.invert = invert;
		
		this.setChoosed(c);
		this.setOpaque(true);
		this.setBorder(new LineBorder(new Color(0,0,0)));
		
		mLsr = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					
					JCCLabel jl = (JCCLabel)e.getSource();
					
					ccwin.setColor(jl.getChoosed());
					CommonSwingUtils.setLocationCenter(parent,ccwin);
					ccwin.setVisible(true);
					
					if (ccwin.getSelectedColor() != null ) {
						jl.setChoosed(ccwin.getSelectedColor());
					}
				}
			}
		};
		
		this.addMouseListener(mLsr);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		if ( super.isEnabled() == enabled ) {
			// 処理なし
			return;
		}
		
		// disabledなら触んな！
		super.setEnabled(enabled);
		if ( enabled ) {
			this.addMouseListener(mLsr);
		}
		else {
			this.removeMouseListener(mLsr);
		}
	}
	
	public void setChoosed(Color c) {
		if (invert) {
			setBackground(c);
		}
		else {
			setForeground(c);
			setBackground(Color.WHITE);
		}
	}
	
	public Color getChoosed() {
		if (invert) {
			return getBackground();
		}
		else {
			return getForeground();
		}
	}
}