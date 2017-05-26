import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.sound.sampled.UnsupportedAudioFileException;

import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.InputFormatException;

public class speechSegmentation {

	AudioConvert audioseg;
	
	public audioSections getSpeechSegmentation(String inputpath, String outputpath, 
    String format, int sampleFrequency, int sampleSize) throws InputFormatException, EncoderException, UnsupportedAudioFileException, IOException{
		String tobeformat = "wav";
		if(format!=null) tobeformat = format;
		int tobesamplefrequency = 16000;
		if(sampleFrequency>0) tobesamplefrequency = sampleFrequency;
		int tobesamplesize = 16;
		if(sampleSize>0) tobesamplesize = sampleSize;
		audioseg = new AudioConvert(tobeformat, tobesamplefrequency, tobesamplesize);
		audioSections speechSeg = audioseg.getAudioSectons(inputpath);
		SerializedSave(speechSeg, outputpath);
		return speechSeg;
	}

	//保存音频切片集
	public void SerializedSave(audioSections as, String outputpath) throws IOException{
		File file = new File(outputpath);
		if(!file.exists()){
			file.createNewFile();
		}
		ObjectOutputStream oos = null;
		oos = new ObjectOutputStream(new FileOutputStream(file));
		oos.writeObject(as);
		oos.close();
		System.out.println("file save succeed");
	}
	
	//读取音频切片集
	public audioSections SerializedRead(String filepath) throws IOException, ClassNotFoundException{
		File file = new File(filepath);
		if(!file.exists()){
			System.out.println("error filepath or file does not exist");
			return null;
		}
		ObjectInputStream ois = null;
		ois = new ObjectInputStream(new FileInputStream(file));
		audioSections ret = (audioSections)ois.readObject();
		ois.close();
		System.out.println("read succeed");
		return ret;
	}
	
	//以下为测试代码
	public static void main(String[] args) throws InputFormatException, EncoderException, UnsupportedAudioFileException, IOException, ClassNotFoundException{
		speechSegmentation tmps = new speechSegmentation();
		String inputpath = "D:\\DataGames\\childvoice\\07.mp3";
		String outputpath = "D:\\DataGames\\childvoice\\07serialized.txt";
		String format = "wav";
		int samplefrequency = 16000;
		int samplesize = 8;
		audioSections tmpa = tmps.getSpeechSegmentation(inputpath, outputpath, format, samplefrequency, samplesize);
		audioSections tmpa1 = tmps.SerializedRead(outputpath);
		
		System.out.println(tmpa.getPath()+" "+tmpa.getNumOfSegment());
		for(segmentUnit su : tmpa.segments){
			System.out.println(su.starttime+" "+su.endtime+" "+su.label);
		}
		System.out.println(tmpa1.getPath()+" "+tmpa1.getNumOfSegment());
		for(segmentUnit su : tmpa1.segments){
			System.out.println(su.starttime+" "+su.endtime+" "+su.label);
			String savepath = "D:\\DataGames\\childvoice\\07"+"_"+su.starttime+"_"+su.endtime+".wav";
			tmps.audioseg.audioSegmentation(tmpa1.getPath(), savepath, su.starttime, su.endtime);
		}
		//tmps.audioseg.audioSegmentation("D:\\DataGames\\childvoice\\07.wav", "D:\\DataGames\\childvoice\\07_00_05.wav", 0, 5);
	}
	
}
