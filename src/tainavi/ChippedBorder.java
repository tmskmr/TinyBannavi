package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.border.LineBorder;

public class ChippedBorder extends LineBorder {
	
	private static final long serialVersionUID = 1L;

	public ChippedBorder(Color color, int thickness) {
		super(color, thickness);
	}

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		g.fillRect(0, 0, width, thickness);				// 上
		//g.fillRect(0, height-thickness, width, thickness);	// 下
		//g.fillRect(0, 0, thickness, height);			// 左
		g.fillRect(width-thickness, 0, thickness, height);	// 右
	}

}
