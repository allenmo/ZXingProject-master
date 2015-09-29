package com.dtr.zxing.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.dtr.zxing.R;
import com.dtr.zxing.decode.DecodeThread;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static java.lang.String.valueOf;

public class ResultActivity extends Activity {
	private static final int MSG_SN_NOT_EXIST = 1;
	private static final int MSG_SN_EXIST = 2;
	private static final int MSG_PICTURE_EXIST = 3;
	private static final int MSG_PICTURE_NOT_EXIST = 4;
	private static final int MSG_DOWNLOAD_PICTURE = 5;
	private static final int MSG_UPLOAD_PICTURE = 6;
	private static final int MSG_UPDATE_PICTURE = 7;
	private ImageView mResultImage;
	private TextView mResultText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyHandler myHandler = new MyHandler();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_result);

		Log.v("allen:", "after ResultActivity start");
		Bundle extras = getIntent().getExtras();

		mResultImage = (ImageView) findViewById(R.id.result_image);
		mResultText = (TextView) findViewById(R.id.result_text);

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
			if(resultArr.length == 6){
				// new thread to get xml for fn=imageQuery------------------------------
				Thread threadGetXml = new Thread(new Runnable() {
					@Override
					public void run() {
						Log.v("allen: in threadGetXml", "Hi");
						String sn = resultArr[5];
						StringBuilder xml = new StringBuilder();
						xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
						xml.append("<imageQuery>\n");
						xml.append("<sn>" + sn + "</sn>\n");
						xml.append("</imageQuery>\n");
						try{
							byte[] xmlbyte = xml.toString().getBytes("UTF-8");
							URL url = new URL("http://192.168.6.9/webel/fixture.php?fu=imageQuery&sid=" + valueOf(Math.random()));
							//URL url = new URL("http://192.168.1.101/webel/fixture.php?fu=imageQuery&sid=" + valueOf(Math.random()));
							//URL url = new URL("http://192.168.1.4/webel/fixture.php?fu=imageQuery&sid=" + valueOf(Math.random()));
							HttpURLConnection conn = (HttpURLConnection)url.openConnection();
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

							if(conn.getResponseCode() != 200)
								throw	new RuntimeException("请求url失败");

							InputStream is = conn.getInputStream();//获取返回数据

							ByteArrayOutputStream out = new ByteArrayOutputStream();
							byte[] buf = new byte[1024];
							int len;
							while((len = is.read(buf)) != -1){
								out.write(buf, 0, len);
							}
							String string = out.toString("UTF-8");
							Log.v("allen: return xml is", string);
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
												image_exist = parser.nextText();
												//Log.v("allen: image_exist is", image_exist);
											}
										}
										eventType = parser.next();
									}
									// 发送信息到MyHandler
									Message msg = myHandler.obtainMessage();
									if(sn_exist == "N"){
										msg.what = MSG_SN_NOT_EXIST;
										msg.obj = sn;
									}else if(image_exist == "N"){
										msg.what = MSG_PICTURE_NOT_EXIST;
										msg.obj = sn;
									}else {
										msg.what = MSG_PICTURE_EXIST;
										msg.obj = sn;
									}
									myHandler.sendMessage(msg);
								}catch (XmlPullParserException e){
									e.printStackTrace();
									System.out.println(e);
								}catch (IOException e){
									e.printStackTrace();
									System.out.println(e);
								}
							}

						}catch (Exception e){
							System.out.println(e);
						}

					}
				});
				threadGetXml.start();



			}

		}
	}

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
			switch (msg.what){
				case MSG_SN_NOT_EXIST:
					break;
				case MSG_SN_EXIST:
					break;
				case MSG_PICTURE_EXIST:
					Log.v("allen:Picture", "exist");
					break;
				case MSG_PICTURE_NOT_EXIST:
					Log.v("allen:Picture", "NOT exist");
					break;
				case MSG_DOWNLOAD_PICTURE:
					break;
				case MSG_UPLOAD_PICTURE:
					break;
				case MSG_UPDATE_PICTURE:
					break;
			}
		}
	}

}
