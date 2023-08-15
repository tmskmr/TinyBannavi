package taiSync;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.RecorderList;
import tainavi.ReserveList;



public class ReserveInfo extends ReserveList {

	private boolean byTainavi = true;
	public void setByTainavi(boolean b) { byTainavi = b; }
	public boolean getByTainavi() { return byTainavi; }
	
	private String ipaddr = "";
	private String portno = "";
	public void setIpaddr(String s) { ipaddr = s; }
	public String getIpaddr() { return ipaddr; }
	public void setPortno(String s) { portno = s; }
	public String getPortno() { return portno; }
	
	private String ch_code = "";
	public void setCh_code(String s) { ch_code = s; }
	public String getCh_code() { return ch_code; }
	
	private ArrayList<String> starts = new ArrayList<String>();
	private ArrayList<String> ends = new ArrayList<String>();
	private ArrayList<String> recIds = new ArrayList<String>();
	public void setStarts(ArrayList<String> a) { starts = a; }
	public ArrayList<String> getStarts() { return starts; }
	public void setEnds(ArrayList<String> a) { ends = a; }
	public ArrayList<String> getEnds() { return ends; }
	public void setRecIds(ArrayList<String> a) { recIds = a; }
	public ArrayList<String> getRecIds() { return recIds; }
	
	private String version = "";
	public void setVersion(String s) { version = s; }
	public String getVersion() { return version; }
	
