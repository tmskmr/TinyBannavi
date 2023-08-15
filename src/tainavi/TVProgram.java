package tainavi;

import java.util.ArrayList;

/**
 * Web番組表の設定や、実際の番組情報などを保持するクラスです。
 * @see TVProgramList
 * @see TVProgramUtils
 * @see TVProgramIterator
 */
public interface TVProgram {

	// 定数
	public enum ProgScrumble { NONE, NOSCRUMBLE, SCRUMBLED };
	public enum ProgFlags { NOFLAG, NEW, LAST };
	public enum ProgType { PROG, SYOBO, PASSED, PICKED, SEARCHED, OTHERS };
	public enum ProgSubtype { TERRA, CS, CS2, CS3, RADIO, NONE };

	/**
	 *  NOSCRUMBLEは間違って（？）ProgScrumbleを使うこととなってしまったのだ。ここにあるのはマーク表示のための一覧用なのだ。
	 */
	public enum ProgOption {
		HIDDEN_NEW			("【新】", "新番組" ),
		HIDDEN_LAST		("【終】", "最終回" ),
		HIDDEN_NOSCRUMBLE	("【無料】", "無料放送" ),
		FIRST				("【初】", "初回放送" ),
		REPEAT				("[再]",	"再放送" ),
		LIVE				("[生]",	"生放送" ),
		SPECIAL				("[特]",	"特番" ),
		NOSYOBO 			("[!]",		"しょぼかる未定義" ),
		SUBTITLE			("[字]",	"字幕放送"	),
		BILINGUAL			("[二]",	"二か国語放送" ),
		STANDIN				("[吹]",	"吹替放送" ),
		PV 					("[PV]",	"ペイパービュー" ),
		MULTIVOICE			("[多]",	"音声多重放送" ),
		DATA 				("[デ]",	"番組連動データ放送" ),
		SURROUND 			("[SS]",	"サラウンドステレオ" ),
		NEWARRIVAL	 		("[NEW]",	"予約待機の新着" ),
		MODIFIED 			("(更)",	"番組詳細に更新あり - 予約待機分のみ" ),
		NONREPEATED 		("[初]",	"リピート放送の初回放送回" ),
		MOVED		 		("(移)",	"先週無かったか時間が違う" ),
		PRECEDING			("【先】",	"先行放送" ),
		RATING				("[Ｒ]",	"視聴制限あり" ),

		BS4K 				("[4K]",	"BS/CS 4K放送" ),
		BS8K 				("[8K]",	"BS 8K放送"),

		NOOPTION			("",		""),

		BMODE 				("[Ｂ]",	"圧縮Bモードステレオ" ),
		NEWS				("[Ｎ]",	"ニュース" ),
		PROGRESSIVE		("[Ｐ]",	"プログレッシブ放送" ),
		STEREO				("[Ｓ]",	"ステレオ放送" ),
		WIDE				("[Ｗ]",	"ワイド放送" ),
		HDTV				("[HV]",	"HDTV" ),
		MULTIVIEW			("[MV]",	"マルチビューテレビ" ),
		SDTV				("[SD]",	"SDTV" ),
		PPV					("[PPV]",	"ペイパービュー" ),
		SIGN				("[手]",	"手話通訳放送" ),
		TWOWAY				("[双]",	"双方向放送" ),
		COMMENTARY			("[解]",	"音声解説" ),
		WEATHER				("[天]",	"天気予報" ),
		TRAFFIC				("[交]",	"交通情報" ),
		MOVIE				("[映]",	"劇映画" ),
		FREE				("[無]",	"無料放送" ),
		PAY					("[料]",	"有料放送"),
		SHOPPING			("[販]",	"通販" ),
		SURROUND51			("[5.1]",	"5.1chサラウンド" ),
		SURROUND222		("[22.2]",	"22.2chサラウンド" ),
		HDR					("[HDR]",	"ハイダイナミックレンジ" ),
		;

		String mark;
		String desc;

