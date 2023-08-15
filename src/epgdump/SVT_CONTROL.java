package epgdump;

import java.util.ArrayList;

public class SVT_CONTROL {

	int		servive_id ;				// イベントID
	int		original_network_id ;	// OriginalNetworkID
	int		transport_stream_id ;	// TransporrtStreamID
	String	servicename ;			// サービス名

	boolean enabled = false;
	
	ArrayList<EIT_CONTROL> eittop = new ArrayList<EIT_CONTROL>();
	
	
	// XMLEncoder/Decoder用
	public void setServive_id(int d) { servive_id = d; }
	public int getServive_id() { return servive_id; }
	public void setOriginal_network_id(int d) { original_network_id = d; }
	public int getOriginal_network_id() { return original_network_id; }
	public void setTransport_stream_id(int d) { transport_stream_id = d; }
	public int getTransport_stream_id() { return transport_stream_id; }
	public void setServicename(String s) { servicename = s; }
	public String getServicename() { return servicename; }
	public void setEnabled(boolean b) { enabled = true; }
	public boolean getEnabled() { return enabled; }
	public void setEittop(ArrayList<EIT_CONTROL> a) { eittop = a; }
	public ArrayList<EIT_CONTROL> getEittop() { return eittop; }
}
