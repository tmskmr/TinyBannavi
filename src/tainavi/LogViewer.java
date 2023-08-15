package tainavi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


public class LogViewer extends JDialog {

	private static final long serialVersionUID = 1L;

	JScrollPane jpane = null;
	JTextArea jtext = null;
	
	public LogViewer(String logfile) {
		
		super();
		
		this.setTitle("LogViewer - "+logfile);
		jtext = new JTextArea(25,60);
		jtext.setEditable(false);
		this.setContentPane(jpane = new JScrollPane(jtext));
		this.pack();
		
		try {
			File f = new File(logfile);
			BufferedReader r = new BufferedReader(new FileReader(f));
			StringBuilder sb = new StringBuilder();
			String msg;
			while ((msg = r.readLine()) != null) {
				sb.append(msg);
				sb.append("\n");
			}
			r.close();
			jtext.setText(sb.toString());
			jtext.setCaretPosition(jtext.getText().length());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setCaretPosition(int pos) {
		jtext.setCaretPosition(pos);
	}
	
	public void setModal(boolean b) {
		super.setModal(b);
	}
}
