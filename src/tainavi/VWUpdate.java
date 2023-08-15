package tainavi;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JDialog;
import javax.swing.JOptionPane;


public class VWUpdate {

	private final String thisEncoding = "UTF-8";
	
	public enum UpdateResult { DONE, FAIL, PASS, NOUPDATE };
	
	private static final String site =  "http://sourceforge.jp";
	private static final String updDir = "update";
	
	private static final TVProgramUtils utils = new TVProgramUtils();
	
	private StatusWindow stwin = null;
	
	private static UpdateResult isDone = UpdateResult.NOUPDATE;
	
	private static final String MSG_DONEUPDATE = "更新が完了しました。再起動してください。"+
			"\n\n★バックアップはbin.old、env.oldにあります。"+
			"\n\n☆再起動に失敗するとの報告がよせられています。"+
			"\n☆調査にご協力いただける方、事象発生時の"+
			"\n☆log.txtを提供いただけると助かります。";
	
	private static final String MSGID = "[アップデート] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;
	
	/*
	 * 公開メソッド
	 */
	
	public VWUpdate(StatusWindow sw) {
		stwin = sw;
	}
	
	public UpdateResult checkUpdate(final String versioninfo)	{
		
		new SwingBackgroundWorker(true) {
			
			@Override
			protected Object doWorks() throws Exception {
				checkUpdateSub(versioninfo);
				return null;
			}
			
			@Override
			protected void doFinally() {
			}
		}.execute();
		
		return isDone;
	}
	
	
	
	/*
	 * 非公開メソッド 
	 */
	
	// うーん… 
	private void StWinAppend(String message) {
		if (stwin!=null) stwin.append(message);
	}
	private void StWinAppendMessage(String message) {
		if (stwin!=null) stwin.append(message);
		System.out.println(message);
	}
	private void StWinAppendError(String message) {
		if (stwin!=null) stwin.append(message);
		System.err.println(message);
	}
	
	
	//
	private void checkUpdateSub(String versioninfo) {
		
		boolean isFull = new File("jre").exists() || new File("jre6").exists();
		
		Matcher ma = null;
		
		// 自分のバージョンを確認する
		String currentver = "";
		ma = Pattern.compile("([0-9]+)\\.([0-9]+)(\\.([0-9]+))?(.)?").matcher(versioninfo);
		if (ma.find()) {
			if (ma.group(4) == null) {
				currentver = String.format("%03d_%03d", Integer.valueOf(ma.group(1)), Integer.valueOf(ma.group(2)));
			}
			else {
				String ab = "b";
				if (ma.group(5) != null && ma.group(5).equals("α")) {
					ab = "a";
				}
				currentver = String.format("%03d_%03d_%03d%s", Integer.valueOf(ma.group(1)), Integer.valueOf(ma.group(2)), Integer.valueOf(ma.group(4)),ab);
			}
		}
		StWinAppendMessage("＝＝＝　アップデートの確認を行います　＝＝＝");
		StWinAppendMessage("お使いのバージョンは "+currentver+" です.");
		
		String latestlink = "";
		String latestfile = "";
		String latestver = "";
		String updatelink = "";
		String updatefile = "";
		String updatever = "";
		
		// Web上の最新バージョンを確認する
		String response = utils.webToBuffer(site+"/projects/tainavi/releases/",thisEncoding,false);
		if ( response != null ) {
			// 最新版
			ma = Pattern.compile("href=\"(/projects/tainavi/downloads/\\d+/(TinyBannavi.?(\\d+)_(\\d+)_"+((isFull)?("with"):("no"))+"_JRE\\.zip)/)").matcher(response);
			if (ma.find()) {
				latestlink = site+ma.group(1);
				latestfile = ma.group(2);
				latestver = String.format("%03d_%03d", Integer.valueOf(ma.group(3)), Integer.valueOf(ma.group(4)));
			}
			StWinAppendMessage("最新のバージョンは "+latestver+" です.");
			
			// アップデート差分
			ma = Pattern.compile("href=\"(/projects/tainavi/downloads/\\d+/(TinyBannavi.?(\\d+)_(\\d+)_(\\d+)b\\.zip)/)").matcher(response);
			if (ma.find()) {
				updatelink = site+ma.group(1);
				updatefile = ma.group(2);
				updatever = String.format("%03d_%03d_%03db", Integer.valueOf(ma.group(3)), Integer.valueOf(ma.group(4)), Integer.valueOf(ma.group(5)));
			}
			StWinAppendMessage("最新のアップデートは "+updatever+" です.");
		}
		
		if (response == null || (latestver.length() == 0 && updatever.length() == 0)) {
			// サイト構成が変わったかな？
			StWinAppend("");
			
			String msg = "【警告】アップデート情報を取得できませんでした。プロジェクトサイト(http://sourceforge.jp/projects/tainavi/)を確認してください。";
			StWinAppendError(msg);
			
			StWinAppend("");
			
			msg = "★★★★　起動時のアップデート確認が不要な場合は各種設定タブで無効にしてください。　★★★";
			StWinAppendMessage(msg);
			
			isDone = UpdateResult.PASS;
			
			CommonUtils.milSleep(5000);
		}
		else {
			// 更新対象を確認する
			if (latestver.compareTo(currentver) > 0) {
				{
					String msg = latestver+" にバージョンアップします.";
					StWinAppendMessage(msg);
				}
				if ( isFull ) {
					StWinAppend("");
					
					String msg = "★★★JRE同梱版はサイズが大きいためダウンロードに時間がかかります★★★";
					StWinAppendMessage(msg);
				}
				
				if (confirmUpdate(latestfile)) {
					// メジャーorマイナーバージョンが更新された場合
					cleanup(updDir);
					if (doUpdate(latestlink,latestfile)) {
						isDone = UpdateResult.DONE;
						showMessage(MSG_DONEUPDATE);
					}
					else {
						isDone = UpdateResult.FAIL;
						showMessage("バージョンアップが失敗しました.");
					}
				}
				else {
					isDone = UpdateResult.PASS;
					StWinAppendMessage("バージョンアップはキャンセルされました.");
				}
			}
			else if (updatever.length()>0 && updatever.compareTo(currentver) > 0) {
				String msg = updatever+" にアップデートします.";
				StWinAppendMessage(msg);
				
				if (confirmUpdate(updatefile)) {
					// リビジョンが更新された場合
					cleanup(updDir);
					if (doUpdate(updatelink,updatefile)) {
						isDone = UpdateResult.DONE;
						showMessage(MSG_DONEUPDATE);
					}
					else {
						isDone = UpdateResult.FAIL;
						showMessage("アップデートが失敗しました.");
					}
				}
				else {
					isDone = UpdateResult.PASS;
					StWinAppendMessage("アップデートはキャンセルされました.");
				}
			}
			else {
				isDone = UpdateResult.NOUPDATE;
				String msg = "お使いのタイニー番組ナビゲータは最新です.";
				StWinAppendMessage(msg);
				
				CommonUtils.milSleep(1000);
			}
		}
		StWinAppendMessage("＝＝＝　アップデートを確認しました　＝＝＝");
	}
	
