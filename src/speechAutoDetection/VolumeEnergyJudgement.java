package speechAutoDetection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.ws.handler.MessageContext.Scope;

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
 		this.overlaptime = 10;
 		this.numOfAvgStc = 100;
 		this.avgStc = 0;
 		
 		this.framePerMs = wavData.samplingFrequency/1000;
 		this.silenceSpan = this.timeSpan*this.numOfAvgStc;
 		this.spanDist = this.timeSpan - this.overlaptime;
 		
 		this.ratioOfThreshold = 0.9;
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
 	
 	double[] DecibelsValues;
 	double[] audioRegion;
 	
 	int lenOfSpans;
 	
 	public void pointDetection(){
 		System.out.println("start to compute decibel values");
 		int msOfTheWav = (int) (wavData.numOfFrame*1000/wavData.samplingFrequency);
 		lenOfSpans = 1 + (int)((msOfTheWav-this.timeSpan)/this.spanDist);
 		System.out.println("there are " + lenOfSpans+" spans");
 		DecibelsValues = new double[lenOfSpans];
 		audioRegion = new double[lenOfSpans];
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
 		neoThreshold = minv + valley*0.2;          //静音阈值                    
 		pointThreshold = minv + valley*0.7;     //较清楚声音阈值       
 		threshold3 = minv + valley*0.45;            //不太清楚的声音     
 		threshold4 = valley * 0.4;
 		System.out.println("thresholds compute over");
 		for(int i=0;i<len;i++){
 			if(DecibelsValues[i]>pointThreshold) audioRegion[i] = 3;
 			else if(DecibelsValues[i]<=pointThreshold && DecibelsValues[i]>threshold3) audioRegion[i] = 2;
 			else if(DecibelsValues[i]<=threshold3 && DecibelsValues[i]>neoThreshold) audioRegion[i] = 1;
 		}
 	}
 
 	/*
 	public ArrayList<Integer> counterPointCompute(double[] DecibelValues, int len){
 		ArrayList<Integer> tmpa = new ArrayList<>();
 		//tmpa.add((int)(len*this.spanDist/1000));
 		this.avgStc = 0;
 		for(int i=0 ; i<this.numOfAvgStc ; i++){
 			this.avgStc += DecibelValues[len-1-i];
 		}
 		for(int i=len-1-this.numOfAvgStc; i>=0; i--){
 			double tmpv = this.avgStc/this.numOfAvgStc;
 			double absDiff = Math.abs(tmpv - DecibelValues[i]);
 			int lastpoint = pointTime.get(pointTime.size()-1);
 			//先不用三个threshold做判断
 			if(absDiff>threshold4 && Math.abs(lastpoint-i)>this.numOfAvgStc){
 				tmpa.add(i);
 			}
 			this.avgStc = this.avgStc - DecibelValues[i+this.numOfAvgStc]+DecibelValues[i];
 		}
 		return tmpa;
 	}
*/
 	
 	public void PointCompute(double[] DecibelValues, int len){
 		pointTime = new ArrayList<>();
 		pointTime.add(0);
 		this.avgStc = 0;
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
 		ArrayList<Integer> counterPoint = counterPointCompute(DecibelValues, len);
 		for(int i : counterPoint){
 			int tmps = (int)(this.spanDist*i/1000);
 			pointInSecond.add(tmps);
 		} */
 		
 		System.out.println("there are "+pointInSecond.size()+" points that being detected");
 		/*
 		for(int i: pointInSecond){
 			System.out.println(i);
 		}
 		*/
 		ArrayList<Integer> seconds = new ArrayList<>();
 		seconds.addAll(pointInSecond);
 		seconds.add((int)(len*this.spanDist/1000));
 		
 		//删除间隔过短的点
 		ArrayList<Integer> secondPoints = new ArrayList<>();
 		int lastIndex = 0;
 		this.silenceSpan = 2 * this.silenceSpan / 1000;
 		secondPoints.add(seconds.get(0));
 		int slen = seconds.size();
 		for(int i=1;i<slen;i++){
 			int tmpx = seconds.get(i);
 			if((tmpx - seconds.get(lastIndex))>this.silenceSpan){
 				secondPoints.add(tmpx);
 				lastIndex = i;
 			}
 		}
 		
 		/*
 		//ArrayList<Integer> tmpaaa = mainSoundSegment(secondPoints);
 		secondPoints = mainSoundSegment(secondPoints);
 		System.out.println("there are " + secondPoints.size()+" points");
 		
 		//看区间能量
 		int spsize = secondPoints.size();
 		for(int i=1;i<spsize;i++){
 			int starttime = secondPoints.get(i-1), endtime = secondPoints.get(i);
 			int startvp = starttime*1000/this.spanDist, endvp = endtime*1000/this.spanDist;
 			double energy = 0;
 			for(int j=startvp; j<endvp;j++){
 				energy += DecibelValues[j];
 			}
 			System.out.println(energy/(endvp-startvp));
 		}
 		*/
 		System.out.println(secondPoints.size());
 		secondPoints = zeroCross(secondPoints);
 		System.out.println(secondPoints.size());
 		secondPoints = energyBackward(secondPoints);
 		System.out.println(secondPoints.size());
 		fromPointToUtter(secondPoints);
 	}
 	
 	public ArrayList<Integer> energyBackward(ArrayList<Integer> points){
 		ArrayList<Integer> ret = new ArrayList<>();
 		Set<Integer> tmpret = new TreeSet<>();
 		int shortspan =this.numOfAvgStc;
 		double shortthreshold = threshold4*this.ratioOfThreshold;
 		int len = points.size();
 		for(int i=1;i<len;i++){
 			int starttime = points.get(i-1), endtime = points.get(i);
 			if((endtime-starttime)<=2*this.silenceSpan/1000*2) {
 				tmpret.add(starttime);
 				tmpret.add(endtime);
 			}
 			else{
 				ArrayList<Integer> tmpp = new ArrayList<>();
 				tmpp.add(endtime);
 				int startp = starttime*1000/this.spanDist, endp = endtime*1000/this.spanDist;
 				double acc = 0;
 				for(int j=0;j<shortspan;j++){
 					acc += DecibelsValues[endp-j];
 				}
 				for(int j=endp-shortspan; j>=startp;j--){
 					double thr = acc/shortspan;
 					acc = acc + DecibelsValues[j] - DecibelsValues[j + shortspan];
 					if(Math.abs(DecibelsValues[j] - thr) > shortthreshold){
 						int tmpt = (int)(this.spanDist*j/1000);
 						if((tmpp.get(tmpp.size()-1) - tmpt) >= 4 && (tmpt - starttime)>=4){
 							tmpp.add(tmpt);
 						}
 					}
 				}
 				tmpp.add(starttime);
 				tmpret.addAll(tmpp);
 			}
 		}
 		ret.addAll(tmpret);
 		return ret;
 	}
 	
 	public ArrayList<Integer> zeroCross(ArrayList<Integer> points){
 		ArrayList<Integer> ret = new ArrayList<>();
 		Set<Integer> tmpret = new TreeSet<>();
 		int[] crosszero = new int[wavData.len];
 		double[] czVls = new double[lenOfSpans];
 		for(int i=0;i<wavData.len;i++){
 			double v = 0;
 			for(int n=0;n<wavData.numOfChannel;n++){
 				v += wavData.data[n][i];
 			}
 			if(v>=0) crosszero[i] = 1;
 			else crosszero[i] = -1;
 		}
 		int tmplen = lenOfSpans-1;
 		double maxcv = 0, mincv = 2 * this.timeSpan * this.framePerMs;
 		for(int i=0;i<tmplen;i++){
 			int startSampling = i*this.spanDist*this.framePerMs+1;
 			int endSampling = (i*this.spanDist+this.timeSpan)*this.framePerMs+1;
 			for(int j=startSampling; j<endSampling; j++){
 				czVls[i] += Math.abs(crosszero[j] - crosszero[j-1]);
 			}
 			czVls[i] /= 2;
 			if(czVls[i]>maxcv) maxcv = czVls[i];
 			if(czVls[i]<mincv) mincv = czVls[i];
 		}
 		
 		double czdist = (maxcv - mincv) * 0.4;
 		
 		int len = points.size();
 		for(int i=1;i<len;i++){
 			//以采样点为单位
 			int starttime = points.get(i-1);
 			int endtime = points.get(i);
 			if(endtime-starttime<=4) {
 				tmpret.add(starttime);
 				tmpret.add(endtime);
 			}
 			else if(endtime - starttime>20){
 				ArrayList<Integer> segmented = splitByEnergyAgain(starttime, endtime);
 				int sdlen = segmented.size();
 				for(int j=1;j<sdlen;j++){
 					int st = segmented.get(j-1), et = segmented.get(j);
 					ArrayList<Integer> zcseg = backwardZeroCross(czVls, st, et, czdist);
 	 				tmpret.addAll(zcseg);
 				}
 			} 
 			else{
 				ArrayList<Integer> zcseg = backwardZeroCross(czVls, starttime, endtime, czdist);
 				tmpret.addAll(zcseg);
 			}
 		}
 		ret.addAll(tmpret);
 		return ret;
 	}
 	
 	//返回一个倒序时间
 	public ArrayList<Integer> backwardZeroCross(double[] zc, int starttime, int endtime, double czdist){
 		ArrayList<Integer> ret = new ArrayList<>();
 		ret.add(endtime);
 		int startp = starttime*1000/this.spanDist;
 		int endp = endtime*1000/this.spanDist;
 		
 		double accv = 0;
 		for(int i=0;i<this.numOfAvgStc; i++){
 			accv += zc[endp-i];
 		}
 		
 		for(int i = endp-this.numOfAvgStc;i>=startp; i--){
 			double thr = accv/this.numOfAvgStc;
 			accv = accv + zc[i] - zc[i+this.numOfAvgStc];
 			if(Math.abs(thr - zc[i]) > czdist){
 				int timeInS = (int)(this.spanDist*i/1000);
 				if((ret.get(ret.size()-1) - timeInS)>=this.silenceSpan/1000 && (timeInS - starttime)>=this.silenceSpan/1000){
 					ret.add(timeInS);
 				}
 			}
 		}
 		ret.add(starttime);
 		return ret;
 	}
 	
 	public ArrayList<Integer> splitByEnergyAgain(int starttime, int endtime){
 		ArrayList<Integer> ret = new ArrayList<>();
 		ret.add(starttime);
 		int startSampling = starttime*1000/this.spanDist;
 		int endSampling = endtime*1000/this.spanDist;
 		double accv = 0;
 		for(int i=0;i<this.numOfAvgStc;i++){
 			accv += DecibelsValues[startSampling+i];
 		}
 		for(int i=startSampling+this.numOfAvgStc;i<endSampling;i++){
 			double thr = accv/this.numOfAvgStc;
 			accv = accv + DecibelsValues[i] - DecibelsValues[i-this.numOfAvgStc];
 			if(Math.abs(DecibelsValues[i]-thr)>this.threshold4){
 				int timeInS = (int)(this.spanDist*i/1000);
 				if(timeInS - ret.get(ret.size()-1)>=this.silenceSpan/1000 && (endtime - timeInS)>=this.silenceSpan/1000){
 					ret.add(timeInS);
 				}
 			}
 		}
 		ret.add(endtime);
 		return ret;
 	}
 	
 	public ArrayList<Integer> mainSoundSegment(ArrayList<Integer> secondpoints){
 		int len = secondpoints.size();
 		if(len<2) return secondpoints;
 		//int shortspan = this.numOfAvgStc/2;
 		Set<Integer> points = new TreeSet<>();
 		for(int i=1;i<len;i++){
 			Set<Integer> tmpss = new TreeSet<>();
 			//以秒为单位
 			int starttime = secondpoints.get(i-1), endtime = secondpoints.get(i);
 			tmpss.add(starttime);
 			tmpss.add(endtime);
 			//second to energy position
 			int startvp = starttime*1000/this.spanDist, endvp = endtime*1000/this.spanDist;
 			/*
 			if(i<10){
 				for(int j=startvp;j<endvp;j++){
 					System.out.print((int)audioRegion[j]+" ");
 				}
 				System.out.println();
 			}
 			*/
 			int lasti = endvp,  over1 = 0, below1 = 0;
 			while(endvp>startvp && audioRegion[endvp]>1) endvp--;
 			if(endvp-startvp>this.numOfAvgStc*2 && lasti-endvp>this.numOfAvgStc*2){
 				int tmptp = (int)(this.spanDist*endvp/1000);
 				tmpss.add(tmptp);
 			}
 			lasti = endvp;
 			int las = 0;         //0表示上衣切割的是较安静的声音
 			for(int j=endvp; j>=startvp;j--){
 				if(audioRegion[j]>1){
 					over1++;
 					below1 = 0;
 					if(over1>5 && las==0){
 						las = 1;
 						int tos = j + 5;
 		 				int tmptp = (int)(this.spanDist*tos/1000);
 		 				tmpss.add(tmptp);
 		 				//break;
 					}
 				}else{
 					over1 = 0;
 					below1++;
 					if(below1>5 && las==1){
 						las = 0;
 						int tos = j + 5;
 		 				int tmptp = (int)(this.spanDist*tos/1000);
 		 				tmpss.add(tmptp);
 		 				//break;
 					}
 					
 				}
 			}
 			ArrayList<Integer> tmpal = new ArrayList<>();
 			int tmpsl = tmpss.size();
 			ArrayList<Integer> tmpnum = new ArrayList<>();
 			tmpnum.addAll(tmpss);
 			int lastin = 0;
 			tmpal.add(tmpnum.get(lastin));
 			for(int k=1;k<tmpsl;k++){
 				int tmpp = tmpnum.get(k);
 				if(tmpp - tmpnum.get(lastin)>this.silenceSpan/1000){
 					tmpal.add(tmpp);
 					lastin = k;
 				}
 			}
 			points.addAll(tmpal);
 		}
 		ArrayList<Integer> ret = new ArrayList<>();
 		ret.addAll(points);
 		return ret;
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
 			//System.out.println(outpath+" segmented and saved");
 		}
 	}
 	
	public static void main(String[] args) throws UnsupportedAudioFileException, IOException, InputFormatException, EncoderException {
		String inputpath = "D:\\DataGames\\childvoice\\历史1.wav";
		//wavData data = new wavData();
		//data.readDataFromWave(inputpath);
		//System.out.println(data.len);
		//System.out.println(data.data.length);
		//System.out.println(data.numOfFrame);
		//System.out.println(data.samplingSize);
		//System.out.println(data.data[0][data.len-1]);
		
		//AudioConvert tmpac = new AudioConvert();
		//String path = tmpac.audioFormatChange(inputpath);
		
		String outputfilepath = "D:/DataGames/tmpvoiceseg";
		VolumeEnergyJudgement vej = new VolumeEnergyJudgement(inputpath);
		vej.SpeechAutoSegment(inputpath, outputfilepath);
		
		//System.out.println("max decibel value: " + vej.pointThreshold);
		//System.out.println("min decibel value:" + vej.neoThreshold);

	}
	
}
