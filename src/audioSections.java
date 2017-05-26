import java.io.Serializable;
import java.util.ArrayList;

public class audioSections implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1287648616357703255L;
	
	//音频文件的路径
	private String path;
	//音频文件的切片数量
	private int numOfSegment;
	//没有输入起始时间和结束时间时，音频文件按照这个时间间隔切片
	private int timeInterval;
	//没有输入标签时，音频切片以此为标签
	private int label;
	//音频文件的切片
	ArrayList<segmentUnit> segments;
	
	public audioSections(){
		this.numOfSegment = 0;
		this.timeInterval = 5;
		this.label = 0;
		this.segments = new ArrayList<>();
	}
	
	public audioSections(String path, int timeInterval, int label){
		this.path = path;
		this.numOfSegment = 0;
		this.timeInterval = timeInterval;
		this.label = label;
		this.segments = new ArrayList<>();
	}
	
	public audioSections(String path, int timeInterval, int label, ArrayList<segmentUnit> segments){
		this.path = path;
		this.numOfSegment = segments.size();
		this.timeInterval = timeInterval;
		this.label = label;
		this.segments = segments;
	}
	
	public void AddSegment(segmentUnit sunit){
		this.segments.add(sunit);
		this.numOfSegment++;
	}
	
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public int getNumOfSegment() {
		return numOfSegment;
	}
	public void setNumOfSegment(int numOfSegment) {
		this.numOfSegment = numOfSegment;
	}
	public int getTimeInterval() {
		return timeInterval;
	}
	public void setTimeInterval(int timeInterval) {
		this.timeInterval = timeInterval;
	}
	public int getLabel() {
		return label;
	}
	public void setLabel(int label) {
		this.label = label;
	}
	
}
