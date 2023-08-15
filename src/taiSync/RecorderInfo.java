package taiSync;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

import tainavi.EncryptPassword;
import tainavi.RecorderList;
import tainavi.b64;

public class RecorderInfo extends RecorderList {
	
	private int localPort = 80;
	public void setLocalPort(int p) { localPort = p; }
	public int getLocalPort() { return localPort; }
	
	private static String saveFile = "env/taiSyncRecInfo.xml";
	
	public static ArrayList<RecorderInfo> load() {
		File f = new File(saveFile);
		if (f.exists()) {
			try {
				XMLDecoder dec = new XMLDecoder(new BufferedInputStream(new FileInputStream(saveFile)));
	            ArrayList<RecorderInfo> a = (ArrayList<RecorderInfo>)dec.readObject();
	            dec.close();
	            for (RecorderInfo rec : a) {
	            	rec.setRecorderPasswd(EncryptPassword.dec(b64.dec(rec.getRecorderPasswd())));
	            }
	            return(a);
	        } catch(Exception e) {
	        	System.out.println("Exception: load recorder="+saveFile+"("+e.toString()+")");
	        }
		}
        return(new ArrayList<RecorderInfo>());
	}
	
	public static void save(ArrayList<RecorderInfo> a) {
        try {
			for (RecorderInfo rec : a) {
				rec.setRecorderPasswd(b64.enc(EncryptPassword.enc(rec.getRecorderPasswd())));
			}
            XMLEncoder enc = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(saveFile)));
            enc.writeObject(a);
            enc.close();
        } catch(FileNotFoundException e) {
        	System.out.println("Exception: save recorder="+saveFile);
        }
	}

}
