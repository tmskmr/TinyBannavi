package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.Border;

public class DashBorder implements Border {
	
	private Color color = Color.RED;
	private int thickness = 3;
	private int length = 6;
	private int space = 4;
	
	public DashBorder(Color color, int thickness, int length, int space) {
		this.color = color;
		this.thickness = thickness;
		this.length = length;
		this.space = space;
	}
	
	@Override
	public boolean isBorderOpaque() {
		return false;
	}
	
	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(thickness,thickness,thickness,thickness);
	}

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		g.setColor(color);
		for (int i = 0; i < width; i += length) {
			g.fillRect(i, 0, length, thickness);
			g.fillRect(i, (height-0)-thickness, length, thickness);
			i += space;
		}
		for (int i = 0; i < height; i += length) {
			g.fillRect(0, i, thickness, length);
			g.fillRect((width-0)-thickness, i, thickness, length);
			i += space;
		}
	}
	
	//
	
	public Color getDashColor() {
		return color;
	}
	
	public void setDashColor(Color color) {
		this.color = color;
	}
	
	public int getThickness() {
		return thickness;
	}
	
	public void setThickness(int thickness) {
		this.thickness = thickness;
	}

}
