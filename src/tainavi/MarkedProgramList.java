package tainavi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.TVProgram.ProgOption;
import tainavi.TVProgram.ProgType;

/**
 * 検索は都度行うのではなく、番組表を読み込んだり検索条件を変更したりといったイベントの際に全件作成してしまって、表示はそれを絞り込むだけにして高速化をはかる
 */
public class MarkedProgramList {
	
	/*
	 * 定数
	 */

	private static final String MSGID = "[検索結果生成] ";
	//private static final String ERRID = "[ERROR]"+MSGID;
	//private static final String DBGID = "[DEBUG]"+MSGID;
	
	/*
	 * 
	 */
	
	private ArrayList<ProgDetailList> programs = null;
	private ArrayList<ArrayList<TraceKey>> traceKeys = null;
	private ArrayList<ArrayList<Integer>> traceScores = null;
	private ArrayList<ArrayList<SearchKey>> searchKeys = null;
	private ArrayList<ArrayList<String>> searchStrs = null;

	public ProgDetailList getProg(int n) { return this.programs.get(n); }
	public ArrayList<TraceKey> getTKey(int n) { return this.traceKeys.get(n); }
	public ArrayList<Integer> getTScore(int n) { return this.traceScores.get(n); }
	public ArrayList<SearchKey> getSKey(int n) { return this.searchKeys.get(n); }
	public ArrayList<String> getSStr(int n) { return this.searchStrs.get(n); }

	public int size() { return (programs==null)?(0):(programs.size()); }

	private boolean disableFazzySearch = false;
	private boolean disableFazzySearchReverse = false;
	
	// 
	private boolean historyOnlyUpdateOnce = false;
	public void setHistoryOnlyUpdateOnce(boolean b) { historyOnlyUpdateOnce = b; }
	
	//
	private boolean showOnlyNonrepeated = true;
	public void setShowOnlyNonrepeated(boolean b) { showOnlyNonrepeated = b; }
	
	
	//
	
	public void build(ArrayList<TVProgram> progs, ArrayList<TraceKey> trKeys, ArrayList<SearchKey> srKeys) {
		// フラグを落とす
		clearMarkedFlag(progs);
		
		// 番組追跡
    	for (TraceKey trace : trKeys) {
    		buildByKeyword(progs, trace, null);
    	}
    	// キーワード検索
		for (SearchKey search : srKeys) {
    		buildByKeyword(progs, null, search);
    	}
		// 新着チェック
		chkNewArrival();
	}
	
	/**
	 * 検索条件がかわったなら全番組のフラグを初期化しないといけない
	 */
	private void clearMarkedFlag(ArrayList<TVProgram> tvprograms) {
		for ( TVProgram tvp : tvprograms ) {
			if (tvp.getType() != TVProgram.ProgType.PROG && tvp.getType() != TVProgram.ProgType.SYOBO) {
				continue;
			}
			
			for ( ProgList tvpl : tvp.getCenters() ) {
				if ( ! tvpl.enabled) {
    				continue;
    			}
				
				for ( ProgDateList tvc : tvpl.pdate ) {
					for ( ProgDetailList tvd : tvc.pdetail ) {
						tvd.marked = tvd.markedByTrace = false;
						tvd.nonrepeated = false;
						tvd.showinstandby = false;
					}
				}
			}
		}
	}
	
