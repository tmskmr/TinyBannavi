package tainavi;

import java.util.ArrayList;

import tainavi.TVProgram.ProgFlags;
import tainavi.TVProgram.ProgGenre;
import tainavi.TVProgram.ProgOption;
import tainavi.TVProgram.ProgScrumble;
import tainavi.TVProgram.ProgSubgenre;

/**
 * 過去ログ保存のTXT化で不要になったが、古いXML形式を読むためには必要
 * @deprecated
 */
public class PassedProgramList extends ProgDetailList {

	/*
	 * for SAVE/LOAD用のgetter・setter
	 */
	
	public void setTitle(String s) { this.title=s; }
	public String getTitle() { return this.title; }
	//public void setTitlePop(String s) {}
	//public String getTitlePop() { return TraceProgram.replacePop(this.title); }
	public void setDetail(String s) { this.detail=s; }
	public String getDetail() { return this.detail; }
	//public void setDetailPop(String s) {}
	//public String getDetailPop() { return TraceProgram.replacePop(this.detail); }
	@Override
	public void setAddedDetail(String s) { this.addedDetail=s; }
	@Override
	public String getAddedDetail() { return this.addedDetail; }
	public void setStart(String s) { this.start=s; }
	public String getStart() { return this.start; }
	public void setEnd(String s) { this.end=s; }
	public String getEnd() { return this.end; }
	//public void setStartDateTime(String s) { this.startDateTime=s; }
	//public String getStartDateTime() { return this.startDateTime; }
	//public void setEndDateTime(String s) { this.endDateTime=s; }
	//public String getEndDateTime() { return this.endDateTime; }
	//public void setLink(String s) { this.link=s; }
	//public String getLink() { return this.link; }
	public void setLength(int s) { this.length=s; }
	public int getLength() { return this.length; }
	public void setExtension(boolean s) { this.extension=s; }
	public boolean getExtension() { return this.extension; }
	//public void setReserved(boolean s) { this.reserved=s; }
	//public boolean getReserved() { return this.reserved; }
	public void setNoscrumble(ProgScrumble noscrumble) { this.noscrumble=noscrumble; }
	public ProgScrumble getNoscrumble() { return this.noscrumble; }
	public void setGenre(ProgGenre genre) { this.genre=genre; }
	public ProgGenre getGenre() { return this.genre; }
	public void setSubgenre(ProgSubgenre subgenre) { this.subgenre=subgenre; }
	public ProgSubgenre getSubgenre() { return this.subgenre; }
	public void setFlag(ProgFlags flag) { this.flag=flag; }
	public ProgFlags getFlag() { return this.flag; }
	public void setOption(ArrayList<ProgOption> option) { this.option=option; }
	@Override
	public ArrayList<ProgOption> getOption() { return this.option; }
}