		ProgOption(String m, String d){
			mark = m;
			desc = d;
		}

		public String getProgLabel(){ return mark + desc;	}
		public String getMark(){ return mark; }

		public static ProgOption GetProgOptionFromMark(String mark){
			for (ProgOption po : ProgOption.values()){
				if (po.getMark().equals(mark))
					return po;
			}

			return null;
		}
	};

	public static enum ProgGenre {
		NEWS		("ニュース/報道",		"0"),
		SPORTS		("スポーツ",			"1"),
		VARIETYSHOW	("情報/ワイドショー",	"2"),
		DORAMA		("ドラマ",			"3"),
		MUSIC		("音楽",				"4"),
		VARIETY		("バラエティー",		"5"),
		MOVIE		("映画",				"6"),
		ANIME		("アニメ/特撮",		"7"),
		DOCUMENTARY	("ドキュメンタリー/教養",	"8"),
		THEATER		("劇場/公演",		"9"),
		HOBBY		("趣味/教育",		"A"),
		WELFARE		("福祉",				"B"),
		//KIDS		("キッズ",			"ZZZ"),	// もとからなかったらしい
		//EXTENTION	("拡張",			"E"),	// 非対応とする
		NOGENRE		("その他",			"F");

		private String name;
		private String iepg;

		private ProgGenre(String name, String iepg) {
			this.name = name;
			this.iepg = iepg;
		}

		@Override
		public String toString() {
			return(name);
		}

		public String toIEPG() {
			return(iepg);
		}

		/**
		 * ジャンル名文字列に一致するものを返す
		 */
		public static ProgGenre get(String name) {
			for ( ProgGenre g : ProgGenre.values() ) {
				if ( g.name.equals(name) ) {
					return g;
				}
			}
			return null;
		}

		/**
		 * IEPGで
		 */
		public static ProgGenre getByIEPG(String iepg) {
			for ( ProgGenre g : ProgGenre.values() ) {
				if ( g.iepg.equals(iepg) ) {
					return g;
				}
			}
			return null;
		}
	};

	public static enum ProgSubgenre {
		NEWS_TEIJI		(ProgGenre.NEWS,	"定時・総合",		"0"),
		NEWS_TENKI		(ProgGenre.NEWS,	"天気",			"1"),
		NEWS_TOKUSHU	(ProgGenre.NEWS,	"特集・ドキュメント",	"2"),
		NEWS_SEIJI		(ProgGenre.NEWS,	"政治・国会",		"3"),
		NEWS_KEIZAI		(ProgGenre.NEWS,	"経済・市況",		"4"),
		NEWS_KAIGAI		(ProgGenre.NEWS,	"海外・国際",		"5"),
		NEWS_KAISETU	(ProgGenre.NEWS,	"解説",			"6"),
		NEWS_TOURON		(ProgGenre.NEWS,	"討論・会談",		"7"),
		NEWS_HOUDOU		(ProgGenre.NEWS,	"報道特番",		"8"),
		NEWS_LOCAL		(ProgGenre.NEWS,	"ローカル・地域",	"9"),
		NEWS_KOTSU		(ProgGenre.NEWS,	"交通",			"A"),
		NEWS_ETC		(ProgGenre.NEWS,	"その他",			"F"),

		SPORTS_NEWS		(ProgGenre.SPORTS,	"スポーツニュース",		"0"),
		SPORTS_BASEBALL	(ProgGenre.SPORTS,	"野球",				"1"),
		SPORTS_FOOTBALL	(ProgGenre.SPORTS,	"サッカー",			"2"),
		SPORTS_GOLF		(ProgGenre.SPORTS,	"ゴルフ",				"3"),
		SPORTS_BALLGAME	(ProgGenre.SPORTS,	"その他の球技",		"4"),
		SPORTS_SUMO		(ProgGenre.SPORTS,	"相撲・格闘技",		"5"),
		SPORTS_OLYNPIC	(ProgGenre.SPORTS,	"オリンピック・国際大会",	"6"),
		SPORTS_MARATHON	(ProgGenre.SPORTS,	"マラソン・陸上・水泳",	"7"),
		SPORTS_MORTAR	(ProgGenre.SPORTS,	"モータースポーツ",		"8"),
		SPORTS_MARINE	(ProgGenre.SPORTS,	"マリン・ウィンタースポーツ","9"),
		SPORTS_KEIBA	(ProgGenre.SPORTS,	"競馬・公営競技",		"A"),
		SPORTS_ETC		(ProgGenre.SPORTS,	"その他",				"F"),

