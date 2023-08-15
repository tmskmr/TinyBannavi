
package tainavi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import tainavi.TVProgram.ProgSubtype;
import tainavi.TVProgram.ProgType;

/**
 * Web番組表をまたいだソートのために無理やり組んだクラス。
 * @see TVProgram
 * @see TVProgramList
 * @version 3.15.4β　TVProgramListのメンバにするため抽象クラス化した。
 * @version 3.16.3β プラグイン種別に加えてエリアコードによる分類も追加（IterationType.BSも追加）
 */
abstract class TVProgramIterator implements Iterator<ProgList>,Iterable<ProgList> {

	// 抽象メソッド
	
	/**
	 * ソートしたリストを作るよ。
	 * @param tvprogs : 有効なプラグインをどうぞ。
	 * @param clst : 放送局の順番をしていしたければどうぞ。nullだとソートしない。
	 * @param tuner : {@link IterationType}
	 */
	public abstract TVProgramIterator build(ArrayList<Center> clst, IterationType tuner);
	
	
	/**
	 * どの種類のプラグイン（地上＆ＢＳ、ＣＳ、ラジオ、または全部）に絞るか。
	 */
	public static enum IterationType { ALL, TERRA, BS, CS, PASSED, RADIO };
	
	private ArrayList<ProgList> proglist;
	
	private int idx = -1;
	
	
	// 公開メソッド
	
	@Override
    public Iterator<ProgList> iterator() {
        return this;
    }
	
	@Override
	public boolean hasNext() {
		if ((idx+1) < proglist.size()) {
			return true;
		}
		return false;
	}
	
	@Override
	public ProgList next() {
		if (++idx >= proglist.size()) {
			throw new NoSuchElementException();
		}
		return getP();
	}
	
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	// 独自公開メソッド
	
	public ProgList getP() {
		return proglist.get(idx);
	}
	
	public void rewind() {
		idx = -1;
	}
	
	public int size() {
		return proglist.size();
	}
	
	/**
	 * <P>内部ポインタを指定した放送局まで移動させます
	 * <P><B>iterator.get(iterator.getIndex())とかできるわけではないので注意</B>
	 * @return -1 移動に失敗しました。ポインタは変更されません
	 */
	public int getIndex(String Center) {
		int n = 0;
		for ( ProgList pl : proglist ) {
			if ( pl.Center.equals(Center) ) {
				return idx = n;
			}
			n++;
		}
		return -1;
	}
	
	
	
	// 非公開メソッド
	
	/**
	 * {@link #build(ArrayList, IterationType)} の本体
	 */
	protected TVProgramIterator _build(TVProgramList tvprograms, ArrayList<Center> clst, IterationType tuner) {
		
		this.rewind();
		
		proglist = new ArrayList<ProgList>();
		
		if (clst != null && clst.size() > 0) {
			// 局順リストあり
			for (Center cr : clst) {
				for ( TVProgram p : tvprograms ) {
					if (isProgramEnabled(p,tuner)) {
						int centerid;
						if ((centerid = findCenterId(p.getCenters(),cr)) >= 0) {
							
							if ( ! isAreaCodeEnabled(p,cr,tuner)) {
								// エリアコードによる判定を追加
								continue;
							}
							
							proglist.add(p.getCenters().get(centerid));
							break;
						}
					}
				}
			}
			
			// 過去ログはリストにない局もリストアップして追加する
			if ( tuner == IterationType.PASSED ) {
				for ( TVProgram p : tvprograms ) {
					if (isProgramEnabled(p,tuner)) {
						int centerid=0;
						for ( ProgList pl : p.getCenters() ) {
							if ( ! proglist.contains(pl) ) {
								proglist.add(pl);
							}
							centerid++;
						}
					}
				}
			}
			
		}
		else {
			// 局順リストなし（後方互換）
			int centerid;
			for (int siteid=0; siteid<tvprograms.size(); siteid++) {
				TVProgram p = tvprograms.get(siteid);
				if (isProgramEnabled(p,tuner)) {
					for (Center cr : p.getSortedCRlist()) {
						if ((centerid = findCenterId(p.getCenters(),cr)) >= 0) {
							
							if ( ! isAreaCodeEnabled(p,cr,tuner)) {
								// エリアコードによる判定を追加
								continue;
							}
							
							proglist.add(p.getCenters().get(centerid));
						}
					}
				}
			}
		}
		
		return this;
	}
	
	private int findCenterId(ArrayList<ProgList> pl, Center cr) {
		for (int x=0; x<pl.size(); x++) {
			if (pl.get(x).enabled && pl.get(x).Center.equals(cr.getCenter())) {
				return x;
			}
		}
		return -1;
	}
	
	private boolean isProgramEnabled(TVProgram p, IterationType tuner) {
		if (p.getType() == ProgType.PROG) {
			if (tuner == IterationType.TERRA || tuner == IterationType.BS || tuner == IterationType.ALL) { 
				if (p.getSubtype() == ProgSubtype.TERRA) {
					return true;
				}
			}
			if (tuner == IterationType.CS || tuner == IterationType.ALL) {
				if (p.getSubtype() == ProgSubtype.TERRA || p.getSubtype() == ProgSubtype.CS || p.getSubtype() == ProgSubtype.CS2) {
					return true;
				}
			}
			if (tuner == IterationType.RADIO || tuner == IterationType.ALL) {
				if (p.getSubtype() == ProgSubtype.RADIO) {
					return true;
				}
			}
		}
		else if (p.getType() == ProgType.PASSED && tuner == IterationType.PASSED) {
			return true;
		}
		return false;
	}
	
	private boolean isAreaCodeEnabled(TVProgram p, Center cr, IterationType tuner) {
		if (tuner == IterationType.TERRA) {
			if (cr.getAreaCode().equals(TVProgram.bsCode) || cr.getAreaCode().equals(TVProgram.csCode)) {
				// TERRAとBSをわけてみたい
				return false;
			}
		}
		else if (tuner == IterationType.BS) {
			if ( ! cr.getAreaCode().equals(TVProgram.bsCode)) {
				// TERRAとBSをわけてみたい
				return false;
			}
		}
		else if (tuner == IterationType.CS) {
			if (p.getSubtype() == ProgSubtype.TERRA) {
				if ( ! cr.getAreaCode().equals(TVProgram.csCode)) {
					// EDCBみたいに、TERRAの中にTERRA/BS/CSが混在しているとか
					return false;
				}
			}
		}
		return true;
	}
}