	private void buildByKeyword(ArrayList<TVProgram> tvprograms, TraceKey tKey, SearchKey sKey) {
		// 検索条件のマッチカウントのクリア
		SearchItem item = tKey != null ? tKey : sKey;
		item.clearMatchedList();

		//
		for (int siteid=0; siteid<tvprograms.size(); siteid++) {
			// ほげほげ
			TVProgram tvp = tvprograms.get(siteid);
			
			if (tvp.getType() != TVProgram.ProgType.PROG && tvp.getType() != TVProgram.ProgType.SYOBO) {
				continue;
			}
			
			for (int centerid=0; centerid<tvp.getCenters().size(); centerid++) {
				// ほげほげ
				ProgList tvpl = tvp.getCenters().get(centerid);
    			
				if ( ! tvpl.enabled) {
    				continue;
    			}
				
				if (tKey != null && ! tKey.getCenter().equals(tvpl.Center) ) {
					continue;
				}
				
				if ( tKey != null && tKey.getShowLatestOnly() ) {
					System.out.println(MSGID+"[リピート放送判定] リピート放送を排除する検索キー： *"+tvp.getType()+"* "+tKey._getLabel());
				}
				
				// 一時保存用
				MatchedBuffer mBuf = new MatchedBuffer();
    			
    			// キーワード検索用
    			String centerPop = TraceProgram.replacePop(tvpl.Center);
    			
				for (int dateid=0; dateid<tvpl.pdate.size(); dateid++) {
					String matchedString = null;
					ProgDateList tvc = tvpl.pdate.get(dateid);
					for (int progid=0; progid<tvc.pdetail.size(); progid++) {
						ProgDetailList tvd = tvc.pdetail.get(progid);
						
						// 番組情報がありませんは表示しない
						if (tvd.start.equals("")) {
							continue;
						}
						
    					//マッチング
						int fazScore = 0;
						boolean isFind = false;
						if (tKey != null) {
							if (tKey.getDisableRepeat() == true && tvd.isOptionEnabled(ProgOption.REPEAT)) {
								// 再放送を除く
								continue;
							}
							
							if (this.disableFazzySearch == true) {
								// 完全一致
								if (tKey._getTitlePop().equals(tvd.titlePop)) {
									isFind = true;
								}
							}
							else {
								//あいまい検索・正引き
								String target = ProgDetailList.tracenOnlyTitle ?  tvd.splitted_titlePop : tvd.titlePop;
								fazScore = TraceProgram.sumScore(target, tKey._getTitlePop());
								if (fazScore >= tKey.getFazzyThreshold()) {
									isFind = true;
								}
								else if ( ! this.disableFazzySearchReverse) {
									// 逆引き
									fazScore = TraceProgram.sumScore(tKey._getTitlePop(), target);
									if (fazScore >= tKey.getFazzyThreshold()) {
										isFind = true;
									}
								}
							}
						}
						else if (sKey != null) {
    						isFind = SearchProgram.isMatchKeyword(sKey, ((sKey.getCaseSensitive()==false)?(centerPop):(tvpl.Center)), tvd);
    						if ( isFind ) {
    							matchedString = SearchProgram.getMatchedString();
    						}
    					}
						
						if (isFind) {
							tvd.marked = true;
							tvd.markedByTrace = tvd.markedByTrace || tKey != null;
							mBuf.add(tvd, tKey, fazScore, sKey, matchedString);
						}
					}
				}
				for ( MatchedBufferData d : mBuf.getData() ) {
					if ( d.prog.marked ) {
						if ( tKey != null && tKey.getShowLatestOnly() ) {
							System.out.println(MSGID+"[リピート放送判定] [結果] リピート放送ではないと判断されました： "+d.prog.startDateTime+" 「"+d.prog.title+"("+d.bareTitle+")」 ("+d.storyNo+")");
							d.prog.nonrepeated = true;
						}
						if ( ! (sKey != null && ! sKey.getShowInStandby()) ) {
							d.prog.showinstandby = true;
						}
						this.add(d.prog, d.tKey, d.tScore, d.sKey, d.sStr);
					}
					else {
						if ( tKey != null && tKey.getShowLatestOnly() ) {
							if ( ! showOnlyNonrepeated ) {
								// 復活戦
								d.prog.marked = d.prog.markedByTrace = true;
								if ( sKey != null && sKey.getShowInStandby() ) {
									d.prog.showinstandby = true;
								}
								this.add(d.prog, d.tKey, d.tScore, d.sKey, d.sStr);
							}
						}
					}
				}
			}
		}
	}
	
	// 一時保存のためのサブクラス
	private class MatchedBufferData {
		public ProgDetailList prog = null;
		public TraceKey tKey = null;
		public int tScore = 0;
		public SearchKey sKey = null;
		public String sStr = null;
		//
		public String bareTitle = null;
		public Integer storyNo = null;
		