	// 掃除する
	private boolean cleanup(String dir) {
		File d = new File(dir);
		if (d.exists() && ! CommonUtils.rmdir(d)) {
			return false;
		}
		return d.mkdir();
	}
	
	// 更新しますか
	private boolean confirmUpdate(String newfile) {
		String msg = "タイニー番組ナビゲータを("+newfile+")に更新しますか？";
		
		// JOptionPaneにsetAlwaysOnTopを設定するのは大変
		JOptionPane jOptPane = new JOptionPane();
		jOptPane.setMessage(msg);
		jOptPane.setOptionType(JOptionPane.YES_NO_OPTION);
		JDialog jdialog = jOptPane.createDialog("更新");
		jdialog.setAlwaysOnTop(true);
		jdialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		jdialog.setVisible(true);
		jdialog.dispose();
		
		int ret = (Integer) jOptPane.getValue();
		if (ret == JOptionPane.YES_OPTION) {
			return true;
		}
		return false;
	}
	
	// 再起動してください
	private boolean showMessage(String msg) {
		// JOptionPaneにsetAlwaysOnTopを設定するのは大変
		JOptionPane jOptPane = new JOptionPane();
		jOptPane.setMessage(msg);
		jOptPane.setOptionType(JOptionPane.PLAIN_MESSAGE);
		JDialog jdialog = jOptPane.createDialog("メッセージ");
		jdialog.setAlwaysOnTop(true);
		jdialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		jdialog.setVisible(true);
		jdialog.dispose();
		
		return false;
	}
	
