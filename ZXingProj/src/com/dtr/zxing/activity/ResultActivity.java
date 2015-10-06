package com.dtr.zxing.activity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.dtr.zxing.R;
import com.dtr.zxing.camera.CameraManager;
import com.dtr.zxing.decode.DecodeThread;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static java.lang.String.format;
import static java.lang.String.valueOf;

public class ResultActivity extends Activity implements View.OnClickListener {
	private static final int MSG_SN_NOT_EXIST = 1;
	private static final int MSG_SN_EXIST = 2;
	private static final int MSG_PICTURE_EXIST = 3;
	private static final int MSG_PICTURE_NOT_EXIST = 4;
	private static final int MSG_DOWNLOAD_PICTURE = 5;
	private static final int MSG_UPLOAD_PICTURE = 6;
	private static final int MSG_UPDATE_PICTURE = 7;
	private static final int MSG_SN_NULL = 8;
	private static final int MSG_CONN_FAIL = 9;
	private static final int MSG_UPLOAD_PICTURE_SUCCESS = 10;
	private static final int MSG_UPLOAD_PICTURE_FAIL = 11;
	private ImageView mResultImage;
	private TextView mResultText;
	private Button button;
	private Button button2;
	private ImageView fixtureImage;
	private String filePath;
	private File filePic;
	private CheckBox checkBox;
	private String sn;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyHandler myHandler = new MyHandler();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_result);

		Log.v("allen,thread1", Thread.currentThread().getName());
		//Log.v("allen:", "after ResultActivity start");
		Bundle extras = getIntent().getExtras();

		mResultImage = (ImageView) findViewById(R.id.result_image);
		mResultText = (TextView) findViewById(R.id.result_text);
		button = (Button)findViewById(R.id.button);
		button2 = (Button)findViewById(R.id.button2);
		button.setOnClickListener(this);
		button2.setOnClickListener(this);
		fixtureImage =(ImageView)findViewById(R.id.fixtureImage);
		checkBox = (CheckBox)findViewById(R.id.checkBox);


		// ---------------------------------------------------------------------------------------
		if (null != extras) {
			int width = extras.getInt("width");
			int height = extras.getInt("height");

			LayoutParams lps = new LayoutParams(width, height);
			lps.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
			lps.leftMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
			lps.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
			
			mResultImage.setLayoutParams(lps);

			Bitmap barcode = null;
			byte[] compressedBitmap = extras.getByteArray(DecodeThread.BARCODE_BITMAP);
			if (compressedBitmap != null) {
				barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.length, null);
				// Mutable copy:
				//barcode = barcode.copy(Bitmap.Config.ALPHA_8, true);
				//barcode = barcode.copy(Bitmap.Config.ARGB_4444, true);
				//barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
				barcode = barcode.copy(Bitmap.Config.RGB_565, true);
			}

			mResultImage.setImageBitmap(barcode);
			// string------------------------------
			String result = extras.getString("result");
			mResultText.setText(result);
			String[] resultArr = result.split(";");
			if(resultArr.length == 6 || resultArr.length == 1){
				// new thread to get xml for fn=imageQuery##############################
				Thread threadGetXml = new Thread(new Runnable() {
					@Override
					public void run() {
						Log.v("allen: in threadGetXml", "Hi");
						String sn = "";
						if(resultArr.length == 6){
							sn = resultArr[5];
						}else {
							sn = resultArr[0];
						}
						if(sn != ""){
							StringBuilder xml = new StringBuilder();
							xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
							xml.append("<imageQuery>\n");
							xml.append("<sn>" + sn + "</sn>\n");
							xml.append("</imageQuery>\n");
							try{
								byte[] xmlbyte = xml.toString().getBytes("UTF-8");
								//URL url = new URL("http://192.168.6.9/webel/fixture.php?fu=imageQuery&sid=" + valueOf(Math.random()));
								//URL url = new URL("http://192.168.1.101/webel/fixture.php?fu=imageQuery&sid=" + valueOf(Math.random()));
								URL url = new URL("http://192.168.1.4/webel/fixture.php?fu=imageQuery&sid=" + valueOf(Math.random()));
								HttpURLConnection conn = null;
								conn = (HttpURLConnection) url.openConnection();

								conn.setConnectTimeout(5000);
								conn.setDoOutput(true);
								conn.setDoInput(true);
								conn.setUseCaches(false);
								conn.setRequestMethod("POST");
								conn.setRequestProperty("Connection", "Keep-Alive");//长连接
								conn.setRequestProperty("Charset", "UTF-8");
								conn.setRequestProperty("Content-Length", String.valueOf(xmlbyte.length));
								conn.setRequestProperty("Content-Type", "Text/xml; charset=UTF-8");
								conn.setRequestProperty("X-ClientType", "2");
								conn.getOutputStream().write(xmlbyte);
								conn.getOutputStream().flush();
								conn.getOutputStream().close();

								Log.v("allen, RC is", valueOf(conn.getResponseCode()));
								if(conn.getResponseCode() != 200) {
									//throw new RuntimeException("请求url失败");
								}
								InputStream is = conn.getInputStream();//获取返回数据

								ByteArrayOutputStream out = new ByteArrayOutputStream();
								byte[] buf = new byte[1024];
								int len;
								while((len = is.read(buf)) != -1){
									out.write(buf, 0, len);
								}
								String string = out.toString("UTF-8");
								Log.v("allen: return xml is", "\r\n" + string);
								out.close();

								// xml解析
								if(string.length()>0){
									String image_exist = null;
									String sn_exist = null;
									XmlPullParser parser = Xml.newPullParser();
									try{
										parser.setInput(new ByteArrayInputStream(string.getBytes("UTF-8")), "UTF-8");
										//parser.setInput(new ByteArrayInputStream(out.toByteArray()), "UTF-8");
										//parser.setInput(is,"UTF-8");
										int eventType = parser.getEventType();
										while (eventType != XmlPullParser.END_DOCUMENT){
											if(eventType == XmlPullParser.START_TAG){
												if("sn_exist".equals(parser.getName())){
													sn_exist = parser.nextText();
													//Log.v("allen: sn_exist is", sn_exist);
												}else if("image_exist".equals(parser.getName())){
													image_exist = parser.nextText().trim();
													//Log.v("allen: image_exist is", image_exist);
												}
											}
											eventType = parser.next();
										}
										// 发送信息到MyHandler
										//Log.v("allen: image_exist is",image_exist);
										Message msg = myHandler.obtainMessage();
										if("N".equals(sn_exist)){
											msg.what = MSG_SN_NOT_EXIST;
											msg.obj = sn;
											myHandler.sendMessage(msg);
										}else{
											msg.what = MSG_SN_EXIST;
											msg.obj = sn;
											myHandler.sendMessage(msg);
										}
										Message msg2 = myHandler.obtainMessage();
										if("N".equals(image_exist)){
											msg2.what = MSG_PICTURE_NOT_EXIST;
											msg2.obj = sn;
											myHandler.sendMessage(msg2);
										}else {
											msg2.what = MSG_PICTURE_EXIST;
											msg2.obj = sn;
											myHandler.sendMessage(msg2);
										}
									}catch (XmlPullParserException e){
										e.printStackTrace();
										System.out.println(e);
									}catch (IOException e){
										e.printStackTrace();
										System.out.println(e);
									}
								}
							}catch (Exception e){
								e.printStackTrace();
								//System.out.println(e);
								Message msg = myHandler.obtainMessage();
								msg.what = MSG_CONN_FAIL;
								msg.obj = sn;
								myHandler.sendMessage(msg);
							}
						}else{
							// 发送信息到MyHandler, MSG_SN_NULL
							Message msg = myHandler.obtainMessage();
							msg.what = MSG_SN_NULL;
							msg.obj = sn;
							myHandler.sendMessage(msg);
						}
					}
				});
				threadGetXml.start();
			}
		}
	}


	/* Class
	 * upload file to server
	 *
	 */
	public  class UploadFile{
		private static final String TAG = "uploadFile";
		private static final int TIME_OUT = 10*1000;
		private static final String CHARSET = "utf-8";
		private static final boolean SUCCESS = true;
		private static final boolean FAILURE = false;
		//public Handler myHandler;

		public boolean uploadFile(File file,String sn){
			//Log.v("allen, file name is", file.getName());
			String BOUNDARY = UUID.randomUUID().toString();
			String PREFIX = "--", LINE_END = "\r\n";
			String CONTENT_TYPE = "multipart/form-data";
			String RequestURL = "http://192.168.1.4/webel/fixture.php?fu=fixturePictureUpload&sid=" + Math.random();
			//String RequestURL = "http://192.168.1.4/webel/fixturePictureTemp.php";
			try{
				URL url = new URL(RequestURL);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setReadTimeout(TIME_OUT);
				conn.setConnectTimeout(TIME_OUT);
				conn.setDoInput(true);
				conn.setDoOutput(true);
				conn.setUseCaches(false);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Charset", CHARSET);
				conn.setRequestProperty("connection", "keep-alive");
				conn.setRequestProperty("Content-Type", CONTENT_TYPE + ";boundary=" + BOUNDARY);
				if(file != null){
					OutputStream outputStream = conn.getOutputStream();
					DataOutputStream dos = new DataOutputStream(outputStream);
					StringBuffer sb = new StringBuffer();

					sb.append(PREFIX);
					sb.append(BOUNDARY);
					sb.append(LINE_END);
					sb.append("Content-Disposition: form-data; name=\"sn\"");
					sb.append(LINE_END);
					sb.append(LINE_END);
					sb.append(sn);
					sb.append(LINE_END);

					sb.append(PREFIX);
					sb.append(BOUNDARY);
					sb.append(LINE_END);
					sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + LINE_END);
					//sb.append("Content-Type: application/octet-stream; charset=" + CHARSET + LINE_END);
					sb.append("Content-Type: image/jpeg; charset=" + CHARSET + LINE_END);
					sb.append(LINE_END);

					Log.v("allen: sb is", sb.toString());

					dos.write(sb.toString().getBytes());
					InputStream is = new FileInputStream(file);
					byte[] bytes = new byte[1024];
					int len = 0;
					while ((len = is.read(bytes)) != -1){
						dos.write(bytes, 0, len);
					}
					is.close();
					dos.write(LINE_END.getBytes());
					byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINE_END).getBytes();
					dos.write(end_data);
					dos.flush();

					int res = conn.getResponseCode();
					if(res == 200){
						InputStream isPic = conn.getInputStream();//获取返回数据

						ByteArrayOutputStream out = new ByteArrayOutputStream();
						byte[] buf = new byte[1024];
						int lenPicback;
						while((lenPicback = isPic.read(buf)) != -1){
							out.write(buf, 0, lenPicback);
						}
						String string = out.toString("UTF-8");
						Log.v("allen: return xml is", "\r\n" + string);
						out.close();

						// xml解析
						String result = "F";
						if(string.length()>0) {
							XmlPullParser parser = Xml.newPullParser();
							try {
								parser.setInput(new ByteArrayInputStream(string.getBytes("UTF-8")), "UTF-8");
								//parser.setInput(new ByteArrayInputStream(out.toByteArray()), "UTF-8");
								//parser.setInput(is,"UTF-8");
								int eventType = parser.getEventType();
								while (eventType != XmlPullParser.END_DOCUMENT) {
									if (eventType == XmlPullParser.START_TAG) {
										if ("result".equals(parser.getName())) {
											result = parser.nextText();
											//Log.v("allen: sn_exist is", sn_exist);
										}
									}
									eventType = parser.next();
								}
							} catch (XmlPullParserException e) {
								e.printStackTrace();
								//System.out.println(e);
							} catch (IOException e) {
								e.printStackTrace();
								//System.out.println(e);
							}
						}
						return SUCCESS;
					}

				}

			}catch (MalformedURLException e){
				e.printStackTrace();
			}catch (IOException e){
				e.printStackTrace();
			}
			return FAILURE;
		}
	}

	/* Method
	 * handling click actions
	 */
	@Override
	public void onClick(View v) {

		MyHandler myHandler = new MyHandler();
		switch (v.getId()){
			case R.id.button:
				Log.v("allen: button", "pressed");
				String state = Environment.getExternalStorageState();
				Log.v("allen:SD card state is",state);
				if(state.equals(Environment.MEDIA_MOUNTED)){
					Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
					filePath = getFileName();
					Log.v("allen:filePath is", filePath);
					intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(filePath)));
					startActivityForResult(intent, 1);
				}else{
					Toast.makeText(ResultActivity.this, R.string.comm_msg_nosdcard, Toast.LENGTH_LONG).show();
				}
				break;
			case R.id.button2:
				Log.v("allen: button2", "pressed");
				Message msg = myHandler.obtainMessage();
				Log.v("allen,thread2", Thread.currentThread().getName());
				// 新线程上传图片#####################################
				Thread uploadPicThread = new Thread(new Runnable() {
					@Override
					public void run() {
						UploadFile uploadFile = new UploadFile();
						boolean upResult = uploadFile.uploadFile(filePic,sn);
						if(upResult){
							//Toast.makeText(ResultActivity.this, R.string.upload_picture_success, Toast.LENGTH_LONG).show();
							//Log.v("allen,Upload","sucess !!!!!!!");
							msg.what = MSG_UPLOAD_PICTURE_SUCCESS;
							myHandler.sendMessage(msg);
						}else{
							//Toast.makeText(ResultActivity.this, R.string.upload_picture_fail, Toast.LENGTH_LONG).show();
							//Log.v("allen,Upload","fail !!!!!!!");
							msg.what = MSG_UPLOAD_PICTURE_FAIL;
							myHandler.sendMessage(msg);
						}
						Log.v("allen, upload pic res", valueOf(upResult));
					}
				});
				uploadPicThread.start();
				break;
		}

	}

	/* Method
	 * 生成文件路径和文件名
	 */
	private String getFileName(){
		String saveDir = Environment.getExternalStorageDirectory() + "/FixturePic";
		File dir = new File(saveDir);
		if(!dir.exists()){
			dir.mkdir();
		}
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		String fileName = saveDir + "/" + formatter.format(date) + ".jpeg";
		filePic = new File(fileName);
		if(!filePic.exists()){
			try{
				filePic.createNewFile();
			}catch (IOException e){
			}
		}
		return  fileName;
	}

	/* Class
	* Class of handling Message
	*
	 */
	class MyHandler extends Handler{
		public MyHandler(){
		}

		public MyHandler(Looper L){

		}

		@Override
		public void handleMessage(Message msg){
			super.handleMessage(msg);
			//此处可以更新UI
			//Log.v("allen:in handleMeg","hi");
			//Log.v("allen:what,obj", msg.what + "," + (String)msg.obj);
			String s = "";
			switch (msg.what){
				case MSG_SN_NOT_EXIST:
					checkBox.setChecked(false);
					button2.setClickable(false);
					Toast.makeText(ResultActivity.this,getString(R.string.sn_not_exist),Toast.LENGTH_LONG).show();
					sn = "";
					break;
				case MSG_SN_EXIST:
					checkBox.setChecked(true);
					button2.setClickable(true);
					sn = (String)msg.obj;
					Toast.makeText(ResultActivity.this,getString(R.string.sn_exist),Toast.LENGTH_LONG).show();
					break;
				case MSG_PICTURE_EXIST:
					//Log.v("allen:Picture", "exist");
					//button.setVisibility(View.VISIBLE);
						button2.setText(getString(R.string.button2_update).toString());
					break;
				case MSG_PICTURE_NOT_EXIST:
					//Log.v("allen:Picture", "NOT exist");
						//s = "上传图片";
						button2.setText(getString(R.string.button2_upload).toString());
					break;
				case MSG_DOWNLOAD_PICTURE:
					break;
				case MSG_UPLOAD_PICTURE:
					break;
				case MSG_UPDATE_PICTURE:
					break;
				case MSG_SN_NULL:
					Log.v("allen:SN name is", "null");
					break;
				case MSG_CONN_FAIL:
					Toast.makeText(ResultActivity.this, getString(R.string.TOAST_CAN_NOT_CONNECT_DB),Toast.LENGTH_LONG).show();
					break;
				case MSG_UPLOAD_PICTURE_SUCCESS:
					Toast.makeText(ResultActivity.this, getString(R.string.upload_picture_success),Toast.LENGTH_LONG).show();
					break;
				case MSG_UPLOAD_PICTURE_FAIL:
					Toast.makeText(ResultActivity.this, getString(R.string.upload_picture_fail),Toast.LENGTH_LONG).show();
					break;
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == 0x1 && resultCode == this.RESULT_OK){
			BitmapFactory.Options myoptions = new BitmapFactory.Options();
			myoptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(filePic.getAbsolutePath(),myoptions);
			//myoptions.inSampleSize = 4;
			//int height =myoptions.outHeight*222/myoptions.outWidth;
			//int with_org = myoptions.outWidth;
			int maxValue = Math.max(myoptions.outHeight, myoptions.outWidth);
			int be = maxValue/1024;
			//Log.v("allen, be", valueOf(be));
			myoptions.inSampleSize = Math.round(be);
			Log.v("allen, inSampleSize", valueOf(myoptions.inSampleSize));
			//Log.v("allen, with_org is", valueOf(with_org));
			//myoptions.outWidth =222;
			//myoptions.outHeight=height;
			myoptions.outWidth = myoptions.outWidth/myoptions.inSampleSize;
			myoptions.outHeight = myoptions.outHeight/myoptions.inSampleSize;
			myoptions.inJustDecodeBounds = false;
			//myoptions.inSampleSize = myoptions.outWidth/222;
			//myoptions.inSampleSize = with_org/222;
			//myoptions.inSampleSize = 2;
			//Log.v("allen, inSampleSize is", valueOf(myoptions.inSampleSize));
			myoptions.inPurgeable = true;
			myoptions.inInputShareable = true;
			myoptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
			//myoptions.inPreferredConfig = Bitmap.Config.RGB_565;
			//myoptions.inPreferredConfig = Bitmap.Config.ALPHA_8;
			//myoptions.inPreferredConfig = Bitmap.Config.ARGB_4444;
			myoptions.inScreenDensity = 120;
			//myoptions.inScreenDensity = 90;
			Bitmap bitmap = BitmapFactory.decodeFile(filePath,myoptions);
			fixtureImage.setImageBitmap(bitmap);
			Log.v("allen, bitmap size", "Hight:" + valueOf(bitmap.getHeight()) + " Width:" + valueOf(bitmap.getWidth()));// 4192 x 3104, 4128 x3096

			FileOutputStream fileOutputStream = null;
			try {
				fileOutputStream = new FileOutputStream(filePath);
				bitmap.compress(Bitmap.CompressFormat.PNG, 100,fileOutputStream);
				fileOutputStream.flush();
				fileOutputStream.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

}








































