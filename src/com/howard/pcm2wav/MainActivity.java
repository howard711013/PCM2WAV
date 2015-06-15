package com.howard.pcm2wav;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener{
	
	private static final int RESULT_CODE_GET_FILE_PATH = 1;
	
	private Button mBrowser;
	private Button mStart;
	private TextView mPath;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main_activity);
		
		mBrowser =(Button)findViewById(R.id.bt_browser);
		mStart =(Button)findViewById(R.id.bt_start);
		mPath =(TextView)findViewById(R.id.tv_path);
		
		mBrowser.setOnClickListener(this);
		mStart.setOnClickListener(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == RESULT_CODE_GET_FILE_PATH) {
			if (resultCode == Activity.RESULT_OK) {
				Uri uri = data.getData();
				if (uri != null) {
					mPath.setText(uri.getPath());
				}
			}
		}

	}
	
	private void run2Wave(String path) throws Exception
	{
		convertAudioFiles(path,path+".wav");
	}
	
	
	@Override
	public void onClick(View v) {
		
		int id = v.getId();
		
		switch(id)
		{
			case R.id.bt_browser:
			{
				Intent picker = new Intent(Intent.ACTION_GET_CONTENT);
				picker.setType("*/*");
				Intent destIntent = Intent.createChooser(picker, "");
				startActivityForResult(destIntent, RESULT_CODE_GET_FILE_PATH);
			}
			break;
			case R.id.bt_start:
			{
				try {
					run2Wave(mPath.getText().toString());
					Toast.makeText(v.getContext(), "PCM to WAVE Done", Toast.LENGTH_LONG).show();
				} catch (Exception e) {
					Toast.makeText(v.getContext(), "PCM to WAVE fail : " + e.getMessage(), Toast.LENGTH_LONG).show();;
				}
			}
			break;
		}
	}
	
	private void convertAudioFiles(String src, String target) throws Exception {
		   FileInputStream fis = new FileInputStream(src);
		   FileOutputStream fos = new FileOutputStream(target);

		   //计算长度
		   byte[] buf = new byte[1024 * 4];
		   int size = fis.read(buf);
		   int PCMSize = 0;
		   while (size != -1) {
		      PCMSize += size;
		      size = fis.read(buf);
		    }
		   fis.close();

		   //填入参数，比特率等等。这里用的是16位单声道 8000 hz
		   WaveHeader header = new WaveHeader();
		   //长度字段 = 内容的大小（PCMSize) + 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
		   header.fileLength = PCMSize + (44 - 8);
		   header.FmtHdrLeth = 16;
		   header.BitsPerSample = 16;
		   header.Channels = 1;
		   header.FormatTag = 0x0001;
		   header.SamplesPerSec = 8000;
		   header.BlockAlign = (short)(header.Channels * header.BitsPerSample / 8);
		   header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec;
		   header.DataHdrLeth = PCMSize;

		   byte[] h = header.getHeader();

		   assert h.length == 44; //WAV标准，头部应该是44字节
		   //write header
		   fos.write(h, 0, h.length);
		   //write data stream
		   fis = new FileInputStream(src);
		   size = fis.read(buf);
		   while (size != -1) {
		      fos.write(buf, 0, size);
		      size = fis.read(buf);
		   }
		   fis.close();
		   fos.close();
		   System.out.println("Convert OK!");
		}

		public class WaveHeader {
		   public final char fileID[] = {'R', 'I', 'F', 'F'};
		   public int fileLength;
		   public char wavTag[] = {'W', 'A', 'V', 'E'};;
		   public char FmtHdrID[] = {'f', 'm', 't', ' '};
		   public int FmtHdrLeth;
		   public short FormatTag;
		   public short Channels;
		   public int SamplesPerSec;
		   public int AvgBytesPerSec;
		   public short BlockAlign;
		   public short BitsPerSample;
		   public char DataHdrID[] = {'d','a','t','a'};
		   public int DataHdrLeth;

		public byte[] getHeader() throws IOException {
		   ByteArrayOutputStream bos = new ByteArrayOutputStream();
		   WriteChar(bos, fileID);
		   WriteInt(bos, fileLength);
		   WriteChar(bos, wavTag);
		   WriteChar(bos, FmtHdrID);
		   WriteInt(bos,FmtHdrLeth);
		   WriteShort(bos,FormatTag);
		   WriteShort(bos,Channels);
		   WriteInt(bos,SamplesPerSec);
		   WriteInt(bos,AvgBytesPerSec);
		   WriteShort(bos,BlockAlign);
		   WriteShort(bos,BitsPerSample);
		   WriteChar(bos,DataHdrID);
		   WriteInt(bos,DataHdrLeth);
		   bos.flush();
		   byte[] r = bos.toByteArray();
		   bos.close();
		   return r;
		}

		private void WriteShort(ByteArrayOutputStream bos, int s) throws IOException {
		   byte[] mybyte = new byte[2];
		   mybyte[1] =(byte)( (s << 16) >> 24 );
		   mybyte[0] =(byte)( (s << 24) >> 24 );
		   bos.write(mybyte);
		}


		private void WriteInt(ByteArrayOutputStream bos, int n) throws IOException {
		   byte[] buf = new byte[4];
		   buf[3] =(byte)( n >> 24 );
		   buf[2] =(byte)( (n << 8) >> 24 );
		   buf[1] =(byte)( (n << 16) >> 24 );
		   buf[0] =(byte)( (n << 24) >> 24 );
		   bos.write(buf);
		}

		private void WriteChar(ByteArrayOutputStream bos, char[] id) {
		   for (int i=0; i<id.length; i++) {
		      char c = id[i];
		      bos.write(c);
		   }
		}
		}
}