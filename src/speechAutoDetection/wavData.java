package speechAutoDetection;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class wavData {

	int[][] data = null;
	int numOfChannel;
	long numOfFrame;
	int samplingFrequency;
	int samplingSize;
	int len;
	
	public void readDataFromWave(String inputpath) throws UnsupportedAudioFileException, IOException{
		System.out.println("start to read wav file");
		File wav = new File(inputpath);
		//System.out.println(wav.length());
		FileInputStream fis = new FileInputStream(wav);
		BufferedInputStream bis = new BufferedInputStream(fis);
		AudioInputStream wavstream = AudioSystem.getAudioInputStream(bis);
		AudioFormat sourceFormat = wavstream.getFormat();
		
		this.numOfChannel = sourceFormat.getChannels();
		this.numOfFrame = wavstream.getFrameLength();
		this.samplingFrequency = (int) sourceFormat.getFrameRate();
		this.samplingSize = sourceFormat.getSampleSizeInBits();
		this.len = (int)(this.numOfFrame/this.numOfChannel);
		this.data = new int[this.numOfChannel][this.len];
		int byteperSample = this.samplingSize/8;
		for(int i=0;i<this.len;i++){
			for(int n=0;n<this.numOfChannel;n++){
				this.data[n][i] = this.read16bit(bis, byteperSample);
			}
		}
		System.out.println("data read over");
		
	}
	
	private int read16bit(BufferedInputStream bis, int bytepersample){
		byte[] buf = new byte[bytepersample];
		int res = 0;
		try {
			if(bis.read(buf)!=bytepersample)
				throw new IOException("no more data!!!");
			//res = (buf[0]&0x000000FF) | (((int)buf[1])<<8);
			res = (buf[0]&0x000000FF);
			for(int i=1;i<bytepersample;i++){
				res |= (((int)buf[1])<<(8*i));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}
	
}
