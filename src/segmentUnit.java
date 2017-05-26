import java.io.Serializable;

public class segmentUnit implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3719879622339419127L;

	//该音频在原音频的开始时间
	long starttime;
	//该音频在原音频的结束时间
	long endtime;
	//该音频的标签，代表类别
	int label;
	
	public segmentUnit(){
		starttime = 0;
		endtime = 0;
		label = 0;
	}
	
	public segmentUnit(long starttime, long endtime, int label){
		this.starttime = starttime;
		this.endtime = endtime;
		this.label = label;
	}
	
}
