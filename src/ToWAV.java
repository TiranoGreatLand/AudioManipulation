import java.io.File;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import it.sauronsoftware.jave.InputFormatException;
import it.sauronsoftware.jave.MultimediaInfo;

public class ToWAV {

	public static void main(String[] args) throws IllegalArgumentException, InputFormatException, EncoderException{
		
		String tempPath = "D:\\DataGames\\childvoice\\07.mp3";
		String targetPath = "D:\\DataGames\\childvoice\\0702.wav";
		
		File source = new File(tempPath);
		MultimediaInfo m = new Encoder().getInfo(source);
		System.out.println(m.getFormat());
		
		File target = new File(targetPath); 
		
		AudioAttributes audio = new AudioAttributes(); 
		audio.setCodec("pcm_alaw"); 
		audio.setBitRate(new Integer(64000)); 
		audio.setChannels(new Integer(1)); 
		audio.setSamplingRate(new Integer(8000)); 
		EncodingAttributes attrs = new EncodingAttributes(); 
		attrs.setFormat("wav"); 
		attrs.setAudioAttributes(audio); 
		Encoder encoder = new Encoder(); 
		encoder.encode(source, target, attrs); 

	}
	
}
