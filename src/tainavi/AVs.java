package tainavi;

public class AVs {
	private String recorderId = "";
	private String genre = "";
	private String videorate = "";
	private String audiorate = "";
	private String dvdcompat = "";
	
	private String device = "";
	private String xchapter = "";
	private String mschapter = "";
	private String mvchapter = "";
	
	private String aspect = "";
	private String bvperf = "";
	private String lvoice = "";
	private String autodel = "";
	private String portable = "";

	private String folder = "";
	
	private Boolean pursues = true;
	
	public void setRecorderId(String s) { recorderId = s; }
	public String getRecorderId() { return recorderId; }
	public void setGenre(String s) { genre = s; }
	public String getGenre() { return genre; }
	public void setVideorate(String s) { videorate = s; }
	public String getVideorate() { return videorate; }
	public void setAudiorate(String s) { audiorate = s; }
	public String getAudiorate() { return audiorate; }
	public void setDVDCompat(String s) { dvdcompat = s; }
	public String getDVDCompat() { return dvdcompat; }

	public void setDevice(String s) { device = s; }
	public String getDevice() { return device; }
	public void setXChapter(String s) { xchapter = s; }
	public String getXChapter() { return xchapter; }
	public void setMsChapter(String s) { mschapter = s; }
	public String getMsChapter() { return mschapter; }
	public void setMvChapter(String s) { mvchapter = s; }
	public String getMvChapter() { return mvchapter; }

	public void setAspect(String s) { aspect = s; }
	public String getAspect() { return aspect; }
	public void setBvperf(String s) { bvperf = s; }
	public String getBvperf() { return bvperf; }
	public void setLvoice(String s) { lvoice = s; }
	public String getLvoice() { return lvoice; }
	public void setAutodel(String s) { autodel = s; }
	public String getAutodel() { return autodel; }
	public void setPortable(String s) { portable = s; }
	public String getPortable() { return portable; }

	public void setFolder(String s) { folder = s; }
	public String getFolder() { return folder; }
	
	public void setPursues(Boolean b) { pursues = b; }
	public Boolean getPursues() { return pursues; }
}
