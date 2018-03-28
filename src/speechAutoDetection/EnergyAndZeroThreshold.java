package speechAutoDetection;

import java.io.IOException;
import java.util.ArrayList;

import javax.sound.sampled.UnsupportedAudioFileException;

import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.InputFormatException;
import speechPreProcessing.AudioConvert;
import speechPreProcessing.audioSections;
import speechPreProcessing.segmentUnit;

public class EnergyAndZeroThreshold {

	int timeSpan;                      //单位：毫秒
	int overlaptime;                  //单位：毫秒
 	int numOfAvgStc;               //取多少个timeSpan来做平均值
 	double avgStc;                   //多少个timeSpan的平均值
 	//这4个暂时通过计算得出
 	double pointThreshold;      //超过多少时判定为端点
 	double neoThreshold;        //超过多少时不为静音
 	double threshold3;             //一个中间阈值，判定是否出现较清晰的语音
 	double threshold4;             //某段声音和判定阈值之间差的绝对值超过多少时，视为可能的端点
 	
 	//新加入的用于短时窗口计算的
 	double swThresholrd1;
 	double swThresholrd2;
 	double swThresholrd3;
 	
 	int silenceSpan;                  //一段静音需要超过多少毫秒
 	int framePerMs;                  //每毫秒多少单通道帧
	
 	int spanDist;                      //每段timeSpan和下一段之间的间隔
 	
 	double ratioOfThreshold;  
 	
 	wavData wavData;
 	ArrayList<Integer> pointTime;
 	
 	public EnergyAndZeroThreshold(String wavpath) throws UnsupportedAudioFileException, IOException{
 		wavData = new wavData();
 		wavData.readDataFromWave(wavpath);
 		this.timeSpan = 20;
 		this.overlaptime = 10;
 		this.numOfAvgStc = 100;
 		this.avgStc = 0;
 		
 		this.framePerMs = wavData.samplingFrequency/1000;
 		this.silenceSpan = this.timeSpan*this.numOfAvgStc;
 		this.spanDist = this.timeSpan - this.overlaptime;
 		
 		this.ratioOfThreshold = 1.05;
 	}
 	
 	/*
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
 	*/

 	public void energyNormalization(){
 		sampleEnergy = new double[wavData.len];
 		zeroCross = new double[wavData.len];
 		for(int i=0;i<wavData.numOfChannel;i++){
 			sampleEnergy[0] += wavData.data[i][0];
 		}
 		
 		if(sampleEnergy[0]>=0) zeroCross[0] = 1;
 		else zeroCross[0] = -1;
 		
 		sampleEnergy[0] = Math.abs(sampleEnergy[0]);
 		
 		double biggest = sampleEnergy[0];
 		double smallest = sampleEnergy[0];
 		
 		for(int i=1;i<wavData.len;i++){
 			for(int j=0;j<wavData.numOfChannel;j++){
 				sampleEnergy[i] += wavData.data[j][i];
 			}
 			
 			if(sampleEnergy[i]>=0) zeroCross[i] = 1;
 			else zeroCross[i] = 0;
 			
 			sampleEnergy[i] = Math.abs(sampleEnergy[i]);
 			
 			if(sampleEnergy[i]>biggest) biggest = sampleEnergy[i];
 			if(sampleEnergy[i]<smallest) smallest = sampleEnergy[i];
 		}
 		System.out.println(biggest);
 		System.out.println(smallest);
 		//归一化，选择 (x-smallest)/(biggest-smallest) 而非 x/biggest
 		double divide = biggest - smallest;
 		for(int i=0;i<wavData.len;i++){
 			sampleEnergy[i] = (sampleEnergy[i]-smallest)/divide;
 		}
 	}
 	
 	double[] sampleEnergy;
 	double[] zeroCross;          //暂时留着，需要用的话拿出来
 	
 	double[] roughWindowEnergy;
 	double[] shortWindowEnergy;
 	
 	public void pointDetection(){
 		System.out.println("start to compute decibel values");
 		int msOfTheWav = (int) (wavData.numOfFrame*1000/wavData.samplingFrequency);
 		//会有两个timeSpan
 		int lenOfSpans = 1 + (int)((msOfTheWav-this.timeSpan)/this.spanDist);
 		roughWindowEnergy = new double[lenOfSpans];
 		//计算另一个短时timeSpan
 		int anotherlos = 1+(int)((msOfTheWav-this.timeSpan/2)/(this.spanDist/2));
 		shortWindowEnergy = new double[anotherlos];
 		
 		System.out.println("there are " + lenOfSpans+" long spans");
 		System.out.println("there are " + anotherlos+" short spans");
 		energyNormalization();
 		energyWindowCompute(lenOfSpans-1, anotherlos-1);
 		PointDetectRough(lenOfSpans-1);
 		
 		fromPointToUtter(roughPoint);
 	}
 	