	// 更新を実行する
	private boolean doUpdate(String releaselink, String releasefile) {
		//
		String releasepath = updDir+File.separator+releasefile;
		
		// リリースファイルを取得する
		if ( ! download(releaselink,releasepath)) {
			String msg = "リリースファイルのダウンロードに失敗しました.";
			StWinAppendError(msg);
			
			return false;
		}
		
		// リリースファイルを展開する
		try {
			String msg = "リリースファイルを解凍します.";
			StWinAppendMessage(msg);
			unzip.doUnzip(updDir,releasepath);
		}
		catch (IOException e) {
			String msg = "zipファイルの解凍に失敗しました: "+e;
			StWinAppendError(msg);
			
			return false;
		}
		catch (Exception e) {
			String msg = "zipファイルの解凍に失敗しました: "+e;
			StWinAppendError(msg);
			
			return false;
		}
		
		// アップデートを実施する
		try {
			String msg = "アップデートを実行します.";
			StWinAppendMessage(msg);
			
			int exitvalue;
			if (CommonUtils.isWindows()) {
				exitvalue = CommonUtils.executeCommand("cmd /c call "+updDir+"\\TinyBannavi\\_update.cmd");
			}
			else {
				exitvalue = CommonUtils.executeCommand("sh "+updDir+"/TinyBannavi/_update.sh");
			}
			msg = "アップデートスクリプトの終了値: "+exitvalue;
			StWinAppendMessage(msg);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	// リリースファイルを取得する
	private boolean download(String releaselink, String fname) {
		
		// リリースページを取得する
		String response = utils.webToBuffer(releaselink,thisEncoding,false);
		
		// リリースファイルのURLを取得する
		String flink = "";
		Matcher ma = Pattern.compile("<meta http-equiv=\"refresh\" content=\"\\d+; url=(.+?\\.zip)\">").matcher(response);
		if ( ! ma.find()) {
			String msg = "リリースファイルの情報が取得できませんでした。プロジェクトサイト(http://sourceforge.jp/projects/tainavi/)を確認してください。";
			StWinAppendError(msg);
			return false;
		}
		
		flink = CommonUtils.unEscape(site+ma.group(1));
		
		//
		String msg = "リリースファイルをダウンロードします: "+flink;
		StWinAppendMessage(msg);
		
		// リリースファイルを取得する
		utils.webToFile(flink, fname, null);
		
		return true;
	}
	
	
	
	/*
	 * アップデート履歴
	 */
	
	private static final String histfile = "env"+File.separator+"updatehistory.txt";
	
	public boolean isExpired(Env.UpdateOn u) {
		if (u == Env.UpdateOn.DISABLE) {
			// アップデートしてはいけない
			StWinAppendMessage(MSGID+"アップデートは無効です.");
			return false;
		}
		File hf = new File(histfile);
		if ( ! hf.exists() ) {
			// 履歴がないならアップデートしてください
			StWinAppendMessage(MSGID+"プロジェクトポータルに確認を行います（アップデート履歴ファイルは存在しません）");
			return true;
		}
		
		// 前はいつ更新したっけなー
		String lastdt = CommonUtils.read4file(histfile, true);
		if ( lastdt == null ) {
			// 履歴がおかしいんでアップデートしちゃってください
			StWinAppendError(ERRID+"アップデート履歴ファイルが読み込めません:"+histfile);
			return true;
		}
		GregorianCalendar cala = CommonUtils.getCalendar(lastdt);
		if ( cala == null ) {
			// 履歴がおかしいんでアップデートしちゃってください
			StWinAppendError(ERRID+"アップデート履歴ファイルが読み込めません:"+histfile);
			return true;
		}
		lastdt = CommonUtils.getDate(cala,true);

		// 今日は何の日？
		GregorianCalendar calb = CommonUtils.getCalendar(0);
		String curdt = CommonUtils.getDate(calb,true);
		
		// この日までには更新したいなー
		cala.add(Calendar.DATE, u.getInterval());
		String nextdt = CommonUtils.getDate(cala,true);
		
		StWinAppendError(String.format(MSGID+"アップデート履歴のチェック： 最終確認日=%s 次回確認予定日=%s 本日=%s",lastdt,nextdt,curdt));
		
		if (cala.compareTo(calb) >= 0) {
			StWinAppendMessage(MSGID+"確認は保留されました.");
			return false;
		}
		
		StWinAppendMessage(MSGID+"プロジェクトポータルに確認を行います（保留期限切れ）");
		return true;
	}
	public boolean updateHistory() {
		if ( ! CommonUtils.write2file(histfile, CommonUtils.getDate529(0,false)) ) {
			StWinAppendError(ERRID+"アップデート履歴ファイルが書き込めません:"+histfile);
			return false;
		}
		
		return true;
	}
}
