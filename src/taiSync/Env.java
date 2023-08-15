package taiSync;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class Env {

	// 定数
	private static final String envFile = "env/taiSyncEnv.xml";
	
	
	// メンバ変数
	private boolean debug;
	private boolean appendDT;
	private int period;
	private int keeping;
	private int keepingCheckInterval;
	private int popPort;
	private int smtpPort;
	public void setDebug(boolean b) { debug = b; }
	public boolean getDebug() { return debug; }
	public void setAppendDT(boolean b) { appendDT = b; }
	public boolean getAppendDT() { return appendDT; }
	public void setPeriod(int n) { period = n; }
	public int getPeriod() { return period; }
	public void setKeeping(int n) { keeping = n; }
	public int getKeeping() { return keeping; }
	public void setKeepingCheckInterval(int n) { keepingCheckInterval = n; }
	public int getKeepingCheckInterval() { return keepingCheckInterval; }
	public void setPopPort(int n) { popPort = n; }
	public int getPopPort() { return popPort; }
	public void setSmtpPort(int n) { smtpPort = n; }
	public int getSmtpPort() { return smtpPort; }
	
	
	// 操作系メソッド
	public void save() {
		try {
			XMLEncoder enc = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(envFile)));
            enc.writeObject(this);
            enc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static Env load() {
		Env b = null;
        try {
            XMLDecoder dec = new XMLDecoder(new BufferedInputStream(new FileInputStream(envFile)));
            b = (Env)dec.readObject();
            dec.close();
        } catch(FileNotFoundException e) {
        	System.out.println("Exception: load()"+e);
        	b = new Env();
        }
    	return(b);
	}
	
	
	// コンストラクタ
	public Env() {
		this.setDebug(true);
		this.setAppendDT(true);
		this.setPeriod(7);
		this.setKeeping(5);
		this.setKeepingCheckInterval(3);
		this.setPopPort(110);
		this.setSmtpPort(25);
	}
}
