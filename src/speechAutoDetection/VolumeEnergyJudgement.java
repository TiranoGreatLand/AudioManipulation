package speechAutoDetection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import javax.sound.sampled.UnsupportedAudioFileException;

import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.InputFormatException;
import speechPreProcessing.AudioConvert;
import speechPreProcessing.audioSections;
import speechPreProcessing.segmentUnit;

public class VolumeEnergyJudgement {

	int timeSpan;                      //单位：毫秒
	int overlaptime;                  //单位：毫秒
 	int numOfAvgStc;               //取多少个timeSpan来做平均值
 	double avgStc;                   //多少个timeSpan的平均值
 	//这4个暂时通过计算得出
 	double pointThreshold;      //超过多少时判定为端点
 	double neoThreshold;        //超过多少时不为静音
 	double threshold3;             //一个中间阈值，判定是否出现较清晰的语音
 	double threshold4;             //某段声音和判定阈值之间差的绝对值超过多少时，视为可能的端点
 	
 	int silenceSpan;                  //一段静音需要超过多少毫秒
 	int framePerMs;                  //每毫秒多少单通道帧
	
 	int spanDist;                      //每段timeSpan和下一段之间的间隔
 	
 	double ratioOfThreshold;  
 	
 	wavData wavData;
 	ArrayList<Integer> pointTime;
 	
 	public VolumeEnergyJudgement(String wavpath) throws UnsupportedAudioFileException, IOException{
 		wavData = new wavData();
 		wavData.readDataFromWave(wavpath);
 		this.timeSpan = 20;
 		this.overlaptime = 0;
 		this.numOfAvgStc = 100;
 		this.avgStc = 0;
 		
 		this.framePerMs = wavData.samplingFrequency/1000;
 		this.silenceSpan = this.timeSpan*this.numOfAvgStc;
 		this.spanDist = this.timeSpan - this.overlaptime;
 		
 		this.ratioOfThreshold = 1.05;
 	}

 	public double DecibelValuePerSpan(int startframe, int endframe){
 		int end = endframe;
 		if(end>wavData.numOfFrame) end = (int) wavData.numOfFrame;
 		double ret = 0;
 		for(int i=startframe; i<end;i++){
 			for(int n=0;n<wavData.numOfChannel; n++){
 				ret += wavData.data[n][i]*wavData.data[n][i];
 			}
 		}
 		ret = 10 * Math.log10(ret+1);
 		return ret;
 	}
 	
 	public void pointDetection(){
 		System.out.println("start to compute decibel values");
 		int msOfTheWav = (int) (wavData.numOfFrame*1000/wavData.samplingFrequency);
 		int lenOfSpans = 1 + (int)((msOfTheWav-this.timeSpan)/this.spanDist);
 		System.out.println("there are " + lenOfSpans+" spans");
 		double[] DecibelsValues = new double[lenOfSpans];
 		for(int i=0;i<lenOfSpans;i++){
 	 		int startms = i * this.spanDist;
 	 		int endms = startms + this.timeSpan;
 	 		int startFrame = startms * this.framePerMs;
 	 		int endFrame = endms * this.framePerMs;
 			DecibelsValues[i] = DecibelValuePerSpan(startFrame, endFrame);
 		}
 		System.out.println("decibel values compute over");
 		ThresholdCompute(DecibelsValues, lenOfSpans);
 		PointCompute(DecibelsValues, lenOfSpans);
 	}
 	
 	public void ThresholdCompute(double[] DecibelsValues, int len){
 		System.out.println("start to compute thresholds");
 		double maxv = DecibelsValues[0];         // the max
 		double minv = DecibelsValues[0];           // the min 
 		for(int i=1;i<len;i++){
 			if(DecibelsValues[i]>maxv) maxv = DecibelsValues[i];
 			if(DecibelsValues[i]<minv) minv = DecibelsValues[i]; 
 		}
 		// 需要进一步计算threshold3
 		double valley = maxv - minv;
 		neoThreshold = minv + valley*0.1;          //静音阈值                    
 		pointThreshold = minv + valley*0.75;     //较清楚声音阈值       
 		threshold3 = minv + valley*0.45;            //不太清楚的声音     
 		threshold4 = valley * 0.4;
 		System.out.println("thresholds compute over");
 	}
 	
