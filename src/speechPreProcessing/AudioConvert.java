package speechPreProcessing;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import it.sauronsoftware.jave.InputFormatException;
import it.sauronsoftware.jave.MultimediaInfo;

public class AudioConvert {

	//音频转换后的格式，默认为wav
	String convertedFormat;
	//sampleFrequency: 音频转换后的采样频率，默认为16000
	int sampleFrequency;
	//sampleSize: 音频转换后的采样点大小，单位是bit，可取值为8或者16，默认为16
	int sampleSize;
	//timeInterval：切片时间间隔, audioSections函数中使用, 默认为5，单位：秒
	int timeInterval;
	//label：标签, audioSections函数中使用，默认为0
	int label;
	//inputInterval：audioSections函数中使用，默认为false
	//如果为false，就使用固定时间间隔和标签获取切片文件
	//如果为true，每次输入切片的起始时间、结束时间以及标签,
	//直到起始时间或结束时间输入超出范围
	boolean inputInterval;
	
	public AudioConvert(){
		this.convertedFormat = "wav";
		this.sampleFrequency = 16000;
		this.sampleSize = 16;
		this.label = 0;
		this.timeInterval = 5;
		this.inputInterval = false;
	}
	
	public AudioConvert(String convertedFormat, int sampleFrequency, int sampleSize){
		this.convertedFormat = convertedFormat;
		this.sampleFrequency = sampleFrequency;
		this.sampleSize = sampleSize;
		this.label = 0;
		this.timeInterval = 5;
		this.inputInterval = false;
	}
	
	public AudioConvert(String convertedFormat, int sampleFrequency, int sampleSize, 
			int label, int timeInterval, boolean inputInterval){
		this.convertedFormat = convertedFormat;
		this.sampleFrequency = sampleFrequency;
		this.sampleSize = sampleSize;
		this.label = label;
		this.timeInterval = timeInterval;
		this.inputInterval = inputInterval;
	}
	
	//读取音频文件，如果不是this.convertedFormat格式的，就转换成this.convertedFormat格式的
	//返回：音频的地址，没转换就是原地址，转换过就是转换后的音频的地址
	//inputpath: 音频地址
	public String audioFormatChange(String inputpath) throws InputFormatException, EncoderException{
		File source = new File(inputpath);
		if(!source.exists()){
			System.out.println("no such file!");
			return null;
		}
		MultimediaInfo m = new Encoder().getInfo(source);
		String format = m.getFormat();
		if(format.equals(this.convertedFormat)){
			return inputpath;
		} else{
			int pathlen = inputpath.length();
			String newpath = inputpath.substring(0, pathlen-format.length())+this.convertedFormat;
			File target = new File(newpath);
			AudioAttributes audio = new AudioAttributes(); 
			audio.setCodec("pcm_alaw"); 
			audio.setBitRate(new Integer(128000)); 
			audio.setChannels(new Integer(1)); 
			audio.setSamplingRate(new Integer(16000)); 
			EncodingAttributes attrs = new EncodingAttributes(); 
			attrs.setFormat(this.convertedFormat); 
			attrs.setAudioAttributes(audio); 
			Encoder encoder = new Encoder(); 
			encoder.encode(source, target, attrs); 
			return newpath;
		}
	}
	
	public AudioInputStream audioRead(String inputpath) throws InputFormatException, EncoderException, UnsupportedAudioFileException, IOException{
		File source = new File(inputpath);
		if(!source.exists()){
			System.out.println("no such file!");
			return null;
		}
		FileInputStream fileInputStream = new FileInputStream(source);
		BufferedInputStream bis = new BufferedInputStream(fileInputStream);
		AudioInputStream wavstream = AudioSystem.getAudioInputStream(bis);
		return wavstream;
	}
	
	//将音频转换为指定参数(this.sampleFrequency, this.sampleSize)的形式
	//返回：无，保存转换过的音频
	//a: 输入的音频
	//outputpath: 保存的路径
	public void audioConvert(AudioInputStream a, String outputpath) throws IOException{
		AudioFormat af = a.getFormat();
		int sfreq = (int)af.getSampleRate();
		int ssize = af.getSampleSizeInBits();
		if(sfreq == this.sampleFrequency && ssize == this.sampleSize){
			System.out.println("it does nor need to be changed");
		} else{
			AudioFormat convertedFormat = new AudioFormat(this.sampleFrequency, this.sampleSize, af.getChannels(), false, false);
			AudioInputStream convertedAudio = AudioSystem.getAudioInputStream(convertedFormat, a);
			File target = new File(outputpath);
			AudioSystem.write(convertedAudio, Type.WAVE, target);
		}
	}	
	