		VSHOW_GEINO		(ProgGenre.VARIETYSHOW,	"芸能・ワイドショー",		"0"),
		VSHOW_FASHION	(ProgGenre.VARIETYSHOW,	"ファッション",			"1"),
		VSHOW_LIFE		(ProgGenre.VARIETYSHOW,	"暮らし・住まい",		"2"),
		VSHOW_HEALTH	(ProgGenre.VARIETYSHOW,	"健康・医療",			"3"),
		VSHOW_SHOPPING	(ProgGenre.VARIETYSHOW,	"ショッピング・通販",		"4"),
		VSHOW_GOURMET	(ProgGenre.VARIETYSHOW,	"グルメ・料理",		"5"),
		VSHOW_EVENT		(ProgGenre.VARIETYSHOW,	"イベント",			"6"),
		VSHOW_INFO		(ProgGenre.VARIETYSHOW,	"番組紹介・お知らせ",	"7"),
		VSHOW_ETC		(ProgGenre.VARIETYSHOW,	"その他",				"F"),

		DRAMA_KOKUNAI	(ProgGenre.DORAMA,	"国内ドラマ",	"0"),
		DRAMA_KAIGAI	(ProgGenre.DORAMA,	"海外ドラマ",	"1"),
		DRAMA_JIDAI		(ProgGenre.DORAMA,	"時代劇",	"2"),
		DRAMA_ETC		(ProgGenre.DORAMA,	"その他",		"F"),

		MUSIC_KOKUNAI	(ProgGenre.MUSIC,	"国内ロック・ポップス",			"0"),
		MUSIC_KAIGAI	(ProgGenre.MUSIC,	"海外ロック・ポップス",			"1"),
		MUSIC_CLASSIC	(ProgGenre.MUSIC,	"クラシック・オペラ",				"2"),
		MUSIC_JAZZ		(ProgGenre.MUSIC,	"ジャズ・フュージョン",			"3"),
		MUSIC_KAYOKYOKU	(ProgGenre.MUSIC,	"歌謡曲・演歌",				"4"),
		MUSIC_LIVE		(ProgGenre.MUSIC,	"ライブ・コンサート",				"5"),
		MUSIC_RANKING	(ProgGenre.MUSIC,	"ランキング・リクエスト",			"6"),
		MUSIC_KARAOKE	(ProgGenre.MUSIC,	"カラオケ・のど自慢",			"7"),
		MUSIC_MINYO		(ProgGenre.MUSIC,	"民謡・邦楽",					"8"),
		MUSIC_DOYO		(ProgGenre.MUSIC,	"童謡・キッズ",					"9"),
		MUSIC_MINZOKU	(ProgGenre.MUSIC,	"民族音楽・ワールドミュージック",	"A"),
		MUSIC_ETC		(ProgGenre.MUSIC,	"その他",						"F"),

		VARIETY_QUIZ	(ProgGenre.VARIETY,	"クイズ",			"0"),
		VARIETY_GAME	(ProgGenre.VARIETY,	"ゲーム",			"1"),
		VARIETY_TALK	(ProgGenre.VARIETY,	"トークバラエティ",	"2"),
		VARIETY_OWARAI	(ProgGenre.VARIETY,	"お笑い・コメディ",	"3"),
		VARIETY_MUSIC	(ProgGenre.VARIETY,	"音楽バラエティ",	"4"),
		VARIETY_TABI	(ProgGenre.VARIETY,	"旅バラエティ",		"5"),
		VARIETY_RYORI	(ProgGenre.VARIETY,	"料理バラエティ",	"6"),
		VARIETY_ETC		(ProgGenre.VARIETY,	"その他",			"F"),

