package tainavi;

import java.util.ArrayList;

import tainavi.TVProgram.ProgType;
import tainavi.plugintv.Syobocal;

/**
 * <P>{@link TVProgram} のリストを実現するクラスです.
 * <P><B>使用の前に必ずclear()を実行すること！！</B> 
 * @version 3.15.4β～
 */
public class TVProgramList extends ArrayList<TVProgram> implements Cloneable {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * 放送局をソートした結果が得られるはずだ。
	 */
	public TVProgramIterator getIterator() { return iterator; }
	
	private final TVProgramIterator iterator = new TVProgramIterator() {
		@Override
		public TVProgramIterator build(ArrayList<Center> clst, IterationType tuner) {
			return super._build(TVProgramList.this,clst,tuner);
		}
	};
	
	
	//　ここから本体
	
	public TVProgramList() {
		super();
	}
	
	private TVProgramList progs = null;
	private TVProgramList tv = null;
	private TVProgramList cs = null;
	private TVProgramList cs2 = null;
	private TVProgramList radio = null;
	private Syobocal syobo = null;
	private PassedProgram passed = null;
	private PickedProgram picked = null;
	private SearchResult searched = null;	// これだけ別枠

	/**
	 * {@link ProgType#PROG}だけのリストを返す
	 */
	public TVProgramList getProgPlugins() {
		return progs;
	}
	
	/**
	 * @param progId : nullの場合は先頭の要素を返す。
	 */
	public TVProgram getTvProgPlugin(String progId) { return (tv.size()>0)?(getProgPlugin(tv, progId)):(null); }
	/**
	 * @param progId : nullの場合は先頭の要素を返す。
	 */
	public TVProgram getCsProgPlugin(String progId) { return (cs.size()>0)?(getProgPlugin(cs, progId)):(null); }
	/**
	 * @param progId : nullの場合は先頭の要素を返す。
	 */
	public TVProgram getCs2ProgPlugin(String progId) { return (cs2.size()>0)?(getProgPlugin(cs2, progId)):(null); }
	/**
	 * @param progId : nullの場合は先頭の要素を返す。
	 */
	public TVProgram getRadioProgPlugin(String progId) { return (radio.size()>0)?(getProgPlugin(radio, progId)):(null); }
	/**
	 * @param progId : nullの場合は先頭の要素を返す。
	 */
	public TVProgram getProgPlugin(String progId) {
		return getProgPlugin(this, progId);
	}
	
	public Syobocal getSyobo() { return syobo; }
	public PassedProgram getPassed() { return passed; }
	public PickedProgram getPickup() { return picked; }
	public SearchResult getSearched() { return searched; }
	
	private TVProgram getProgPlugin(TVProgramList pList, String progId) {
		if ( pList.size() == 0 ) {
			return null;
		}
		if ( progId == null ) {
			return pList.get(0);
		}
		for ( TVProgram prog : pList ) {
			if ( prog.getTVProgramId().equals(progId) ) {
				return prog;
			}
		}
		return null;
	}

	public TVProgramList getTvProgPlugins() { return tv; }
	public TVProgramList getCsProgPlugins() { return cs; }
	public TVProgramList getCs2ProgPlugins() { return cs2; }
	public TVProgramList getRadioProgPlugins() { return radio; }

	//
	@Override
	public boolean add(TVProgram prog) {
		
		if ( prog.getType() != ProgType.SEARCHED ) {
			// 検索結果は全体リストに含めない
			super.add(prog);
		}
		
		if ( tv == null ) {
			return true;	// ループ防止
		}
		
		if ( prog.getType() == ProgType.PROG ) {
			
			progs.add(prog);
			
			switch (prog.getSubtype()) {
			case TERRA:
				tv.add(prog);
				break;
			case CS:
				cs.add(prog);
				break;
			case CS2:
				cs2.add(prog);
				break;
			case RADIO:
				radio.add(prog);
				break;
			default:
				break;
			}
		}
		else if ( prog.getType() == ProgType.SYOBO ) {
			syobo = (Syobocal) prog;
		}
		else if ( prog.getType() == ProgType.PASSED ) {
			passed = (PassedProgram) prog;
		}
		else if ( prog.getType() == ProgType.PICKED ) {
			picked = (PickedProgram) prog;
		}
		else if ( prog.getType() == ProgType.SEARCHED ) {
			searched = (SearchResult) prog;
		}
		else {
			System.err.println("[DEBUG] 不正なWeb番組表プラグインです： type="+prog.getType()+" subtype="+prog.getSubtype()+" id="+prog.getTVProgramId());
			return false;
		}
		
		return true;
	}
	
	//
	@Override
	public void clear() {
		super.clear();
		progs = new TVProgramList();
		tv = new TVProgramList();
		cs = new TVProgramList();
		cs2 = new TVProgramList();
		radio = new TVProgramList();
		syobo = null;
		passed = null;
		picked = null;
		searched = null;
	}

	//
	@Override
	public TVProgramList clone() {
		TVProgramList p = (TVProgramList) super.clone();
		FieldUtils.deepCopy(p, this); // ディープコピーするよ
		return p;
	}
}
