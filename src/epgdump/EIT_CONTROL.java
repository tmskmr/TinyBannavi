package epgdump;

import java.util.ArrayList;

public class EIT_CONTROL {

	int		table_id ;
	int		service_id ;
	int		event_id ;			// イベントID
	
    int		yy;
    int		mm;
    int		dd;
    int		hh;
    int		hm;
	int		ss;
	int		dhh;
	int		dhm;
	int		dss;
	int		ehh;
	int		ehm;
	int		ess;
	
	String	title ;			// タイトル
	String	subtitle ;		// サブタイトル
	String	detail ;		// 番組詳細
	String	performer ;		// 出演者
	
	ArrayList<String> content_type = new ArrayList<String>() ;		// ジャンル情報
	
	/* 個々にもたせる必要はない。メモリを無駄に消費するだけだった。
	// テンポラリ
	byte[] d_tmp = new byte[Util.MAXSECLEN];
	int d_tmp_len = -1;
	byte[] p_tmp = new byte[Util.MAXSECLEN];
	int p_tmp_len = -1;
	String dDesc;
	String pDesc;
	*/
	
	// XMLEncoder/Decoder用
	public void setTable_id(int d) { table_id = d; }
	public int getTable_id() { return table_id; }
	public void setService_id(int d) { service_id = d; }
	public int getService_id() { return service_id; }
	public void setEvent_id(int d) { event_id = d; }
	public int getEvent_id() { return event_id; }
	
	public void setYy(int d) { yy = d; }
	public int getYy() { return yy; }
	public void setMm(int d) { mm = d; }
	public int getMm() { return mm; }
	public void setDd(int d) { dd = d; }
	public int getDd() { return dd; }
	
	public void setHh(int d) { hh = d; }
	public int getHh() { return hh; }
	public void setHm(int d) { hm = d; }
	public int getHm() { return hm; }
	public void setSs(int d) { ss = d; }
	public int getSs() { return ss; }

	public void setDhh(int d) { dhh = d; }
	public int getDhh() { return dhh; }
	public void setDhm(int d) { dhm = d; }
	public int getDhm() { return dhm; }
	public void setDss(int d) { dss = d; }
	public int getDss() { return dss; }

	public void setEhh(int d) { ehh = d; }
	public int getEhh() { return ehh; }
	public void setEhm(int d) { ehm = d; }
	public int getEhm() { return ehm; }
	public void setEss(int d) { ess = d; }
	public int getEss() { return ess; }

	public void setTitle(String s) { title = s; }
	public String getTitle() { return title; }
	public void setSubtitle(String s) { subtitle = s; }
	public String getSubtitle() { return subtitle; }
	public void setDetail(String s) { detail = s; }
	public String getDetail() { return detail; }
	public void setPerformer(String s) { performer = s; }
	public String getPerformer() { return performer; }
	
	public void setContent_type(ArrayList<String> a) { content_type = a; }
	public ArrayList<String> getContent_type() { return content_type; }
}