	// 鯛ナビのリクエストから生成
	public ReserveInfo(String pstr) {
		
		this.setId(null);
		
		Matcher ma = Pattern.compile("(.+?)=(.+?)(&|$)").matcher(pstr);
		while (ma.find()) {
			try {
				String key = ma.group(1);
				String val = URLDecoder.decode(ma.group(2), "UTF-8");
				
				if (key.equals("ver")) {
					this.setVersion(val);
				}
				else if (key.equals("id")) {
					this.setId(val);
				}
				else if (key.equals("exec")) {
					this.setExec(val.equals("ON"));
				}
				else if (key.equals("title")) {
					//System.out.println(ma.group(2)+ "->" +val);
					this.setTitle(val);
				}
				else if (key.equals("ch_code")) {
					this.setCh_code(val);
				}
				else if (key.equals("channel")) {
					this.setChannel(val);
				}
				else if (key.equals("pattern")) {
					this.setRec_pattern(val);
				}
				else if (key.equals("ahh")) {
					this.setAhh(val);
				}
				else if (key.equals("amm")) {
					this.setAmm(val);
				}
				else if (key.equals("zhh")) {
					this.setZhh(val);
				}
				else if (key.equals("zmm")) {
					this.setZmm(val);
				}
				else if (key.equals("tuner")) {
					this.setTuner(val);
				}
				else if (key.equals("video")) {
					this.setRec_mode(val);
				}
				else if (key.equals("audio")) {
					this.setRec_audio(val);
				}
				else if (key.equals("disc")) {
					this.setRec_device(val);
				}
				else if (key.equals("dvdr")) {
					this.setRec_dvdcompat(val);
				}
				else if (key.equals("xchapter")) {
					this.setRec_xchapter(val);
				}
				else if (key.equals("mvchapter")) {
					this.setRec_mvchapter(val);
				}
				else if (key.equals("mschapter")) {
					this.setRec_mschapter(val);
				}
				else if (key.equals("auto_delete")) {
					this.setRec_autodel(val);
				}
				else if (key.equals("lVoice")) {
					this.setRec_lvoice(val);
				}
				else if (key.equals("edge_left")) {
					this.setRec_aspect(val);
				}
				else if (key.equals("autocomp")) {
					this.setAutocomplete(val.equals("ON"));
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (this.getRec_mode() != null && (this.getRec_mode().startsWith("[TS") || this.getRec_mode().startsWith("[DR]") || this.getRec_mode().startsWith("[AVC] "))) {
			this.setRec_audio("  ");
		}
	}
	
	// 親クラスからコピー
	public ReserveInfo(ReserveList o) {
		
		//this.setId(o.getId());	// 独自のIDをふる
		this.setRec_pattern(o.getRec_pattern());
		this.setRec_pattern_id(o.getRec_pattern_id());
		this.setRec_nextdate(o.getRec_nextdate());
		this.setAhh(o.getAhh());
		this.setAmm(o.getAmm());
		this.setZhh(o.getZhh());
		this.setZmm(o.getZmm());
		this.setRec_min(o.getRec_min());
		this.setTuner(o.getTuner());
		this.setRec_mode(o.getRec_mode());
		this.setTitle(o.getTitle());
		//this.setTitlePop(o.getTitlePop());
		this.setChannel(o.getChannel());
		//this.setCh_name(o.getCh_name());

		//this.setDetail(o.getDetail());

		this.setRec_audio(o.getRec_audio());
		//this.setRec_folder(o.getRec_folder());
		//this.setRec_genre(o.getRec_genre());
		//this.setRec_dvdcompat(o.getRec_dvdcompat());
		this.setRec_device(o.getRec_device());
		
		this.setRec_xchapter(o.getRec_xchapter());
		this.setRec_mvchapter(o.getRec_mvchapter());
		this.setRec_mschapter(o.getRec_mschapter());

		this.setRec_autodel(o.getRec_autodel());
		this.setRec_lvoice(o.getRec_lvoice());
		this.setRec_aspect(o.getRec_aspect());

		this.setStartDateTime(o.getStartDateTime());
		this.setEndDateTime(o.getEndDateTime());
		
		this.setExec(o.getExec());
		this.setPursues(o.getPursues());
		this.setAutocomplete(o.getAutocomplete());
		
		// 独立項目
		this.starts.add(o.getStartDateTime());
		this.ends.add(o.getEndDateTime());
		this.recIds.add(o.getId());
	}

	public ReserveInfo() {
	}
	
	
	// 情報のファイルの読み込みと書き出し
	private static String saveFile = "env/taiSyncRsvInfo.xml";
	
	public static ArrayList<ReserveInfo> load() {
		File f = new File(saveFile);
		if (f.exists()) {
            XMLDecoder dec;
			try {
				dec = new XMLDecoder(new BufferedInputStream(new FileInputStream(saveFile)));
	            ArrayList<ReserveInfo> a = (ArrayList<ReserveInfo>)dec.readObject();
	            dec.close();
		        return(a);
	        } catch(Exception e) {
	        	System.out.println("Exception: load recorder="+saveFile+"("+e.toString()+")");
	        }
		}
        return(new ArrayList<ReserveInfo>());
	}
	
	public static void save(ArrayList<ReserveInfo> a) {
        try {
            XMLEncoder enc = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(saveFile)));
            enc.writeObject(a);
            enc.close();
        } catch(FileNotFoundException e) {
        	System.out.println("Exception: save recorder="+saveFile);
        }
	}
	
	public static void showReserveInfo(ArrayList<ReserveInfo> a, String ipaddr) {
		// ソートする
		ArrayList<ReserveInfo> b = new ArrayList<ReserveInfo>();
		for (int i=0; i<a.size(); i++) {
			boolean f = true;
			// 自分の分だけ
			if (ipaddr != null && ! a.get(i).ipaddr.equals(ipaddr)) {
				continue;
			}
			for (int j=0; j<b.size(); j++) {
				if (b.get(j).getStartDateTime().compareTo(a.get(i).getStartDateTime()) > 0) {
					b.add(j, a.get(i));
					f = false;
					break;
				}
			}
			if (f) {
				b.add(a.get(i));
			}
		}
		
		// 出力する
		System.err.println("---Reserved List Start---");
		for ( int i = 0; i<b.size(); i++ ) {
			// 詳細情報の取得
			ReserveInfo e = b.get(i);
			
			int wlen = 0;
			String ptn = e.getRec_pattern();
			Matcher ma = Pattern.compile("[^ -~｡-ﾟ]").matcher(ptn);
			while (ma.find()) {
				wlen++;
			}
			int ptnlen = 14-wlen;
			ptn = String.format(String.format("%%-%ds",ptnlen),ptn);
			
			System.err.println(String.format("[%3d] <%s> (%3s) (%2s)%s    %s-%s %sm    %-12s    %-8s    %s",
					(i+1), (e.getByTainavi())?("鯛"):("本"), e.getId(), e.getRec_pattern_id(), ptn, e.getStartDateTime(), e.getEndDateTime(), e.getRec_min(), e.getRec_mode(), e.getChannel(), e.getTitle()));
			for (int j=0; j<e.getStarts().size(); j++) {
				System.err.println(String.format("                                +(%3s) %s-%s",
						e.getRecIds().get(j), e.getStarts().get(j), e.getEnds().get(j)));
				
			}
		}
		System.err.println("---Reserved List End---");
	}
}
