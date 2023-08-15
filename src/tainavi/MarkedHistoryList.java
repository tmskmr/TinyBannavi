package tainavi;

import java.io.File;
import java.util.ArrayList;

/**
 * 予約待機の履歴を保存して、前日と当日でどの番組が新しくリストアップされるようになったか判断する
 */
public class MarkedHistoryList {
	
	/*
	 * 定数
	 */

	private static final String MSGID = "[予約待機履歴] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;
	
	// メンバー
	private String saveDateTime = "";
	private ArrayList<MarkedHistory> hists = new ArrayList<MarkedHistory>();
	
	public int size() { return hists.size(); }
	
	public String getSaveDateTime() { return saveDateTime; }
	public void setSaveDateTime(String s) { saveDateTime = s; }
	// get/setHists()と書くはずがなぜかget/setCenters()になってしまった…
	public ArrayList<MarkedHistory> getCenters() { return hists; }
	public void setCenters(ArrayList<MarkedHistory> a) { hists = a; }
	
	// 比較
	public boolean isMatch(MarkedHistory mh) {
		for ( MarkedHistory mho : hists ) {
			if ( mho.isMatch(mh) ) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isModified(MarkedHistory mh) {
		for ( MarkedHistory mho : hists ) {
			if ( mho.isMatch(mh) ) {
				return mho.isModified(mh);
			}
		}
		return false;
	}
	
	// 追加
	public void add(MarkedHistory mh) {
		hists.add(mh);
	}
	
	// 保存先
	private static final String hFile = "env"+File.separator+"makredhistory.xml";
	private static final String hFileTmp = "env"+File.separator+"makredhistory.xml.tmp";
	
	// セーブ・ロード
	public void save(boolean historyOnlyUpdateOnce) {
		System.out.println(MSGID+"履歴を保存します: "+hFile);
		setSaveDateTime(CommonUtils.getDateTime(0));
		String fname = null;
		if (historyOnlyUpdateOnce) {
			// 一時ファイルに保存する
			fname = hFileTmp;
		}
		else {
			// 直接保存する
			fname = hFile;
		}
		if ( ! CommonUtils.writeXML(fname, this) ) {
			System.out.println(ERRID+"履歴の保存に失敗しました： "+fname);
		}
	}
	public static MarkedHistoryList load(boolean historyOnlyUpdateOnce) {
		System.out.println(MSGID+"履歴を読み込みます: "+hFile);

		MarkedHistoryList b = null;
		if ( historyOnlyUpdateOnce ) {
			// 日に一回しか確認しないよ
			File ft = new File(hFileTmp);
			if ( ft.exists() ) {
				b = (MarkedHistoryList) CommonUtils.readXML(hFileTmp);
				if ( b == null ) {
					System.err.println(ERRID+"履歴を読み込めませんでした： "+hFileTmp);
				}
				else {
					if ( b.saveDateTime.compareTo(CommonUtils.getCritDateTime()) < 0 ) {
						// テンポラリ履歴ファイルがすでに過去日のものなら利用する
						System.out.println(MSGID+"日付が変わっているので前回作成した履歴ファイルに利用を切り替えます: "+b.saveDateTime);
						File f = new File(hFile);
						if ( f.exists() ) {
							f.delete();
						}
						ft.renameTo(f);
						
						return b;
					}
				}
			}
		}
		
		// 通常の履歴ファイルを利用する
		if ( new File(hFile).exists() ) {
			b = (MarkedHistoryList) CommonUtils.readXML(hFile);
			if ( b == null ) {
				System.err.println(ERRID+"履歴を読み込めませんでした： "+hFile);
			}
		}
		if ( b == null ) {
			b = new MarkedHistoryList();
		}
		
		return b;
	}
}
