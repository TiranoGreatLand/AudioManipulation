package audioRW;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WavManager1 {
	
	private WavFileHeader header;
	private byte[] data;
	
	public void WavRead(String inputpath) throws IOException{
		DataInputStream dis = new DataInputStream(new FileInputStream(inputpath));
		header = new WavFileHeader();
		
		byte[] int_16t = new byte[4];
		byte[] int_8t = new byte[2];
		
		header.mChunkID = ""+(char)dis.readByte()+(char)dis.readByte()+(char)dis.readByte()+(char)dis.readByte();
		System.out.println("chunk id "+header.mChunkID);
		
		dis.read(int_16t);
		header.mChunkSize = byteArrayToInt(int_16t);
		System.out.println("chunk size "+header.mChunkSize);
		
		header.mFormat = ""+(char)dis.readByte()+(char)dis.readByte()+(char)dis.readByte()+(char)dis.readByte();
		System.out.println("format: "+header.mFormat+" type");
		
		header.mSubChunk1ID = ""+(char)dis.readByte()+(char)dis.readByte()+(char)dis.readByte()+(char)dis.readByte();
		System.out.println("read fmt chunkID: "+header.mSubChunk1ID+"type");
		
		dis.read(int_16t);
		header.mSubChunk1Size = byteArrayToInt(int_16t);
		System.out.println("fmt chunk size "+header.mSubChunk1Size);
		
		dis.read(int_8t);
		header.mAudioFormat = byteArrayToShort(int_8t);
		System.out.println("audio format "+header.mAudioFormat);
		
		dis.read(int_8t);
		header.mNumChannel = byteArrayToShort(int_8t);
		System.out.println("channel number "+header.mNumChannel);
		
		dis.read(int_16t);
		header.mSampleRate = byteArrayToInt(int_16t);
		System.out.println("sample rate "+header.mSampleRate);
		
		dis.read(int_16t);
		header.mByteRate = byteArrayToInt(int_16t);
		System.out.println("byte rate "+header.mByteRate);
		
        dis.read(int_8t);
        header.mBlockAlign = byteArrayToShort(int_8t);
        System.out.println("block align "+header.mBlockAlign);
        
        dis.read(int_8t);
        header.mBitsPerSample = byteArrayToShort(int_8t);
        System.out.println("bits per sample "+header.mBitsPerSample);
		
        header.mSubChunk2ID = ""+(char)dis.readByte()+(char)dis.readByte()+(char)dis.readByte()+(char)dis.readByte();
        System.out.println("data id "+header.mSubChunk2ID);
        
		dis.read(int_16t);
		header.mSubChunk2Size = byteArrayToInt(int_16t);
		System.out.println("byte rate "+header.mSubChunk2Size);
        
        while(!header.mSubChunk2ID.equals("data")){
        	byte[] tmpb = new byte[header.mSubChunk2Size];
        	dis.read(tmpb);
        	
            header.mSubChunk2ID = ""+(char)dis.readByte()+(char)dis.readByte()+(char)dis.readByte()+(char)dis.readByte();
            System.out.println("data id "+header.mSubChunk2ID);
            
    		dis.read(int_16t);
    		header.mSubChunk2Size = byteArrayToInt(int_16t);
    		System.out.println("byte rate "+header.mSubChunk2Size);
        }
		
        data = new byte[header.mSubChunk2Size];
        dis.read(data);
        System.out.println("data read over, the data block is "+data.length);
        
		dis.close();
	}
	
    private static short byteArrayToShort(byte[] b) {
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    private static int byteArrayToInt(byte[] b) {
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public void wavWrite(String outputpath, int start, int duration) throws IOException{
    	DataOutputStream dos = new DataOutputStream(new FileOutputStream(outputpath));
    	int offset = start;
    	int len = duration;
    	if(header.mBitsPerSample==16){
    		offset *= 2;
    		len *= 2;
    	}
    	
    	header.mSubChunk2Size = len;
    	
    	dos.writeBytes(header.mChunkID);
    	dos.write(intToByteArray(header.mChunkSize), 0, 4);
    	dos.writeBytes(header.mFormat);
    	dos.writeBytes(header.mSubChunk1ID);
    	dos.write(intToByteArray(header.mSubChunk1Size), 0, 4);
    	dos.write(shortToByteArray((short) header.mAudioFormat), 0, 2);
        dos.write(shortToByteArray((short) header.mNumChannel), 0, 2);
        dos.write(intToByteArray((int) header.mSampleRate), 0, 4);
        dos.write(intToByteArray((int) header.mByteRate), 0, 4);
        dos.write(shortToByteArray((short) header.mBlockAlign), 0, 2);
        dos.write(shortToByteArray((short) header.mBitsPerSample), 0, 2);
        dos.writeBytes(header.mSubChunk2ID);
        dos.write(intToByteArray((int) header.mSubChunk2Size), 0, 4);
    	
        dos.write(data, offset, len);
        
        System.out.println(outputpath+" write over");
    	dos.close();
    }
    
    private static byte[] intToByteArray(int data) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(data).array();
    }

    private static byte[] shortToByteArray(short data) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(data).array();
    }
    
    public static void main(String[] args) throws IOException{
    	String inputpath = "D:/DataGames/childvoice/Chinese1.wav";
    	WavManager1 wm = new WavManager1();
    	wm.WavRead(inputpath);
    	String outputpath = "D:/DataGames/tmpvoiceseg/Chinese1Seg.wav";
    	wm.wavWrite(outputpath, 80, 210030);
    }
    
}