		MOVIE_YOGA		(ProgGenre.MOVIE,	"洋画",			"0"),
		MOVIE_HOGA		(ProgGenre.MOVIE,	"邦画",			"1"),
		MOVIE_ANIME		(ProgGenre.MOVIE,	"アニメ",			"2"),
		MOVIE_ETC		(ProgGenre.MOVIE,	"その他",			"F"),

		ANIME_KOKUNAI	(ProgGenre.ANIME,	"国内アニメ",		"0"),
		ANIME_KAIGAI	(ProgGenre.ANIME,	"海外アニメ",		"1"),
		ANIME_TOKUSATSU	(ProgGenre.ANIME,	"特撮",			"2"),
		ANIME_ETC		(ProgGenre.ANIME,	"その他",			"F"),

		DOC_SOCIAL		(ProgGenre.DOCUMENTARY,	"社会・時事",			"0"),
		DOC_HISTORY		(ProgGenre.DOCUMENTARY,	"歴史・紀行",			"1"),
		DOC_NATURE		(ProgGenre.DOCUMENTARY,	"自然・動物・環境",	"2"),
		DOC_SPACE		(ProgGenre.DOCUMENTARY,	"宇宙・科学・医学",	"3"),
		DOC_CULTURE		(ProgGenre.DOCUMENTARY,	"カルチャー・伝統文化",	"4"),
		DOC_BUNGEI		(ProgGenre.DOCUMENTARY,	"文学・文芸",			"5"),
		DOC_SPORTS		(ProgGenre.DOCUMENTARY,	"スポーツ",			"6"),
		DOC_DOCUMENTARY	(ProgGenre.DOCUMENTARY,	"ドキュメンタリー全般",	"7"),
		DOC_INTERVIEW	(ProgGenre.DOCUMENTARY,	"インタビュー・討論",		"8"),
		DOC_ETC			(ProgGenre.DOCUMENTARY,	"その他",				"F"),

		THEATER_GENDAI	(ProgGenre.THEATER,	"現代劇・新劇",	"0"),
		THEATER_MUSICAL	(ProgGenre.THEATER,	"ミュージカル",		"1"),
		THEATER_DANCE	(ProgGenre.THEATER,	"ダンス・バレエ",	"2"),
		THEATER_RAKUGO	(ProgGenre.THEATER,	"落語・演芸",		"3"),
		THEATER_KABUKI	(ProgGenre.THEATER,	"歌舞伎・古典",	"4"),
		THEATER_ETC		(ProgGenre.THEATER,	"その他",			"F"),

		HOBBY_TABI		(ProgGenre.HOBBY,	"旅・釣り・アウトドア",	"0"),
		HOBBY_ENGEI		(ProgGenre.HOBBY,	"園芸・ペット・手芸",	"1"),
		HOBBY_MUSIC		(ProgGenre.HOBBY,	"音楽・美術・工芸",	"2"),
		HOBBY_IGO		(ProgGenre.HOBBY,	"囲碁・将棋",			"3"),
		HOBBY_MAHJONG	(ProgGenre.HOBBY,	"麻雀・パチンコ",		"4"),
		HOBBY_CAR		(ProgGenre.HOBBY,	"車・オートバイ",		"5"),
		HOBBY_COMPUTER	(ProgGenre.HOBBY,	"コンピュータ・ＴＶゲーム",	"6"),
		HOBBY_KAIWA		(ProgGenre.HOBBY,	"会話・語学",			"7"),
		HOBBY_YOJI		(ProgGenre.HOBBY,	"幼児・小学生",		"8"),
		HOBBY_CHUGAKU	(ProgGenre.HOBBY,	"中学生・高校生",		"9"),
		HOBBY_DAIGAKU	(ProgGenre.HOBBY,	"大学生・受験",		"A"),
		HOBBY_SHOGAI	(ProgGenre.HOBBY,	"生涯教育・資格",		"B"),
		HOBBY_KYOIKU	(ProgGenre.HOBBY,	"教育問題",			"C"),
		HOBBY_ETC		(ProgGenre.HOBBY,	"その他",				"F"),

