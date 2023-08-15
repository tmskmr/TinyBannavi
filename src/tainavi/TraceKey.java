package tainavi;

import java.util.ArrayList;

public class TraceKey implements SearchItem {
	
	public static final int defaultFazzyThreshold = 35;
	public static final int noFazzyThreshold = 0;
	
	private String label = null;
	private String title = null;
	private String titlePop = null;
	private String center = null;
	private int fazzyThreshold = 0;
	private String okiniiri = null;
	private boolean disableRepeat = false;
	private boolean showLatestOnly = false;

	private ArrayList<ProgDetailList> _matched = null;
	
	public void setTitle(String s) { title = s; }
	public String getTitle() { return title; }
	public void setCenter(String s) { center = s; }
	public String getCenter() { return center; }
	public void setFazzyThreshold(int n) { fazzyThreshold = n; }
	public int getFazzyThreshold() { return fazzyThreshold; };
	public void setOkiniiri(String s) { okiniiri = s; }
	public String getOkiniiri() { return okiniiri; }
	public void setDisableRepeat(boolean b) { disableRepeat = b; }
	public boolean getDisableRepeat() { return disableRepeat; }
	public void setShowLatestOnly(boolean b) { showLatestOnly = b; }
	public boolean getShowLatestOnly() { return showLatestOnly; }

	// ファイルに保存させない連中
	public void setLabel(String s) { label = s; }
	public String _getLabel() { return label; }
	public void setTitlePop(String s) { titlePop = s; }
	public String _getTitlePop() { return titlePop; }

	// interface
	
	@Override
	public String toString() { return label; }
	
	@Override
	public void clearMatchedList() { _matched = new ArrayList<ProgDetailList>(); }
	@Override
	public void addMatchedList(ProgDetailList pdl) { _matched.add(pdl); }
	@Override
	public ArrayList<ProgDetailList> getMatchedList() { return _matched; }
	@Override
	public boolean isMatched() { return _matched.size() != 0; }
}