		public MatchedBufferData(ProgDetailList prog, TraceKey tKey, int tScore, SearchKey sKey, String sStr) {
			this.prog = prog;
			this.tKey = tKey;
			this.tScore = tScore;
			this.sKey = sKey;
			this.sStr = sStr;
			//
			this.bareTitle = null;
			this.storyNo = null;
		}
	}
	private class MatchedBuffer {
		//
		public ArrayList<MatchedBufferData> data = new ArrayList<MatchedBufferData>();
		public String xDateTime = null ;
		
		// コンストラクタ
		public MatchedBuffer() {
			GregorianCalendar c = CommonUtils.getCalendar(0);
			if ( CommonUtils.isLateNight(c) ) {
				c.add(Calendar.DATE, 6);
			}
			else {
				c.add(Calendar.DATE, 7);
			}
			c.set(Calendar.HOUR_OF_DAY, 5);
			c.set(Calendar.MINUTE, 0);
			xDateTime = CommonUtils.getDateTime(c); 
		}
		
		//
		public ArrayList<MatchedBufferData> getData() {
			return data;
		}
		
		private final String[] exprs = {
				// AT-XやANIMAXの場合
				"[#＃]([0-9０-９]+)",
				"第([0-9０-９]+)[話回]",
				// NHKの場合
				"[(（]([0-9０-９]+?)[)）]",
		};
		
		//
		public void add(ProgDetailList prog, TraceKey tKey, int tScore, SearchKey sKey, String sStr) {
			MatchedBufferData bd = new MatchedBufferData(prog, tKey, tScore, sKey, sStr);
			if ( tKey == null || ! tKey.getShowLatestOnly() ) {
				// キーワード検索だったりリピート有効な場合はそのまま
				data.add(bd);
				return;
			}
			// 話数をとりだす
			{
				// タイトルと番組詳細両方でしらべるもの
				for ( String expr : exprs ) {
					Matcher ma = Pattern.compile(expr).matcher(prog.title);
					if ( ma.find() ) {
						bd.storyNo = Integer.valueOf(CommonUtils.toHANUM(ma.group(1)));
						break;
					}
					ma = Pattern.compile("^[ 　\t]*"+expr).matcher(prog.detail);
					if ( ma.find() ) {
						bd.storyNo = Integer.valueOf(CommonUtils.toHANUM(ma.group(1)));
						break;
					}
				}
			}
			if ( bd.storyNo != null ) {
				
				// 半角数字にそろえりーな
				//bd.storyNo = CommonUtils.toHANUM(bd.storyNo);
				
				// 同一タイトルで重複するものを排除する
				
				// 話数を外した裸のタイトルだけを抽出する
				bd.bareTitle = TraceProgram.replacePop(prog.title.replaceFirst("\\s*([#＃][0-9０-９]+|[(（][0-9０-９]+[）)]|第[0-9０-９]+[話回]).*$", ""));
				
				for ( MatchedBufferData d : data ) {
					if ( d.prog.marked == false ) {
						// 既に無効化されていれば無視
						continue;
					}
					if ( prog.startDateTime.compareTo(xDateTime) < 0) {
						// ７日以内のデータ
						if ( d.bareTitle != null && d.bareTitle.equals(bd.bareTitle) ) {
							if ( d.storyNo != null && d.storyNo >= bd.storyNo ) {
								// 同じかより新しいものがすでにあったら自分を捨てる
								bd.prog.marked = bd.prog.markedByTrace = false;
								System.out.println(MSGID+"[リピート放送判定] [結果] リピート放送と判定されました(すでに新しいものがある)： "+bd.prog.startDateTime+" 「"+bd.prog.title+"("+bd.bareTitle+")」 ("+bd.storyNo+")");
							}
							else {
								// 自分より古いものは捨てる
								d.prog.marked = d.prog.markedByTrace = false;
								System.out.println(MSGID+"[リピート放送判定] [結果] リピート放送と判定されました(より新し番組がみつかった)： "+d.prog.startDateTime+" 「"+d.prog.title+"("+d.bareTitle+")」 ("+d.storyNo+")");
							}
						}
					}
					else {
						// ８日目以降のデータは特殊扱い
						if ( d.bareTitle != null && d.bareTitle.equals(bd.bareTitle) ) {
							if ( d.storyNo != null && d.storyNo >= bd.storyNo ) {
								// 同じかより新しいものがすでにあったら自分を捨てる
								bd.prog.marked = bd.prog.markedByTrace = false;
								System.out.println(MSGID+"[リピート放送判定] [結果] リピート放送と判定されました(すでに新しいものがある[8日目])： "+bd.prog.startDateTime+" 「"+bd.prog.title+"("+bd.bareTitle+")」 ("+bd.storyNo+")");
							}
							else {
								// 自分より古いものがあってもなにもしない
							}
						}
					}
				}
			}
			else {
				// 話数のついてないもの
				bd.bareTitle = TraceProgram.replacePop(prog.title);
				bd.storyNo = -1;
			}
			data.add(bd);
		}
	}
		
