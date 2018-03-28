import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class try2 {

	public static void main(String[] args) throws IOException, UnsupportedAudioFileException{
		String inputpath = "E:\\QQfiles\\342479958\\FileRecv\\动能势能2_201705139142000.wav";
		File wav = new File(inputpath);
		FileInputStream fis = new FileInputStream(wav);
		BufferedInputStream bis = new BufferedInputStream(fis);
		AudioInputStream wavstream = AudioSystem.getAudioInputStream(bis);
		AudioFormat sourceFormat = wavstream.getFormat();
		System.out.println(sourceFormat.getSampleRate());
		System.out.println(sourceFormat.getSampleSizeInBits());
		System.out.println(sourceFormat.getChannels());
		System.out.println(sourceFormat.getFrameRate());
		System.out.println(sourceFormat.getClass());
	}
	
}
