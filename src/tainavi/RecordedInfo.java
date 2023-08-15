package tainavi;

/**
 * <P>録画結果の情報を保持します。
 */
public class RecordedInfo {

	private String id;
	
	private String date="";
	private String ahh="";
	private String amm="";
	private String zhh="";
	private String zmm="";
	private String channel;
	private String ch_name;
	private String ch_orig;
	private String title;
	private String detail;
	private String result;

	private int length;
	private long size;
	private int drop;
	private int drop_mpeg;
	private float sig_a;
	private float sig_z;
	
	private boolean succeeded;

	public String getId() { return id; }
	public void setId(String s) { id=s; }
	
	public String getDate() { return date; }
	public void setDate(String s) { date=s; }
	public String getAhh() { return ahh; }
	public void setAhh(String s) { ahh=s; }
	public String getAmm() { return amm; }
	public void setAmm(String s) { amm=s; }
	public String getZhh() { return zhh; }
	public void setZhh(String s) { zhh=s; }
	public String getZmm() { return zmm; }
	public void setZmm(String s) { zmm=s; }
	public String getTitle() { return title; }
	public void setTitle(String s) { title=s; }
	public String getChannel() { return channel; }
	public void setChannel(String s) { channel=s; }
	public String getCh_name() { return ch_name; }
	public void setCh_name(String s) { ch_name=s; }
	public String getCh_orig() { return ch_orig; }
	public void setCh_orig(String s) { ch_orig=s; }
	public String getDetail() { return detail; }
	public void setDetail(String s) { detail=s; }
	public String getResult() { return result; }
	public void setResult(String s) { result=s; }
	
	public int getLength() { return length; }
	public void setLength(int n) { length=n; }
	public long getSize() { return size; }
	public void setSize(long l) { size=l; }
	public int getDrop() { return drop; }
	public void setDrop(int n) { drop=n; }
	public int getDrop_mpeg() { return drop_mpeg; }
	public void setDrop_mpeg(int n) { drop_mpeg=n; }
	public float getSig_a() { return sig_a; }
	public void setSig_a(float n) { sig_a=n; }
	public float getSig_z() { return sig_z; }
	public void setSig_z(float n) { sig_z=n; }
	
	public boolean getSucceeded() { return succeeded; }
	public void setSucceeded(boolean b) { succeeded = b; }
	
	
}