	private void add(ProgDetailList prog, TraceKey tKey, int tScore, SearchKey sKey, String sStr) {
		
		// 検索条件のマッチカントをカウントアップ
		SearchItem item = sKey != null ? sKey : tKey; 
		if (item != null && prog.type == ProgType.PROG) {
			item.addMatchedList(prog);
		}

		// 既存に
		
		for (int n=0; n<this.programs.size(); n++) {
			if (this.programs.get(n).equals(prog)) {
				if (tKey != null) {
					this.traceKeys.get(n).add(tKey);
					this.traceScores.get(n).add(tScore);
				}
				else if (sKey != null) {
					this.searchKeys.get(n).add(sKey);
					this.searchStrs.get(n).add(sStr);
				}
				return;
			}
		}
			
		// 新規追加

		// 開始時刻順にソート
		int p=0;
		for (; p<this.programs.size(); p++) {
			if (this.programs.get(p).startDateTime.compareTo(prog.startDateTime) > 0) {
				break;
			}
		}
		
		this.programs.add(p,prog);
		this.traceKeys.add(p, new ArrayList<TraceKey>());
		this.traceScores.add(p, new ArrayList<Integer>());
		this.searchKeys.add(p, new ArrayList<SearchKey>());
		this.searchStrs.add(p, new ArrayList<String>());
		if (tKey != null) {
			this.traceKeys.get(p).add(tKey);
			this.traceScores.get(p).add(tScore);
		}
		if (sKey != null) {
			this.searchKeys.get(p).add(sKey);
			this.searchStrs.get(p).add(sStr);
		}
	}
	
	// 
	public void clear(boolean b1, boolean b2) {
		//
		this.disableFazzySearch = b1;
		this.disableFazzySearchReverse = b2;
		
		//
		this.programs = new ArrayList<ProgDetailList>();
		
		this.traceKeys = new ArrayList<ArrayList<TraceKey>>();
		this.traceKeys.add(new ArrayList<TraceKey>());
		this.traceScores = new ArrayList<ArrayList<Integer>>();
		this.traceScores.add(new ArrayList<Integer>());
		
		this.searchKeys = new ArrayList<ArrayList<SearchKey>>();
		this.searchKeys.add(new ArrayList<SearchKey>());
		this.searchStrs = new ArrayList<ArrayList<String>>();
		this.searchStrs.add(new ArrayList<String>());
	}

	// 検索結果の履歴と突き合わせて新着をチェックする
	public void chkNewArrival() {
		
		MarkedHistoryList oldhist = MarkedHistoryList.load(historyOnlyUpdateOnce);
		MarkedHistoryList newhist = new MarkedHistoryList();
		
		int max = this.size();
		for ( int i=0; i<max; i++ ) {
			
			// 通常の番組情報のみ（しょぼかるは対象外）
			if ( this.getProg(i).type != ProgType.PROG ) {
				continue;
			}
			
			MarkedHistory mh = new MarkedHistory();
			mh.setCenter(this.getProg(i).center);
			mh.setStartDateTime(this.getProg(i).startDateTime);
			mh.setDetail(this.getProg(i).detail);
			
			// 番組追跡とキーワード検索のダブリを排除
			if ( newhist.isMatch(mh) ) {
				continue;
			}
			
			// 新着チェック（ ! isMatch()注意）
			this.getProg(i).newarrival = ! oldhist.isMatch(mh);
			this.getProg(i).modified = (this.getProg(i).newarrival)?(false):(oldhist.isModified(mh));
			
			// 履歴更新
			newhist.add(mh);
		}
		
		// 履歴を保存
		newhist.save(historyOnlyUpdateOnce);
	}
}
