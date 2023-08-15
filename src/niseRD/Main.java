package niseRD;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main extends Thread {
	private String folder = null;

	public Main(String folder) {
		this.folder = folder;
		//
		this.start();
	}

	@Override
	public void run() {
		int port = 8080;
		ServerSocket svsock = null;
		Socket sock = null;
		
		//
		try {
			System.out.println("鯛ナビからの接続をポート"+port+"で待ちます");
			svsock = new ServerSocket(port);
			
			while (true) {
				sock = svsock.accept();
			
				new Server(sock, folder);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

}
