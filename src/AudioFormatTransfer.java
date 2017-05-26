
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import it.sauronsoftware.jave.InputFormatException;
import it.sauronsoftware.jave.MultimediaInfo;

public class AudioFormatTransfer {

	public static void main(String[] args) throws InputFormatException, EncoderException, UnsupportedAudioFileException, IOException{
		//File inputfile = new File("D:\\DataGames\\childvoice\\07.mp3");
		File outputfile = new File("D:\\DataGames\\childvoice\\07一岁一个月男孩1.wav");
		File inputfile = new File("D:\\DataGames\\childvoice\\07一岁一个月男孩.wav");
		if(!inputfile.exists()){
			System.out.println("file does not exist");
		}
		FileInputStream fileInputStream = new FileInputStream(inputfile);
		BufferedInputStream bis = new BufferedInputStream(fileInputStream);
		AudioInputStream mp3stream = AudioSystem.getAudioInputStream(bis);
		AudioFormat sourceFormat = mp3stream.getFormat();
		System.out.println(sourceFormat.getSampleRate());
		System.out.println(sourceFormat.getSampleSizeInBits());
		System.out.println(sourceFormat.getChannels());
		System.out.println(sourceFormat.getClass());
		
		AudioFormat convertFormat = new AudioFormat(16000, 8, 2, false, false);
		AudioInputStream converted = AudioSystem.getAudioInputStream(convertFormat, mp3stream);
		AudioSystem.write(converted, Type.WAVE, outputfile);
	}
	
}
