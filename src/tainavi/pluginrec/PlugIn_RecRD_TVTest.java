package tainavi.pluginrec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.ChannelCode;
import tainavi.CommonUtils;
import tainavi.ContentIdEDCB;
import tainavi.HDDRecorder;
import tainavi.HDDRecorderUtils;
import tainavi.ReserveList;
import tainavi.TextValueSet;
import tainavi.HDDRecorder.RecType;

/**
 * TVTestで視聴するプラグイン
 */
public class PlugIn_RecRD_TVTest extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_TVTest clone() {
		return (PlugIn_RecRD_TVTest) super.clone();
	}
	
	/* 必須コード  - ここから */
	
	private static final String thisEncoding = "MS932";

	// 種族の特性
	public String getRecorderId() { return "TVTest"; }
	public RecType getType() { return RecType.TUNER; }
	
	@Override
	public String getChDatHelp() { return
			"TVTestプラグインの設定が正しければ、「レコーダの放送局名」は設定すべき値がプルダウンで選択できるはずです。\n"+
			"★TVTestプラグインはTVの視聴のみ可能です（右クリックメニューの「視聴する」コマンド）。";
	}
	
	// chvalueをつかってもいーよ
	@Override
	public boolean isChValueAvailable() { return true; }
	// CHコードは入力しなくていい
	@Override
	public boolean isChCodeNeeded() { return false; }

	// 個体の特性
	private ChannelCode cc = new ChannelCode(getRecorderId());
	private String errmsg = "";
	//private HashMap<String,String> ccMap;

	private final String MSGID = "["+getRecorderId()+"] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final String DBGID = "[DEBUG]"+MSGID;

	// コンストラクタ
	
	public PlugIn_RecRD_TVTest() {
		super();
		this.setUser("D:\\PT2\\TVTest");
	}
	
	// 公開メソッド

	/*
	 * 
	 */
	public ChannelCode getChCode() {
		return cc;
	}

	/*
	 * チャンネルリモコン機能
	 */
	public boolean ChangeChannel(String webChName) {
		
		if (webChName == null) {
			return true;
		}
		
		final String recChName = cc.getCH_WEB2REC(webChName);
		final String sysDir = getUser();
		final String exeFile = sysDir+File.separator+"TVTest.exe";
		
		String str = text2value(chtype,recChName);
		if (str == null || str.length() == 0) {
			String msg = ERRID+"指定のチャンネルをCHコードに変換できません："+webChName; 
			setErrmsg(msg);
			return false;
		}
		
		String[] da = str.split(",");
		String[] dd = da[1].split(":");
		
		try {
			ProcessBuilder pb = new ProcessBuilder(new String[] {exeFile, "/s", "/d", da[0], "/nid", dd[0], "/tsid", dd[1], "/sid", dd[2]});
			pb.start();
			return true;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		String msg = ERRID+"指定のチャンネルが見つかりません："+recChName; 
		setErrmsg(msg);
		return false;
	}

	/*
	 * レコーダーから予約一覧を取得する
	 */
	public boolean GetRdReserve(boolean force) {
		
		final String chTypeTFile	= String.format("%s%srs_%s.%s.xml", "env", File.separator, getRecorderId(), "chType");
		final String chValueTFile	= String.format("%s%srs_%s.%s.xml", "env", File.separator, getRecorderId(), "chValue");
		
		ArrayList<TextValueSet> cht = null;
		ArrayList<TextValueSet> chv = null;
		if ( new File(chTypeTFile).exists() && new File(chValueTFile).exists() ) {
			cht = TVSload(chTypeTFile);
			if ( cht != null ) {
				chv = TVSload(chValueTFile);
			}
		}
		
		boolean b = _GetRdReserve();
		if ( b ) {
			// 通常はからなずTVTestの設定を取得しなおすが
			TVSsave(chtype, chTypeTFile);
			TVSsave(chvalue, chValueTFile);
		}
		else if ( cht != null && chv != null ) {
			// とれなかったらキャッシュを使う
			chtype = cht;
			chvalue = chv;
		}

		if (getDebug()) System.out.println(DBGID+"有効な放送局の数： "+chtype.size()); 
			
		return b;
	}
	
	public boolean _GetRdReserve() {
		
		setErrmsg(MSGID+"予約一覧取得は無効です。");
		
		final String sysDir = getUser();
		final String iniFile = sysDir+File.separator+"TVTest.ini";
		
		File d = new File(sysDir);
		if ( ! d.exists() || ! d.isDirectory()) {
			String msg = ERRID+"指定のディレクトリがみつからないか、あるいはディレクトリではありません："+sysDir; 
			setErrmsg(msg);
			//System.out.println(msg);
			return(false);
		}
		
		String DriverDirectory = null;
		ArrayList<String> Drivers = new ArrayList<String>();
		String buf = CommonUtils.read4file(iniFile, false, "Unicode");
		if ( buf == null ) {
			String msg = ERRID+"設定ファイルが取得できませんでした："+iniFile; 
			setErrmsg(msg);
			//System.out.println(msg);
			return false;
		}
		
		String[] sa = buf.split("[\r\n]+");
		for ( String str : sa ) {
			Matcher ma = Pattern.compile("^DriverDirectory=(.+?)\\s*$",Pattern.DOTALL).matcher(str);
			if ( ma.find() ) {
				DriverDirectory = ma.group(1);
				if (getDebug()) System.out.println(DBGID+"ドライバーディレクトリの取得： "+ma.group(1)); 
				continue;
			}
			ma = Pattern.compile("^Driver=(.+?)\\.dll").matcher(str);
			if ( ma.find() ) {
				Drivers.add(ma.group(1));
				if (getDebug()) System.out.println(DBGID+"ドライバーの追加： "+ma.group(1)); 
				continue;
			}
			ma = Pattern.compile("^Driver\\d+_FileName=(.+?)\\.dll").matcher(str);
			if ( ma.find() ) {
				Drivers.add(ma.group(1));
				if (getDebug()) System.out.println(DBGID+"ドライバーの追加： "+ma.group(1)); 
				continue;
			}
		}
		
		for ( String Driver : Drivers ) {
			String ch2File = null;
			if ( Driver.contains(File.separator) ) {
				ch2File = Driver+".ch2";
			}
			else {
				ch2File = DriverDirectory+File.separator+Driver+".ch2";
			}
			File f = new File(ch2File);
			if ( ! f.exists() || ! f.isFile()) {
				String msg = ERRID+"チャンネル設定ファイルがみつかりません："+ch2File; 
				setErrmsg(msg);
				//System.out.println(msg);
				continue;
			}
			
			buf = CommonUtils.read4file(ch2File, false, thisEncoding);
			if ( buf == null ) {
				String msg = ERRID+"チャンネル設定ファイルが取得できませんでした："+ch2File; 
				setErrmsg(msg);
				//System.out.println(msg);
				return false;
			}
			
			sa = buf.split("[\r\n]+");
			for ( String str : sa ) {
				if ( str.startsWith(";") ) {
					continue;
				}
				
				String[] da = str.split(",");
				if (da.length < 4) {
					continue;
				}
				
				if ( da[7].equals("17168") || da[7].equals("17169") ) {
					da[0] = "臨)"+da[0];
				}
				
				String val = text2value(chvalue, da[0]);
				if ( val == null || val.length() == 0 ) {
					TextValueSet tn;

					String chid;
					try {
						chid = ContentIdEDCB.getChId(Integer.valueOf(da[6]), Integer.valueOf(da[7]), Integer.valueOf(da[5]));
					}
					catch (NumberFormatException e) {
						String msg = ERRID+"iniファイルの内容が不正です："+ch2File; 
						setErrmsg(msg);
						//System.out.println(msg);
						return false;
					}
					tn = new TextValueSet();
					tn.setText(da[0]);
					tn.setValue(String.valueOf(Long.decode("0x"+chid)));
					chvalue.add(tn);
					
					tn = new TextValueSet();
					tn.setText(da[0]);
					tn.setValue(Driver+".dll,"+da[6]+":"+da[7]+":"+da[5]);
					chtype.add(tn);
					
					if (getDebug()) System.out.println(DBGID+"設定の追加： "+tn.getText()+"="+tn.getValue()); 
				}
			}
		}

		return (true);
	}

	/*
	 * 予約を実行する
	 */
	public boolean PostRdEntry(ReserveList r) {
		setErrmsg(MSGID+"予約処理は無効です。");
		return (false);
	}

	/*
	 * 予約を更新する
	 */
	public boolean UpdateRdEntry(ReserveList o, ReserveList r) {
		setErrmsg(MSGID+"更新処理は無効です。");
		return (false);
	}

	/*
	 * 予約を削除する
	 */
	public ReserveList RemoveRdEntry(String delno) {
		setErrmsg(MSGID+"削除処理は無効です。");
		return (null);
	}

	/*
	 * 電源ＯＮ・ＯＦＦ
	 */
	public void wakeup() {
	}
	public void shutdown() {
	}

	/*
	 * 
	 */
	public String getErrmsg() {
		return (errmsg);
	}

	protected void setErrmsg(String s) {
		errmsg = s;
	}
}
