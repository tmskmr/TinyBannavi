package epgdump;

public class SeriesDesc {

	int descriptor_tag;
	int descriptor_length;
	int series_id;
	int repeat_label;
	int program_pattern;
	int expire_date_valid_flag;
	int expire_date;
	int episode_number;
	int last_episode_number;
	byte[] series_name_char = new byte[Util.MAXSECLEN];

}
