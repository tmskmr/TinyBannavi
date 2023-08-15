package tainavi;

import java.util.ArrayList;

import tainavi.HDDRecorder.RecType;


/**
 * {@link HDDRecorder} のリストを実現するクラスです.
 * @version 3.15.4β～
 */
public class HDDRecorderList extends ArrayList<HDDRecorder> {

	private static final long serialVersionUID = 1L;

	// ↓ 自己フィールド生成を行うor自己フィールド生成によるスタックオーバーフローを回避する(static修飾)ための異常なコード（自戒のためにコメントとして残す）
	//private static final HDDRecorderList mylist = new HDDRecorderList();

	// レコーダIDから種類を調べる
	public RecType getRecId2Type(String recId) {
		ArrayList<HDDRecorder> rl = this.findPlugin(recId);
		if ( rl.size() > 0 ) {
			return rl.get(0).getType();
		}
		return RecType.RECORDER;
	}

	// レコーダIDから表示名称を取得する
	public String getRecorderName(String recId){
		if ( recId == null ) {
			return "";
		}

		for ( HDDRecorder rec : this ) {
			if ( recId.equals(rec.Myself()) ) {
				return rec.getDispName();
			}
		}
		return "";
	}

	/**
	 *  レコーダIDに合ったプラグイン（一族郎党）を探す
	 */
	public HDDRecorderList findPlugin(String recId) {
		if ( recId == null ) {
			return this;
		}

		HDDRecorderList mylist = new HDDRecorderList();
		for ( HDDRecorder rec : this ) {
			if ( recId.equals(rec.getRecorderId()) ) {
				mylist.add(rec);
			}
		}
		return mylist;
	}


	/***************************************
	 * レコーダのインスタンスを探す
	 **************************************/

	/**
	 * 実レコーダのプラグイン（個体）を探す
	 * @param mySelf 「すべて」を指定する場合はNULLをどうぞ
	 * @return
	 * <P> 「すべて」「ピックアップのみ」→全部のインスタンスを返す
	 * <P> 「個別指定」→本来{@link HDDRecorder}を返すべきだが、呼び出し側の処理を書きやすくするために{@link HDDRecorderList}を返す。よって、==nullではなく.size()==0で確認する。
	 */
	public HDDRecorderList findInstance(String mySelf) {
		if ( mySelf == null || mySelf == HDDRecorder.SELECTED_ALL || mySelf == HDDRecorder.SELECTED_PICKUP ) {
			// 「すべて」「ピックアップのみ」→全部のインスタンスを返す
			return this;
		}

		// 個別指定
		HDDRecorderList mylist = new HDDRecorderList();
		for ( HDDRecorder rec : this ) {
			if ( rec.isMyself(mySelf) ) {
				mylist.add(rec);
				break;
			}
		}
		return mylist;
	}

	/**
	 * 実レコーダのプラグイン（種別グループ）を探す
	 */
	public HDDRecorderList findInstance(RecType rectype) {
		if ( rectype == null ) {
			// 全部のインスタンスを返す
			return this;
		}

		// 個別指定
		HDDRecorderList mylist = new HDDRecorderList();
		for ( HDDRecorder rec : this ) {
			if ( rec.getType() == rectype ) {
				mylist.add(rec);
				break;
			}
		}
		return mylist;
	}


	/***************************************
	 * 類似予約検索
	 **************************************/

	/**
	 * 類似予約検索
	 */
	public LikeReserveList findLikeReserves(ProgDetailList tvd, String keywordVal, int thresholdVal, int range, boolean reversesearch) {

		long rangeVal = range * 3600000;	// ミリ秒に

		LikeReserveList likeRsvList = new LikeReserveList();

		for ( HDDRecorder recorder : this ) {

			// 終了した予約を整理する
			recorder.removePassedReserves();

			for ( ReserveList r : recorder.getReserves() ) {

				// タイトルのマッチング
				if ( keywordVal != null ) {
					if ( ! isLikeTitle(r.getTitlePop(), keywordVal, thresholdVal, reversesearch) ) {
						continue;
					}
				}
				else {
					if ( ! isLikeTitle(tvd.titlePop, r.getTitlePop()) ) {
						continue;
					}
				}

				// 放送局のマッチング
				if ( ! isLikeChannel(tvd.center, r.getCh_name()) ) {
					continue;
				}

				// 近接時間チェック
				Long d = getLikeDist(tvd.startDateTime, r, rangeVal);
				if ( d == null ) {
					continue;
				}

				// 類似予約あり
				likeRsvList.add(new LikeReserveItem(recorder, r, d));
			}

		}

		return likeRsvList;
	}

