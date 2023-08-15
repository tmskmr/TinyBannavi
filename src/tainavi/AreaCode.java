package tainavi;



public class AreaCode implements Cloneable {
	private String area;
	private String code;
	private boolean selected;
	
	public AreaCode() {
		this.area = "";
		this.code = "";
		this.selected = false; 
	}
	
	@Override
	public AreaCode clone() {
		try {
			return (AreaCode) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}
	
	public String getArea() { return this.area; }
	public void setArea(String s) { this.area = s; }

	public String getCode() { return this.code; }
	public void setCode(String s) { this.code = s; }
	
	public boolean getSelected() { return this.selected; }
	public void setSelected(boolean b) { this.selected = b; }
}
