package tainavi;

import java.util.HashMap;

import tainavi.TVProgram.ProgFlags;
import tainavi.TVProgram.ProgOption;
import tainavi.TVProgram.ProgScrumble;

/**
 * 番組情報のマークを扱うクラス
 */
public class MarkChar {

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	public static enum MarkItem {
		EXTENTION		( "★延長注意★" ),
		EXTENTION_S		( "(延)" ),
		NEWARRIVAL		( "[NEW]" ),
		NEWARRIVAL_S	( "(N)" ),
		MOVED			( "(移)" ),

		NEW				( "【新】" ),
		LAST			( "【終】" ),
		NOSCRUMBLE		( "【無料】" ),
		MODIFIED		( "(更)" ),
		NONREPEATED		( "(初)" ),
		SPECIAL			( "【特】" ),
		RATING			( "【Ｒ】" ),
		FIRST			( "【初】" ),
		NOSYOBO			( "[!]" ),
		LIVE			( "[生]" ),
		PV				( "[PV]" ),
		SUBTITLE		( "[字]" ),
		BILINGUAL		( "[二]" ),
		MULTIVOICE		( "[多]" ),
		STANDIN			( "[吹]" ),
		DATA			( "[デ]" ),
		SURROUND		( "[SS]" ),
		PRECEDING		( "【先】" ),

		REPEATED		( "[再]" ),
		;

		private String name;

		private MarkItem(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	private Env env;

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public MarkChar(Env env) {
		this.env = env;
	}

	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	/**
	 *  延長警告マークの取得
	 */
	public String getExtensionMark(ProgDetailList tvd) {
		return ((tvd.extension) ? ((env.getShortExtMark())?(MarkItem.EXTENTION_S.getName()):(MarkItem.EXTENTION.getName())) : (""));
	}

	/**
	 * 新番組と最終回だけわける
	 */
	public String getNewLastMark(ProgDetailList tvd) {
		if (tvd.flag == ProgFlags.NEW && env.getOptMarks().get(ProgOption.HIDDEN_NEW) == Boolean.TRUE) {
			return MarkItem.NEW.getName();
		}
		else if (tvd.flag == ProgFlags.LAST && env.getOptMarks().get(ProgOption.HIDDEN_LAST) == Boolean.TRUE) {
			return MarkItem.LAST.getName();
		}
		return "";
	}

	/**
	 *  普通のマークの取得
	 */
	public String getOptionMark(ProgDetailList tvd) {
		return getOptionMark(tvd, env.getOptMarks());
	}

	/**
	 * タイトルの後ろにつくマークの取得
	 */
	public String getPostfixMark(ProgDetailList tvd) {
		return getOptionMark(tvd, env.getOptPostfixMarks());
	}

	/**
	 * 前後のマークの取得
	 *
	 * @param tvd			番組詳細情報
	 * @param options		表示オプション
	 * @return				マーク
	 */
	public String getOptionMark(ProgDetailList tvd, HashMap<TVProgram.ProgOption,Boolean> options) {

		StringBuilder sb = new StringBuilder("");

		if (tvd.noscrumble == ProgScrumble.NOSCRUMBLE && options.get(ProgOption.HIDDEN_NOSCRUMBLE) == Boolean.TRUE) {
			sb.append(MarkItem.NOSCRUMBLE.getName());
		}

		if ( tvd.newarrival && options.get(ProgOption.NEWARRIVAL) == Boolean.TRUE ) {
			sb.append(((env.getShortExtMark())?(MarkItem.NEWARRIVAL_S.getName()):(MarkItem.NEWARRIVAL.getName())));
		}

		if ( tvd.modified && options.get(ProgOption.MODIFIED) == Boolean.TRUE ) {
			sb.append(MarkItem.MODIFIED.getName());
		}
		if ( tvd.nonrepeated && options.get(ProgOption.NONREPEATED) == Boolean.TRUE ) {
			sb.append(MarkItem.NONREPEATED.getName());
		}

		for ( ProgOption opt : tvd.getOption() ) {
			if ( options.get(opt) != null && options.get(opt) == Boolean.FALSE ) {
				continue;
			}

			sb.append(opt.getMark());
		}

		return sb.toString();
	}
}
