package tainavi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <P>個々のタイトルの情報を保持します。
 */
public class TitleInfo implements Cloneable {
	public static final String MOVEONLY = "移動のみ";
	public static final String RECORDING = "録画中";

	private int serial=0;
	private String id=null;
	private String rec_mode="";
	private String rec_date="";
	private String ahh="";
	private String amm="";
	private String zhh="";
	private String zmm="";
	private String rec_min="";
	private String title="";
	private String channel="";
	private String ch_name="";

	private String content_id="";

	private String rec_device=null;
	private ArrayList<TextValueSet> rec_folder=null;
	private ArrayList<TextValueSet> rec_genre=null;
	private String rec_subgenre="";

	private String startDateTime="";
	private String endDateTime="";

	private boolean recording=false;

	private ArrayList<ChapterInfo> chapter=null;

	private boolean detail_loaded;

	private HashMap<String, String> hidden_params = new HashMap<String, String>();

	//
	@Override
	public TitleInfo clone() {
		try {
			TitleInfo p = (TitleInfo) super.clone();
			p.rec_device = (this.rec_device!=null)?(this.rec_device):("");
			return p;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}

	/*
	 * 外部インターフェース
	 */
	public int getSerial(){ return serial; }
	public void setSerial(int n){ serial = n; }
	public String getId() {return id;}
	public void setId(String s) { id=s;}
	public String getRec_date() {return rec_date;}
	public void setRec_date(String s) { rec_date=s;}
	public String getAhh() {return ahh;}
	public void setAhh(String s) { ahh=s;}
	public String getAmm() {return amm;}
	public void setAmm(String s) { amm=s;}
	public String getZhh() {return zhh;}
	public void setZhh(String s) { zhh=s;}
	public String getZmm() {return zmm;}
	public void setZmm(String s) { zmm=s;}
	public String getRec_min() {return rec_min;}
	public void setRec_min(String s) { rec_min=s;}

	public String getRec_mode() {return rec_mode;}
	public void setRec_mode(String s) { rec_mode=s;}
	public String getTitle() {return title;}
	public void setTitle(String s) { title=s;}
	public String getChannel() {return channel;}
	public void setChannel(String s) { channel=s;}
	public String getCh_name() {return ch_name;}
	public void setCh_name(String s) { ch_name=s;}

	public void setDetail(String s) {}

	public String getContentId() {return content_id;}
	public void setContentId(String s) { content_id=s;}

	public String getRec_device() {return rec_device;}
	public void setRec_device(String s) { rec_device=s;}
	public ArrayList<TextValueSet> getRec_folder(){ return rec_folder; }
	public void setRec_folder(ArrayList<TextValueSet> tvs){ rec_folder=tvs; }
	public boolean containsFolder(String id){
		for ( TextValueSet tvs : rec_folder ){
			if (id.equals(tvs.getValue()))
				return(true);
		}

		return(false);
	}

	public String getFolderNameList(){
		String list = "";

		for ( TextValueSet tvs : rec_folder ){
			if (list.length() > 0){
				list += ",";

				Matcher ma = Pattern.compile("\\[[^\\[]+\\](.*)$").matcher(tvs.getText());
				if ( ma.find() ) {
					list += ma.group(1);
				}
				else{
					list += tvs.getText();
				}
			}
			else{
				list = tvs.getText();
			}
		}

		return list;
	}

	public ArrayList<TextValueSet> getRec_genre(){ return rec_genre; }
	public void setRec_genre(ArrayList<TextValueSet> tvs){ rec_genre=tvs; }
	public boolean containsGenre(String id){
		for ( TextValueSet tvs : rec_genre ){
			if (id.equals(tvs.getValue()))
				return(true);
		}

		return(false);
	}

	public String getGenreNameList(){
		String list = "";

		for ( TextValueSet tvs : rec_genre ){
			if (list.length() > 0)
				list += ",";
			list += tvs.getValue() + ":" + tvs.getText();
		}

		return list;
	}

	public String getStartDateTime() {return startDateTime;}
	public void setStartDateTime(String s) { startDateTime=s;}
	public String getEndDateTime() {return endDateTime;}
	public void setEndDateTime(String s) { endDateTime=s;}

	public ArrayList<ChapterInfo> getChapter(){ return chapter; }
	public void setChapter(ArrayList<ChapterInfo> ci){ chapter = ci; }

	public boolean getDetailLoaded(){ return detail_loaded; }
	public void setDetailLoaded(boolean b){ detail_loaded = b; }

	public boolean getRecording(){ return recording; }
	public void setRecording(boolean b){ recording = b; }

	public HashMap<String,String> getHidden_params() { return hidden_params; }
	public void setHidden_params(HashMap<String,String> a) { hidden_params = a; }

	// コピー回数を整形する
	public String formatCopyCount(){
		String copycount = getHidden_params().get("copycount");
		if (copycount == null)
			return "";
		else if (copycount.equals("1"))
			return MOVEONLY;
		else if (copycount.equals("238"))
			return RECORDING;
		else{
			try{
				int count = Integer.parseInt(copycount);
				return String.format("%d回", count-1);
			}
			catch(NumberFormatException e){
			}

			return copycount + "回";
		}
	}

	// 詳細情報を整形する
	public String formatDetail(){
		String detail =
				"番組タイトル：" + getTitle() + "\n" +
				"録画モード：" + getRec_mode() + "\n" +
				"フォルダ：" + getFolderNameList() + "\n";

		String g = "";
		for (TextValueSet ts : getRec_genre()){
			if (!g.equals(""))
				g += ",";
			g += ts.getValue() + ":" + ts.getText();
		}

		detail += "ジャンル：" + g + "\n";
		detail += "コピー回数：" + formatCopyCount() + "\n";
//		detail += "CONTENT ID：" + getContentId() + "\n";

		String dlnaObjectID = getHidden_params().get("dlnaObjectID");
		if (dlnaObjectID != null)
			detail += "DLNA OID：" + dlnaObjectID + "\n";

		return detail;
	}
}