	private boolean isLikeTitle(String rsv_titlePop, String keywordVal, int thresholdVal, boolean reversesearch) {

		// 双方向の比較を行う・正引き
		int fazScore = TraceProgram.sumScore(keywordVal, rsv_titlePop);
		if ( fazScore >= thresholdVal ) {
			return true;
		}
		else if ( reversesearch ) {
			// 逆引き
			fazScore = TraceProgram.sumScore(rsv_titlePop, keywordVal);
			if ( fazScore >= thresholdVal) {
				return true;
			}
		}

		return false;
	}

	private boolean isLikeTitle(String titlePop, String rsv_titlePop) {

		// 完全一致
		if ( rsv_titlePop.equals(titlePop)) {
			return true;
		}

		return false;
	}

	private boolean isLikeChannel(String webChName, String rsv_webChName) {

		if ( rsv_webChName == null ) {
			return false;
		}
		if ( ! rsv_webChName.equals(webChName) ) {
			return false;
		}

		return true;
	}

	private Long getLikeDist(String startDateTime, ReserveList r, long range) {

		Long d = null;

		ArrayList<String> starts = new ArrayList<String>();
		ArrayList<String> ends = new ArrayList<String>();
		CommonUtils.getStartEndList(starts, ends, r);

		for ( int j=0; j<starts.size(); j++ ) {
			long dtmp = CommonUtils.getCompareDateTime(starts.get(j),startDateTime);
			if ( range > 0 && Math.abs(dtmp) >= range ) {
				// 範囲指定があって範囲外ならスキップ
				continue;
			}
			else if ( d == null || Math.abs(d) > Math.abs(dtmp) ) {
				// 初値、または一番小さい値を採用
				d = dtmp;
			}
		}

		return d;
	}

	/***************************************
	 * 隣接予約検索
	 **************************************/
	/**
	 *
	 * @param adjnotrep : falseの場合、終了時刻と開始時刻が重なるものを重複として扱います。
	 * @param overlapup : trueの場合、予約の開始時刻が番組の終了時間の１分前になってるものは重複としてあつかいません。
	 */
	public LikeReserveList findOverlapReserves(ProgDetailList tvd, String clicked, boolean adjnotrep, boolean overlapup) {

		LikeReserveList overlapRsvList =  new LikeReserveList();

		for ( HDDRecorder recorder : this ) {

			for ( ReserveList r : recorder.getReserves() ) {

				// 放送局のマッチング
				if (r.getCh_name() == null) {
					if ( r.getChannel() == null ) {
						System.err.println("予約情報にCHコードが設定されていません。バグの可能性があります。 recid="+recorder.Myself()+" chname="+r.getCh_name());
					}
					continue;
				}
				if ( ! r.getCh_name().equals(tvd.center)) {
					continue;
				}

				// 重複時間チェック
				boolean inRange = false;
				long d = 0;

				{
					ArrayList<String> starts = new ArrayList<String>();
					ArrayList<String> ends = new ArrayList<String>();
					CommonUtils.getStartEndList(starts, ends, r);
					if ( clicked != null ) {
						// 新聞形式はピンポイント（マウスポインタのある位置の時刻）
						for (int j=0; j<starts.size(); j++) {
							if ( clicked.compareTo(starts.get(j)) >= 0 && clicked.compareTo(ends.get(j)) <= 0 ) {
								inRange = true;
								break;
							}
						}
					}
					else {
						// リスト形式は幅がある（開始～終了までの間のいずれかの時刻）
						for (int j=0; j<starts.size(); j++) {
							if ( CommonUtils.isOverlap(tvd.startDateTime, tvd.endDateTime, starts.get(j), ends.get(j), adjnotrep) ) {
								if ( overlapup ) {
									// 前倒し１分設定で実際に１分前倒しだったら、これは無視してよい
									if ( CommonUtils.getCompareDateTime(tvd.endDateTime, starts.get(j)) == 60000 ) {
										continue;
									}
								}
								inRange = true;
								d = CommonUtils.getCompareDateTime(tvd.startDateTime, starts.get(j));
								break;
							}
						}
					}
				}
				if ( ! inRange) {
					continue;
				}

				// 類似予約あり！
				overlapRsvList.add(new LikeReserveItem(recorder, r, d));
			}
		}

		return overlapRsvList;
	}
}
