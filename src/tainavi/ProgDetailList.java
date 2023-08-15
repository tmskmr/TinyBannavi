package tainavi;

import java.util.ArrayList;
import java.util.GregorianCalendar;

import tainavi.TVProgram.ProgFlags;
import tainavi.TVProgram.ProgGenre;
import tainavi.TVProgram.ProgOption;
import tainavi.TVProgram.ProgScrumble;
import tainavi.TVProgram.ProgSubgenre;
import tainavi.TVProgram.ProgSubtype;
import tainavi.TVProgram.ProgType;

/**
 * 番組の個々の詳細情報です
 */
public class ProgDetailList implements Cloneable {

	/*******************************************************************************
	 * メンバ
	 ******************************************************************************/

	/*
	 *  ファイルに保存されるもの
	 */

	public String title = "";				// 分離前タイトル
	public String detail = "";				// 修正前タイトル
	protected String addedDetail = "";

	public String startDateTime = "";
	public String endDateTime = "";
	public int length = 0;					// 放送時間（05:00を基準とした表示上の長さ）

	public String link = "";
	public String linkSyobo = "";

	// ジャンル
	public ProgGenre genre = ProgGenre.NOGENRE;
	public ProgSubgenre subgenre = ProgSubgenre.NOGENRE_ETC;

	// フラグ・オプション関連
	public boolean extension = false;						// 延長注意
	public ProgFlags flag = ProgFlags.NOFLAG;				// 新番組・最終回
	public ProgScrumble noscrumble = ProgScrumble.NONE;	// 有料放送だけ別枠になってしまった！
	protected ArrayList<ProgOption> option = new ArrayList<ProgOption>();

	// 複数ジャンル対応
	public ArrayList<ProgGenre> genrelist = null;
	public ArrayList<ProgSubgenre> subgenrelist = null;

	/*
	 *  ファイルには保存されないもの
	 */

	/**
	 * <P>{@link ProgDateList#Date}が05-29時を基準とした日付を持つのに対して
	 * <P>accurateDateは実際の日付である
	 * <P>（startDateTimeの日付と同じ、ただし曜日文字がつく）
	 */

	public String accurateDate = "";
	public String start = "";				// hh:mm
	public String end = "";				// hh:mm
	public int recmin = 0;					// 番組長（実際の長さ）

	public String progid = "";				// 番組ID

	public boolean nosyobo = false;		// しょぼのぼっち判定
	public boolean marked = false;	// 検索に引っかかったマーク
	public boolean markedByTrace = false;	// 番組追跡に引っかかったマーク
	public boolean newarrival = false;		// 新着だなぁ
	public boolean modified = false;		// 詳細が更新された
	public boolean nonrepeated = false;	// リピート放送の初回かな？
	public boolean showinstandby = false;	// 予約待機への表示を行うか？

	public ProgType type = null;			// Web番組表か、しょぼかるか、過去ログか
	public ProgSubtype subtype = null;		// サブタイプ
	public String center = "";				// 放送局名
	public String extension_mark = "";		// ★延長注意★
	public String prefix_mark = "";		// (移)とか
	public String newlast_mark = "";		// [新]とか
	public String postfix_mark = "";		// [再]とか
	public boolean dontoverlapdown = false;// 終了時間を短縮してはだめ

	public String splitted_title = "";		// 分離済みタイトル→これはtitleへのポインタ
	public String splitted_titlePop = "";	// 分離済みタイトルPOP→これはtitlePopへのポインタ
	public String splitted_detail = "";	// 分離済み番組詳細→detailはこれへのポインタにするよ

	public SearchKey dynKey = null;			// 動的検索用
	public String dynMatched = null;			// 動的検索用

	// 検索高速化用のデータ

	/**
	 * キーワード検索・番組追跡で使うよ
	 */
	public String titlePop = "";

	/**
	 * キーワード検索で使うよ
	 */
	public String detailPop = "";

	/**
	 * 番組追跡で使うよ
	 */
	public static boolean tracenOnlyTitle = false;

	/*******************************************************************************
	 * NGワード処理
	 ******************************************************************************/

	private static final String NO_PROG_TITLE = "番組情報がありません";

	public void abon() {
		start = "";
		title = splitted_title = NO_PROG_TITLE;
		detail = "";
		addedDetail = "";

		extension = false;
		flag = null;
		option = new ArrayList<ProgOption>();
		noscrumble = ProgScrumble.NONE;

		genre = ProgGenre.NOGENRE;
		subgenre = ProgSubgenre.NOGENRE_ETC;
		genrelist = null;
		subgenrelist = null;

		progid = "";
		nosyobo = false;
		marked = false;
		markedByTrace = false;
		newarrival = false;
		modified = false;
		nonrepeated = false;
		showinstandby = false;

		titlePop = "";
		detailPop = "";

		linkSyobo = "";
	}

