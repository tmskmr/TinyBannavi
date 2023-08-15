package tainavi;

/**
 * <P>個々のチャプターの情報を保持します。
 */
public class ChapterInfo implements Cloneable {
	private String name="";
	private int duration=0;
	private boolean changeFlag=false;

	@Override
	public ChapterInfo clone() {
		try {
			ChapterInfo p = (ChapterInfo) super.clone();
			return p;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}

	public String getName() {return name;}
	public void setName(String s) { name=s;}

	public int getDuration() {return duration;}
	public void setDuration(int d) { duration=d;}

	public boolean getChangeFlag() {return changeFlag;}
	public void setChangeFlag(boolean b) { changeFlag=b;}
}