 	public void PointCompute(double[] DecibelValues, int len){
 		pointTime = new ArrayList<>();
 		pointTime.add(0);
 		for(int i=0 ; i<this.numOfAvgStc ; i++){
 			this.avgStc += DecibelValues[i];
 		}
 		for(int i=this.numOfAvgStc; i<len; i++){
 			double tmpv = this.avgStc/this.numOfAvgStc;
 			double absDiff = Math.abs(tmpv - DecibelValues[i]);
 			int lastpoint = pointTime.get(pointTime.size()-1);
 			//先不用三个threshold做判断
 			if(absDiff>threshold4 && (i-lastpoint)>this.numOfAvgStc){
 				pointTime.add(i);
 			}
 			this.avgStc = this.avgStc - DecibelValues[i-this.numOfAvgStc]+DecibelValues[i];
 		}
 		//暂时转化为低精度的秒，看看有多少个
 		Set<Integer> pointInSecond = new TreeSet<>();
 		for(int i : pointTime){
 			int tmps = (int)(this.spanDist*i/1000);
 			pointInSecond.add(tmps);
 		}
 		/*
 		System.out.println("there are "+pointInSecond.size()+" points that being detected");
 		for(int i: pointInSecond){
 			System.out.println(i);
 		}
 		*/
 		ArrayList<Integer> seconds = new ArrayList<>();
 		seconds.addAll(pointInSecond);
 		seconds.add((int)(len*this.spanDist/1000));
 		fromPointToUtter(seconds);
 	}
 	
 	public void fromPointToUtter(ArrayList<Integer> pointInSecond){
 		pointDetectionResult = new audioSections();
 		int len = pointInSecond.size();
 		for(int i=1;i<len;i++){
 			segmentUnit tmpsu = new segmentUnit(pointInSecond.get(i-1), pointInSecond.get(i), 0);
 			pointDetectionResult.AddSegment(tmpsu);
 		}
 	}
 	
 	audioSections pointDetectionResult;
 	
 	public void SpeechAutoSegment(String inputpath, String outputfilepath) throws InputFormatException, EncoderException, IOException, UnsupportedAudioFileException{
 		AudioConvert tmpac = new AudioConvert();
 		pointDetection();
 		for(segmentUnit su : pointDetectionResult.segments){
 			String outpath = outputfilepath+"\\"+"tmp_"+su.starttime+"_"+su.endtime+".wav";
 			tmpac.audioSegmentation(inputpath, outpath, su.starttime, su.endtime);
 			System.out.println(outpath+" segmented and saved");
 		}
 	}
 	
	public static void main(String[] args) throws UnsupportedAudioFileException, IOException, InputFormatException, EncoderException {
		String inputpath = "E:\\QQfiles\\342479958\\FileRecv\\动能势能2_201705139142000.wav";
		//wavData data = new wavData();
		//data.readDataFromWave(inputpath);
		//System.out.println(data.len);
		//System.out.println(data.data.length);
		//System.out.println(data.numOfFrame);
		//System.out.println(data.samplingSize);
		//System.out.println(data.data[0][data.len-1]);
		String outputfilepath = "D:/DataGames/tmpvoiceseg";
		VolumeEnergyJudgement vej = new VolumeEnergyJudgement(inputpath);
		vej.SpeechAutoSegment(inputpath, outputfilepath);
		
		//System.out.println("max decibel value: " + vej.pointThreshold);
		//System.out.println("min decibel value:" + vej.neoThreshold);

	}
	
}
