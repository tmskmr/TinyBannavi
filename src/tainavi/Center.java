package tainavi;

import java.awt.Color;

public class Center implements Cloneable {
	private String areacode;
	private String center;
	private String center_orig = null;
	private String link;
	private String type;
	private boolean enabled;
	private int order;
	private Color bgcolor;
	
	public Center() {
		this.areacode = "";
		this.center = "";
		this.link = "";
		this.type = "";
		this.enabled = true;
		this.order = 0;
		this.bgcolor = new Color(180,180,180);
	}
	public Center(String s, boolean b) {
		this.areacode = "";
		this.center = s;
		this.link = "";
		this.type = "";
		this.enabled = b;
		this.order = 0;
		this.bgcolor = new Color(180,180,180);
	}
	public Center(String s, String l, boolean b) {
		this.areacode = "";
		this.center = s;
		this.link = l;
		this.type = "";
		this.enabled = b;
		this.order = 0;
		this.bgcolor = new Color(180,180,180);
	}
	
	@Override
	public Center clone() {
		try {
			return (Center) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}
	
	public String getAreaCode() { return this.areacode; }
	public void setAreaCode(String s) { this.areacode = s; }
	
	public String getCenter() { return this.center; }
	public void setCenter(String s) { this.center = s; }

	public String getCenterOrig() { return this.center_orig; }
	public void setCenterOrig(String s) { this.center_orig = s; }

	public String getLink() { return this.link; }
	public void setLink(String s) { this.link = s; }

	public String getType() { return this.type; }
	public void setType(String s) { this.type = s; }

	public boolean getEnabled() { return this.enabled; }
	public void setEnabled(boolean b) { this.enabled = b; }

	public int getOrder() { return this.order; }
	public void setOrder(int o) { this.order = o; }

	public Color getBgColor() { return this.bgcolor; }
	public void setBgColor(Color c) { this.bgcolor = c; }
}
