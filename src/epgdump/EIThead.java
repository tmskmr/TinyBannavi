package epgdump;

public class EIThead {

	byte table_id;
	int section_syntax_indicator;
	int reserved_future_use;
	int reserved1;
	int section_length;
	int service_id;
	int reserved2;
	int version_number;
	int current_next_indicator;
	int section_number;
	int last_section_number;
	int transport_stream_id;
	int original_network_id;
	int segment_last_section_number;
	int last_table_id;

}
