package httpDump;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main extends Thread {
	private String id = "";
	private String password = "";

	public Main(String id, String password) {
		this.id = id;
		this.password = password;
		//
		this.start();
	}

	@Override
	public void run() {
		Thread tr = new Thread();
		
		int port = 8080;
		ServerSocket svsock = null;
		Socket sock = null;
		
		//
		try {
			System.out.println("ブラウザからの接続をポート"+port+"で待ちます");
			svsock = new ServerSocket(port);
			
			while (true) {
				sock = svsock.accept();
			
				new Server(sock, id, password);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

}
