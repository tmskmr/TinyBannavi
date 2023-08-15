package tainavi;

public class MarkedHistory {

	private String center = "";
	private String startDateTime = "";
	private String detail = "";
	
	public String getCenter() { return center; }
	public void setCenter(String s) { center = s; }
	public String getStartDateTime() { return startDateTime; }
	public void setStartDateTime(String s) { startDateTime = s; }
	public String getDetail() { return detail; }
	public void setDetail(String s) { detail = s; }
	
	public boolean isMatch(MarkedHistory mh) {
		return (this.center.equals(mh.getCenter()) && this.startDateTime.equals(mh.getStartDateTime()));
	}
	
	public boolean isModified(MarkedHistory mh) {
		return ! this.detail.equals(mh.getDetail());
	}
}