 	public void energyWindowCompute(int longspan, int shortspan){
 		double max = 0, min = 1.0;
 		for(int i=0;i<longspan;i++){
 			int startms = this.spanDist * i;
 			int endms = startms + this.timeSpan;
 			int startSampling = startms * this.framePerMs;
 			int endSampling = endms * this.framePerMs;
 			for(int j=startSampling; j<endSampling;j++){
 				roughWindowEnergy[i] += sampleEnergy[j];
 			}
 			if(max < roughWindowEnergy[i]) max = roughWindowEnergy[i];
 			if(min > roughWindowEnergy[i]) min = roughWindowEnergy[i];
 		}
 		
 		double valley = max - min;
 		neoThreshold = min + valley*0.1;          //静音阈值                    
 		pointThreshold = min + valley*0.7;     //较清楚声音阈值       
 		threshold3 = min + valley*0.4;            //不太清楚的声音    
 		threshold4 = 0.4 * valley;
 		
 		int shortdist = this.spanDist/2;
 		int shorttimespan = this.timeSpan/2;
 		max = 0;
 		min = 1.0;
 		for(int i=0;i<shortspan;i++){
 			int startms = shortdist * i;
 			int endms = startms + shorttimespan;
 			int startSampling = startms * this.framePerMs;
 			int endSampling = endms * this.framePerMs;
 			for(int j=startSampling; j<endSampling;j++){
 				shortWindowEnergy[i] += sampleEnergy[j];
 			}
 			if(max < shortWindowEnergy[i]) max = shortWindowEnergy[i];
 			if(min > shortWindowEnergy[i]) min = shortWindowEnergy[i];
 		}
 		valley = max - min;
 		swThresholrd1 = min +0.1*valley;
 		swThresholrd2 = min + 0.7*valley;
 		swThresholrd3 = min + 0.4*valley;

 	}

 	ArrayList<Integer> roughPoint;
 	
 	public void PointDetectRough(int longspan){
 		roughPoint = new ArrayList<>();
 		roughPoint.add(0);
 		double accValue = 0;
 		for(int i=0; i<this.numOfAvgStc;i++){
 			accValue += roughWindowEnergy[i];
 		}
 		
 		for(int i=this.numOfAvgStc; i<longspan; i++){
 			double avg = accValue/this.numOfAvgStc;
 			if((roughWindowEnergy[i]-avg>threshold4) && roughWindowEnergy[i]>=pointThreshold){
 				roughPoint.add(i);
 			}
 			else if((avg - roughWindowEnergy[i]>threshold4) && roughWindowEnergy[i]<=threshold3){
 				roughPoint.add(i);
 			}
 			accValue = accValue + roughWindowEnergy[i] - roughWindowEnergy[i-this.numOfAvgStc];
 		}
 		
 	}
 	
 	audioSections pointDetectionResult;
 	
 	public void fromPointToUtter(ArrayList<Integer> pointInSecond){
 		
 		//删除时间间隔过短的点
 		ArrayList<Integer> secondPoints = new ArrayList<>();
 		int lastIndex = 0;
 		this.silenceSpan = 2 * this.silenceSpan / 1000;
 		secondPoints.add(pointInSecond.get(0));
 		int slen = pointInSecond.size();
 		for(int i=1;i<slen;i++){
 			int tmpx = pointInSecond.get(i);
 			if((tmpx - pointInSecond.get(lastIndex))>this.silenceSpan){
 				secondPoints.add(tmpx);
 				lastIndex = i;
 			}
 		}
 		
 		pointDetectionResult = new audioSections();
 		int len = secondPoints.size();
 		for(int i=1;i<len;i++){
 			segmentUnit tmpsu = new segmentUnit(secondPoints.get(i-1), secondPoints.get(i), 0);
 			pointDetectionResult.AddSegment(tmpsu);
 		}
 	}
 	
 	public void SpeechAutoSegment(String inputpath, String outputfilepath) throws InputFormatException, EncoderException, IOException, UnsupportedAudioFileException{
 		AudioConvert tmpac = new AudioConvert();
 		pointDetection();
 		for(segmentUnit su : pointDetectionResult.segments){
 			String outpath = outputfilepath+"\\"+"tmp_"+su.starttime+"_"+su.endtime+".wav";
 			tmpac.audioSegmentation(inputpath, outpath, su.starttime, su.endtime);
 			System.out.println(outpath+" segmented and saved");
 		}
 	}
	
 	public static void main(String[] args) throws UnsupportedAudioFileException, IOException, InputFormatException, EncoderException{
 		String inputpath = "D:\\DataGames\\childvoice\\动能势能2_201705139142000.wav";
 		String outputfilepath = "D:/DataGames/tmpvoiceseg";
 		EnergyAndZeroThreshold ez = new EnergyAndZeroThreshold(inputpath);
 		ez.pointDetection();
 		ez.SpeechAutoSegment(inputpath, outputfilepath);
 	}
 	
}
