package epgdump;

public class TSpacket {

	private static final int TSPAYLOADMAX = 184;
	
	int		sync;
	int		transport_error_indicator;
	int		payload_unit_start_indicator;
	int		transport_priority;
	int		pid;
	int		transport_scrambling_control;
	int		adaptation_field_control;
	int		continuity_counter;
	int		adaptation_field;
	byte[]	payload = new byte[TSPAYLOADMAX+1];	// [鯛ナビ] 元ソースには+1しないといけないバグがあるっぽい
	int		payloadlen;
	int		rcount;

}
