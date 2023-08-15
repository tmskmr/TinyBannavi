package tainavi;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


abstract class JColorChooseSlider extends JPanel {

	private static final long serialVersionUID = 1L;

	private final int min = 0;
	private final int max = 255;
	
	private JLabel jlabel;
	private JSlider jslider;
	private JSpinner jspinner;
	
	private ChangeListener slCl;
	private ChangeListener spCl;
	
	private SpinnerListModel model = new SpinnerListModel();
	private boolean bHex = false;
	private ArrayList<String> decData = new ArrayList<String>();
	private ArrayList<String> hexData = new ArrayList<String>();
	
	// コンストラクタ
	
	public JColorChooseSlider(String s) {
		
		super();
		
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		jlabel = new JLabel(s);
		jslider = new JSlider(min,max,min);
		jspinner = new JSpinner(model);
		
		jlabel.setHorizontalAlignment(SwingConstants.CENTER);
		
		jslider.setMinorTickSpacing(17);
		jslider.setMajorTickSpacing(85);
		jslider.setPaintTicks(true);
		jslider.setPaintLabels(true);
		
		Dimension d;
		
		d = jlabel.getPreferredSize();
		d.width = 25;
		jlabel.setPreferredSize(d);
		jlabel.setMaximumSize(d);
		
		d = jslider.getPreferredSize();
		d.width = max;
		jslider.setPreferredSize(d);
		jslider.setMaximumSize(d);
		
		d = jspinner.getPreferredSize();
		d.width = 75;
		jspinner.setPreferredSize(d);
		jspinner.setMaximumSize(d);
		
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.add(jlabel);
		this.add(Box.createRigidArea(new Dimension(10,10)));
		this.add(jslider);
		this.add(Box.createRigidArea(new Dimension(10,10)));
		this.add(jspinner);

		// 初期設定はDEC
		bHex = false;
		for (int i=min; i<=max; i++) {
			decData.add(v2s(i));
		}
		bHex = true;
		for (int i=min; i<=max; i++) {
			hexData.add(v2s(i));
		}
		setHex(false);
		
		// スライダーとスピンネルを連動させる
		
		slCl = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				jspinner.removeChangeListener(spCl);
				model.setValue(v2s(jslider.getValue()));
				evHandle(jslider.getValue());
				jspinner.addChangeListener(spCl);
			}
		};
		spCl = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int n = s2v((String) jspinner.getValue());
				if (n>=min && n<=max) {
					// 入力が適正な場合
					jslider.removeChangeListener(slCl);
					jslider.setValue(n);
					evHandle(n);
					jslider.addChangeListener(slCl);
				}
				else {
					// 入力が不正な場合
					jspinner.removeChangeListener(spCl);
					jspinner.setValue(v2s(jslider.getValue()));
					jspinner.addChangeListener(spCl);
				}
			}
		};
		jslider.addChangeListener(slCl);
		jspinner.addChangeListener(spCl);
	}
	
	// 抽象メソッド
	
	abstract void evHandle(int n);
	
	// 公開メソッド
	
	public int getValue() {
		return jslider.getValue();
	}
	
	public void setValue(int n) {
		jslider.setValue(n);
	}
	
	public void setHex(boolean b) {
		bHex = b;
		int n = jslider.getValue();
		if (bHex) {
			model.setList(hexData);
		}
		else {
			model.setList(decData);
		}
		jslider.setValue(n);
	}
	
	// 非公開メソッド
	
	private String v2s(int n) {
		if (bHex) {
			return String.format("%x",n);
		}
		else {
			return String.format("%d",n);
		}
	}
	
	private int s2v(String s) {
		if (bHex) {
			if (s.matches("^[0-9a-fA-F]+$")) {
				return Integer.decode("0x"+s);
			}
			else {
				return -1;
			}
		}
		else {
			if (s.matches("^[0-9]+$")) {
				return Integer.valueOf(s);
			}
			else {
				return -1;
			}
		}
	}
}
