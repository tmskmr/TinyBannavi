package tainavi;

/**
 * <P>個々のデバイスの情報を保持します。
 */
public class DeviceInfo implements Cloneable {
	private String id="";
	private String name="";
	private String type="";
	private boolean playlistEnable;
	private boolean folderEnable;
	private int allsize;
	private int freesize;
	private int freemin;
	private boolean connected;
	private boolean canwrite;
	private boolean isprotected;
	private boolean mounted;
	private boolean ready;
	private int formatType;

	@Override
	public DeviceInfo clone() {
		try {
			DeviceInfo p = (DeviceInfo) super.clone();
			return p;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}

	public String getId(){ return id; }
	public void setId(String s){ id = s; }

	public String getName() {return name;}
	public void setName(String s){ name = s;}

	public String getType(){ return type; }
	public void setType(String s){ type = s; }

	public boolean getPlaylistEnable(){ return playlistEnable; }
	public void setPlaylistEnable(boolean b){ playlistEnable = b; }

	public boolean getFolderEnable(){ return folderEnable; }
	public void setFolderEnable(boolean b){ folderEnable = b; }

	public int getAllSize(){ return allsize; }
	public void setAllSize(int n){ allsize = n; }

	public int getFreeSize(){ return freesize; }
	public void setFreeSize(int n){ freesize = n; }

	public int getFreeMin(){ return freemin; }
	public void setFreeMin(int n){ freemin = n; }

	public boolean getConnected(){ return connected; }
	public void setConnected(boolean b){ connected = b; }

	public boolean getCanWrite(){ return canwrite; }
	public void setCanWrite(boolean b){ canwrite = b; }

	public boolean getProtected(){ return isprotected; }
	public void setProtected(boolean b){ isprotected = b; }

	public boolean getMounted(){ return mounted; }
	public void setMounted(boolean b){ mounted = b; }

	public boolean getReady(){ return ready; }
	public void setReady(boolean b){ ready = b; }

	public int getFormatType(){ return formatType; }
	public void setFormatType(int n){ formatType = n; }
}