	//切割音频
	//sourceFile:被切割的音频文件所在路径
	//outputpath: 切割出来的音频文件片段所在路径
	//start: 切割片段在原文件中开始时间
	//end：切割片段在原文件中结束时间
	public void audioSegmentation(String sourceFile, String outputpath,
    long start, long end) throws InputFormatException, EncoderException, IOException, UnsupportedAudioFileException{
		AudioInputStream inputStream = null;
		AudioInputStream shortedStream = null;
		File file = new File(sourceFile);
		AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
		//System.out.println(fileFormat);
		AudioFormat format = fileFormat.getFormat();
		
	      inputStream = AudioSystem.getAudioInputStream(file);
	      //long frames = inputStream.getFrameLength();
	      //double durationInSeconds = (frames+0.0) / format.getFrameRate();
	      //System.out.println(durationInSeconds);
	      int bytesPerSecond = format.getFrameSize() * (int)format.getFrameRate();
	      //System.out.println(bytesPerSecond);
	      
	      int secondsToCopy = (int)(end - start);
	      inputStream.skip(start*bytesPerSecond);
	      long framesOfAudioToCopy = secondsToCopy * (int)format.getFrameRate();
	      shortedStream = new AudioInputStream(inputStream, format, framesOfAudioToCopy);
	      File destinationFile = new File(outputpath);
	      AudioSystem.write(shortedStream, fileFormat.getType(), destinationFile);
	}

	
	//得到音频文件的长度
	//返回：long类型的音频持续时间，单位 毫秒
	//输入：音频文件
	public static long getAudioDuration(File file) throws InputFormatException, EncoderException{
		long duration = 0;
		Encoder encoder = new Encoder();
		MultimediaInfo m = encoder.getInfo(file);
		duration = m.getDuration();
		return duration;
	}

	//返回音频片段文件，但没有切割音频
	//返回：一个代表切片集合的class
	//filepath：输入文件的路径
	public audioSections getAudioSectons(String filepath) throws InputFormatException, EncoderException, UnsupportedAudioFileException, IOException{
		String path = audioFormatChange(filepath);
		AudioInputStream audio = audioRead(path);
		audioConvert(audio, path);
		//转换音频成为指定参数的wav格式音频文件，然后开始切分工作
		audioSections ret;
		if(this.timeInterval<=0){
			ret = new audioSections();
			ret.setPath(path);
			ret.setLabel(this.label);
		} else{
			ret = new audioSections(path, this.timeInterval, this.label);
		}
		long audioDuration = getAudioDuration(new File(path))/1000;
		System.out.println("the duration of this audio is: "+audioDuration+" seconds");
		if(this.inputInterval){
			Scanner scanner = new Scanner(System.in);
			long start = 0, end = 0;
			int ilb = 0;
			while(true){
				System.out.println("input start time, end time and label");
				start = scanner.nextLong();
				if(start<0 || start>audioDuration){
					System.out.println("over range");
					break;
				}
				end = scanner.nextLong();
				if(end<0 || end>audioDuration || end<=start){
					System.out.println("over range");
					break;
				}
				ilb = scanner.nextInt();
				segmentUnit tmpsu = new segmentUnit(start, end, ilb);
				ret.AddSegment(tmpsu);
			}
			scanner.close();
		} else {
			ret.setLabel(this.label);
			ret.setTimeInterval(this.timeInterval);
			int tmpt = ret.getTimeInterval();
			while(tmpt<audioDuration){
				segmentUnit tmpsu = new segmentUnit(tmpt-ret.getTimeInterval(), tmpt, ret.getLabel());
				ret.AddSegment(tmpsu);
				tmpt += ret.getTimeInterval();
			}
			if(tmpt>audioDuration && tmpt-ret.getTimeInterval()<audioDuration){
				tmpt -= ret.getTimeInterval();
				segmentUnit tmpsu = new segmentUnit(tmpt, audioDuration, ret.getLabel());
				ret.AddSegment(tmpsu);
			}
		}
		return ret;
	}

	public static void main(String[] args) throws InputFormatException, EncoderException, UnsupportedAudioFileException, IOException{
		AudioConvert ac = new AudioConvert();
		String inputpath = "D:\\DataGames\\childvoice\\history1.wav";
		String outputpath = "D:\\DataGames\\childvoice\\history1_2.wav";
		AudioInputStream tmpas = ac.audioRead(inputpath);
		ac.audioConvert(tmpas, outputpath);
	}
	
}