		WELFARE_KOUREI	(ProgGenre.WELFARE,	"高齢者",		"0"),
		WELFARE_SHOGAI	(ProgGenre.WELFARE,	"障害者",		"1"),
		WELFARE_HUKUSHI	(ProgGenre.WELFARE,	"社会福祉",		"2"),
		WELFARE_VULNTEER(ProgGenre.WELFARE,	"ボランティア",		"3"),
		WELFARE_SHUWA	(ProgGenre.WELFARE,	"手話",			"4"),
		WELFARE_MOJI	(ProgGenre.WELFARE,	"文字（字幕）",	"5"),
		WELFARE_ONSEI	(ProgGenre.WELFARE,	"音声解説",		"6"),
		WELFARE_ETC		(ProgGenre.WELFARE,	"その他",			"F"),

		NOGENRE_ETC		(ProgGenre.NOGENRE,	"その他",			"F"),

		;

		private ProgGenre genre;
		private String name;
		private String iepg;

		private ProgSubgenre(ProgGenre genre, String name, String iepg) {
			this.genre = genre;
			this.name = name;
			this.iepg = iepg;
		}

		@Override
		public String toString() {
			return(name);
		}

		public String toFullString() {
			return genre.toString()+" - "+name;
		}

		public String toIEPG() {
			return(iepg);
		}

		public ProgGenre getGenre() {
			return(genre);
		}

		public static ArrayList<ProgSubgenre> values(ProgGenre gr) {
			ArrayList<ProgSubgenre> ga = new ArrayList<TVProgram.ProgSubgenre>();
			for ( ProgSubgenre g : ProgSubgenre.values() ) {
				if ( g.genre == gr ) {
					ga.add(g);
				}
			}
			return ga;
		}
		public static ProgSubgenre get(ProgGenre gr, String s) {
			for ( ProgSubgenre g : ProgSubgenre.values(gr) ) {
				if ( g.name.equals(s) ) {
					return g;
				}
			}
			return null;
		}
		public static ProgSubgenre get(String s) {
			if ( s == null ) {
				return null;
			}
			boolean isfull = (s.indexOf(" - ") != -1);
			for ( ProgSubgenre g : ProgSubgenre.values() ) {
				if ( s.equals(isfull ? g.toFullString() : g.toString()) ) {
					return g;
				}
			}
			return null;
		}

		public static ProgSubgenre getByIEPG(ProgGenre gr, String iepg) {
			for ( ProgSubgenre g : ProgSubgenre.values(gr) ) {
				if ( g.iepg.equals(iepg) ) {
					return g;
				}
			}
			return null;
		}
	};