	/*******************************************************************************
	 * 過去ログ処理高速化計画
	 ******************************************************************************/

	public boolean isEqualsGenre(ProgGenre mGenre, ProgSubgenre mSubgenre) {
		if ( mGenre == null && mSubgenre == null ) {
			return false;
		}
		if ( this.genrelist == null ) {
			// 従来の比較
			if ( mGenre == null || this.genre == mGenre ) {
				if ( mSubgenre == null ) {
					return true;
				}
				else {
					if ( this.subgenre == mSubgenre ) {
						return true;
					}
				}
			}
		}
		else {
			// マルチジャンル対応時の比較
			for ( int n=0; n<this.genrelist.size(); n++ ) {
				if ( mGenre == null || this.genrelist.get(n) == mGenre ) {
					if ( mSubgenre == null ) {
						return true;
					}
					else {
						if ( this.subgenrelist.get(n) == mSubgenre ) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/*
	 * addedDetailは直接編集させない
	 */

	public String getAddedDetail() { return addedDetail; }
	public void setAddedDetail(String s) { addedDetail = s; }

	/*
	 * optionは直接編集させない
	 */

	public ArrayList<ProgOption> getOption() { return option; }
	public boolean addOption(ProgOption opt) {
		int i = getOptionIndex(opt);
		if ( i == -1 ) {
			this.option.add(opt);
			return true;
		}
		return false;
	}
	public boolean removeOption(ProgOption opt) {
		int i = getOptionIndex(opt);
		if ( i >= 0 ) {
			this.option.remove(i);
			return true;
		}
		return false;
	}
	public boolean isOptionEnabled(ProgOption opt) {
		return (getOptionIndex(opt)>=0);
	}
	private int getOptionIndex(ProgOption opt) {
		int size=this.option.size();
		for (int i=0; i<size; i++) {
			if (this.option.get(i) == opt) {
				return i;
			}
		}
		return -1;
	}

	public boolean setGenreStr() {
		if (genrelist == null || genrelist.size() == 0) {
			return true;
		}

		String s = GENREMARK;
		for ( int n=0; n<genrelist.size(); n++ ) {
			s += genrelist.get(n).toString()+" - "+subgenrelist.get(n).toString()+"\n";
		}
		if ( addedDetail.indexOf(GENREMARK) == -1 ) {
			addedDetail = s+"\n"+addedDetail;
		}
		else {
			addedDetail = addedDetail.replace(GENREMARK+".+?\n\n", GENREMARK+s+"\n");
		}
		return true;
	}
	private static final String GENREMARK = "\n【ジャンル】\n";

	public boolean setContentIdStr() {
		String pid = null;
		if ( ContentIdEDCB.isValid(progid) ) {
			pid = ContentIdEDCB.stripMark(progid);
		}
		else if ( ContentIdREGZA.isValid(progid) ) {
			pid = ContentIdREGZA.stripMark(progid);
		}
		else if ( ContentIdDIMORA.isValid(progid) ) {
			pid = ContentIdDIMORA.stripMark(progid);
		}
		if ( pid != null )
		{
			if ( addedDetail.indexOf(CIDMARK) == -1 ) {
				addedDetail += CIDMARK+pid+"\n";
			}
			else {
				addedDetail = addedDetail.replace(CIDMARK+".+?\n", pid+"\n");
			}
			return true;
		}
		return false;
	}

	private static final String CIDMARK = "\n【ID】\nid=";

	/*******************************************************************************
	 * 過去ログ処理高速化計画
	 ******************************************************************************/

	/**
	 *
	 */
	public static enum WrHeader {

		// 順番をかえなければ、どこに追加してもいい

		START		( "$ST$", true ),
		END			( "$ED$", true ),
		LENGTH		( "$LN$", true ),
		GENRE		( "$GE$", true ),
		GENRELIST	( "$GL$", true ),
		EXTENTION	( "$EX$", true ),
		SCRUMBLED	( "$SC$", true ),
		FLAG		( "$FL$", true ),
		OPTION		( "$OP$", true ),
		LINK		( "$LK$", true ),
		ID			( "$ID$", true ),

		TITLE		( "$TI$", true ),
		DETAIL		( "$DT$", true ),
		ADDED		( "$AD$", true ),

		// ここから下は別の領域なので追加はNG

		BEND		( "$DE#YY$", false ),	// 項目ごとのフッタ
		STARTMARK	( "$DE#AA$", false ),	// ヘッダ
		ENDMARK		( "$DE#ZZ$", false ),	// フッタ

		;

		private String hdr;
		private boolean marker;

		private WrHeader(String hdr, boolean marker) {
			this.hdr = hdr;
			this.marker = marker;
		}

		// ここはtoString()をOverrideしてよい
		@Override
		public String toString() { return hdr; }
	}

	// 区切り文字
	private static final String S_CR = "\n";
	private static final String S_OPT = ";";

	private void addHeader(StringBuilder sb, WrHeader header) {
		sb.append(header.toString());
	}
	private void addBody(StringBuilder sb, String body) {
		sb.append(body);
	}
	private void addBodyEnd(StringBuilder sb) {
		sb.append(WrHeader.BEND);
	}

	/**
	 * @see #ProgDetailList(String)
	 */
	public String toString() {

		StringBuilder sb = new StringBuilder();

		sb.append(WrHeader.STARTMARK);
		sb.append(S_CR);

		for ( WrHeader hdr : WrHeader.values() )
		{
			if ( ! hdr.marker )
			{
				break;
			}

			switch ( hdr ) {
			case TITLE:
				if ( this.title != null && this.title.length() > 0 ) {
					addHeader(sb, hdr);
					addBody(sb, this.title);
					addBodyEnd(sb);
				}
				break;
			case DETAIL:
				if ( this.detail != null && this.detail.length() > 0 ) {
					addHeader(sb, hdr);
					addBody(sb, this.detail);
					addBodyEnd(sb);
				}
				break;
			case ADDED:
				if ( this.addedDetail != null && this.addedDetail.length() > 0 ) {
					addHeader(sb, hdr);
					addBody(sb, this.addedDetail);
					addBodyEnd(sb);
				}
				break;
			case START:
				if ( this.startDateTime != null && this.startDateTime.length() > 0 ) {
					addHeader(sb, hdr);
					addBody(sb, this.startDateTime);
					addBodyEnd(sb);
				}
				break;
			case END:
				if ( this.endDateTime != null && this.endDateTime.length() > 0 ) {
					addHeader(sb, hdr);
					addBody(sb, this.endDateTime);
					addBodyEnd(sb);
				}
				break;
			case LINK:
				if ( this.link != null && this.link.length() > 0 ) {
					addHeader(sb, hdr);
					addBody(sb, this.link);
					addBodyEnd(sb);
				}
				break;
			case LENGTH:
				if ( this.length > 0 ) {
					addHeader(sb, hdr);
					addBody(sb, String.valueOf(this.length));
					addBodyEnd(sb);
				}
				break;
			case GENRE:
				if ( this.genre != null && this.genre != ProgGenre.NOGENRE )
				{
					addHeader(sb, hdr);
					addBody(sb, this.genre.toIEPG());

					if ( this.subgenre != null )
					{
						addBody(sb, this.subgenre.toIEPG());
					}
					else
					{
						addBody(sb, ProgSubgenre.NOGENRE_ETC.toIEPG());
					}

					addBodyEnd(sb);
				}
				break;
			case GENRELIST:
				if ( this.genrelist != null && this.genrelist.size() > 0 )
				{
					addHeader(sb, hdr);

					int n = -1;
					for ( ProgGenre g : this.genrelist )
					{
						++n;
						sb.append(g.toIEPG());

						if ( this.subgenrelist != null && this.subgenrelist.size() > n )
						{
							addBody(sb, this.subgenrelist.get(n).toIEPG());
						}
						else
						{
							sb.append(ProgSubgenre.NOGENRE_ETC);
						}
					}

					addBodyEnd(sb);
				}
				break;
			case EXTENTION:
				if ( this.extension )
				{
					addHeader(sb, hdr);
					// BODYはない
					addBodyEnd(sb);
				}
				break;
			case SCRUMBLED:
				if ( this.noscrumble != null && this.noscrumble != ProgScrumble.NOSCRUMBLE )
				{
					addHeader(sb, hdr);
					// BODYはない
					addBodyEnd(sb);
				}
				break;
			case FLAG:
				if ( this.flag != null && this.flag != ProgFlags.NOFLAG )
				{
					addHeader(sb, hdr);
					addBody(sb, this.flag.toString());
					addBodyEnd(sb);
				}
				break;
			case OPTION:
				if ( this.option != null && this.option.size() > 0 )
				{
					addHeader(sb, hdr);

					for ( ProgOption o : this.option )
					{
						addBody(sb, o.toString());
						addBody(sb, S_OPT);
					}

					addBodyEnd(sb);
				}
				break;
			case ID:
				if ( this.progid != null && this.progid.length() > 0 ) {
					addHeader(sb, hdr);
					addBody(sb, this.progid);
					addBodyEnd(sb);
				}
				break;
			default:
				break;
			}
		}

		sb.append(WrHeader.ENDMARK);
		sb.append(S_CR);

		return sb.toString();
	}

	/*******************************************************************************
	 * コンストラクタ２種
	 ******************************************************************************/

	/**
	 * toString()で生成した文字列を入力にして初期化された{@link ProgDetailList}を返します
	 * @param s 先頭の{@link WrHeader.STARTMARK}+"\n"とおしりの{@link WrHeader.ENDMARK}+"\n"は外しておいてください
	 */
	public ProgDetailList(String s) {

		for ( WrHeader hdr : WrHeader.values() )
		{
			if ( ! hdr.marker )
			{
				break;		// ! markerならもう終わり
			}

			if ( s == null || s.length() <= 0 )
			{
				break;		// なんか変
			}

			if ( ! s.startsWith(hdr.toString()) )
			{
				continue;	// 次へ
			}

			int newtail = s.indexOf(WrHeader.BEND.toString());
			if ( newtail == -1 )
			{
				break;		// なんか変
			}

			String body = s.substring(hdr.toString().length(),newtail);
			s = s.substring(newtail+WrHeader.BEND.toString().length());

			if ( body == null || body.length() == 0 )
			{
				continue;	// いらんわ
			}

			switch ( hdr ) {
			case TITLE:
				this.title = body;
				this.titlePop = TraceProgram.replacePop(this.title);
				break;
			case DETAIL:
				this.detail = body;
				this.detailPop = TraceProgram.replacePop(this.detail);
				break;
			case ADDED:
				this.addedDetail = body;
				break;
			case START:
				GregorianCalendar ca = CommonUtils.getCalendar(body);
				if ( ca != null )
				{
					this.startDateTime = CommonUtils.getDateTime(ca);
					this.accurateDate = CommonUtils.getDate(ca);
					this.start = CommonUtils.getTime(ca);
				}
				break;
			case END:
				GregorianCalendar cz = CommonUtils.getCalendar(body);
				if ( cz != null )
				{
					this.endDateTime = CommonUtils.getDateTime(cz);
					this.end = CommonUtils.getTime(cz);
					this.recmin = CommonUtils.getRecMinVal(this.start,this.end);
				}
				break;
			case LINK:
				this.link = body;
				break;
			case LENGTH:
				try {
					this.length = Integer.valueOf(body);
				}
				catch (NumberFormatException e) {
					//
				}
				break;
			case GENRE:
				if ( body.length() >= 1 )
				{
					this.genre = ProgGenre.getByIEPG(body.substring(0,1));
					if ( this.genre == null )
					{
						this.genre = ProgGenre.NOGENRE;
					}

					if ( body.length() >= 2 )
					{
						this.subgenre = ProgSubgenre.getByIEPG(this.genre, body.substring(1,2));
					}
					if ( this.subgenre == null )
					{
						this.subgenre = ProgSubgenre.NOGENRE_ETC;
					}
				}
				break;
			case GENRELIST:
				while ( body.length() >= 2 )
				{
					if ( this.genrelist == null )
					{
						this.genrelist = new ArrayList<ProgGenre>();
						this.subgenrelist = new ArrayList<ProgSubgenre>();
					}

					ProgGenre g = ProgGenre.getByIEPG(body.substring(0,1));
					if ( g == null )
					{
						g = ProgGenre.NOGENRE;
					}
					ProgSubgenre sg = ProgSubgenre.getByIEPG(g,body.substring(1,2));
					if ( sg == null )
					{
						sg = ProgSubgenre.NOGENRE_ETC;
					}

					this.genrelist.add(g);
					this.subgenrelist.add(sg);

					body = body.substring(2);
				}
				break;
			case EXTENTION:
				this.extension = true;
				break;
			case SCRUMBLED:
				this.noscrumble = ProgScrumble.SCRUMBLED;
				break;
			case FLAG:
				for ( ProgFlags f : ProgFlags.values() )
				{
					if ( f.toString().equals(body) )
					{
						this.flag = f;
						break;
					}
				}
				break;
			case OPTION:
				//this.option = new ArrayList<ProgOption>();	// いらない
				for ( String opt : body.split(S_OPT) )
				{
					for ( ProgOption o : ProgOption.values() )
					{
						if ( o.toString().equals(opt) )
						{
							this.option.add(o);
							break;
						}
					}
				}
				break;
			case ID:
				this.progid = body;
				break;
			default:
				break;
			}
		}
	}

	/**
	 *  通常のコンストラクタ
	 */
	public ProgDetailList() {
	}

	@Override
	public ProgDetailList clone() {
		try {
			return (ProgDetailList) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}

}