	public static final Object[][] optMarks = {
		{ ProgOption.HIDDEN_NEW },
		{ ProgOption.HIDDEN_LAST },
		{ ProgOption.HIDDEN_NOSCRUMBLE },
		{ ProgOption.FIRST },
		{ ProgOption.PRECEDING },
		{ ProgOption.NONREPEATED },
		//{ false, ProgOption.REPEAT, "[再]再放送" },
		{ ProgOption.LIVE },
		{ ProgOption.SPECIAL },
		{ ProgOption.RATING },
		{ ProgOption.SUBTITLE },
		{ ProgOption.MULTIVOICE },
		{ ProgOption.BILINGUAL },
		{ ProgOption.STANDIN },
		{ ProgOption.SURROUND },
		{ ProgOption.DATA },
		{ ProgOption.PV },
		{ ProgOption.NOSYOBO },
		{ ProgOption.NEWARRIVAL },
		{ ProgOption.MODIFIED },
		{ ProgOption.MOVED },
		{ ProgOption.BS4K},
		{ ProgOption.BS8K},

		{ ProgOption.REPEAT },
		{ ProgOption.BMODE },
		{ ProgOption.NEWS },
		{ ProgOption.PROGRESSIVE },
		{ ProgOption.STEREO },
		{ ProgOption.WIDE },
		{ ProgOption.HDTV },
		{ ProgOption.MULTIVIEW },
		{ ProgOption.SDTV },
		{ ProgOption.PPV },
		{ ProgOption.SIGN },
		{ ProgOption.TWOWAY },
		{ ProgOption.COMMENTARY } ,
		{ ProgOption.WEATHER },
		{ ProgOption.TRAFFIC },
		{ ProgOption.MOVIE },
		{ ProgOption.FREE },
		{ ProgOption.PAY },
		{ ProgOption.SHOPPING },
		{ ProgOption.SURROUND51  },
		{ ProgOption.SURROUND222 },
		{ ProgOption.HDR },
	};

	public static final String[] OKINIIRI = {"★★★★★","★★★★","★★★","★★","★",""};

	// エリアコード
	public static final String allCode = "all";
	public static final String trCode = "tr";
	public static final String bsCode = "bs";
	public static final String csCode = "cs";

	// タイトルの頭の邪魔な文字
	public static final String titlePrefixRemoveExpr = "^(\\[(新|無|字|終|HV|SS|二|映|無料)\\]|無料≫|【無料】|【アニメ】)+\\s*";
	public static final String epnoNormalizeExpr = "([第#(])(\\d\\D|\\d$)";

	// 種族の特性
	public String getTVProgramId();
	public TVProgram clone();
	//public void setProperties(TVProgramUtils from);
	public boolean isAreaSelectSupported();

	// 個体の特性
	// なし

	/*
	 * TVProgramRefreshクラスを継承する部分 ←そんなクラスあったっけ…？
	 */
	public ArrayList<ProgList> getCenters();
	public ArrayList<Center> getCRlist();
	public ArrayList<Center> getSortedCRlist();
	public void setSortedCRlist();
	public void refresh();
	public void setExtension(String spoexSearchStart, String spoexSearchEnd, boolean spoexLimitation, ArrayList<SearchKey> extKeys);
	public void abon(ArrayList<String> ngword);
	public String chkComplete();

	//public void setAbnormal(boolean b);
	//public boolean getAbnormal();

	public ProgType getType();
	public ProgSubtype getSubtype();

	public void setDebug(boolean b);
	//public boolean setProxy(String host, String port);

	/*
	 * 公開メソッドＰ（番組表）
	 */
	public boolean loadProgram(String areaCode, boolean force);
	public int getTimeBarStart();
	public void setExpandTo8(boolean b);
	public void setUseDetailCache(boolean b);

	/*
	 * 公開メソッドＡ（地域設定）
	 */
	public ArrayList<AreaCode> getAClist();
	public void loadAreaCode();
	public void saveAreaCode();
	public String getDefaultArea();
	public String getArea(String code);
	public String getCode(String Area);
	public String setSelectedAreaByName(String area);
	public String setSelectedAreaByCode(String code);
	public String getSelectedArea();
	public String getSelectedCode();

	/*
	 * 公開メソッドＣ（放送局設定）
	 */
	//public void loadCenter();
	public void loadCenter(String code, boolean force);
	public boolean saveCenter();

	/*
	 *
	 */
	public void setUserAgent(String s);
	public void setProgDir(String s);
	public void setCacheExpired(int h);
	//public void setProgressArea(StatusWindow o);
	//public void setChConv(ChannelConvert chconv);
	public void setContinueTomorrow(boolean b);
	public void setSplitEpno(boolean b);
	//public String[] doSplitEpno(ProgGenre genre, String title);

	// 拡張機能！
	public boolean setOptString(String s);
	public String getOptString();

	// キャッシュファイルからのみのロードか
	public boolean getCacheFileOnly();
}
