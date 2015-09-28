package com.dtr.zxing.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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

	private ImageView mResultImage;
	private TextView mResultText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
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
			//Log.v("allen:", result);
			String[] resultArr = result.split(";");
			//Log.v("allen:len of resultArr", valueOf(resultArr.length));
			if(resultArr.length == 6){
				// new thread to get xml for fn=imageQuery------------------------------
				Thread threadGetXml = new Thread(new Runnable() {
					@Override
					public void run() {
						Log.v("allen: in threadGetXml", "Hi");
						String sn = resultArr[5];
						//Log.v("allen: SN is", sn);
						StringBuilder xml = new StringBuilder();
						xml.append("<?xml version=\"1.0\" endcoding=\"UTF-8\"?>\n");
						xml.append("<imageQuery>\n");
						xml.append("<sn>" + sn + "</sn>\n");
						xml.append("</imageQuery>\n");
						//Log.v("allen:xml is", xml.toString());
						try{
							//Log.v("allen: in thread->try", "hi->1");
							byte[] xmlbyte = xml.toString().getBytes("UTF-8");
							//URL url = new URL("http://192.168.1.101/webel/fixture.php?fu=imageQuery&sid=" + valueOf(Math.random()));
							URL url = new URL("http://192.168.1.4/webel/fixture.php?fu=imageQuery&sid=" + valueOf(Math.random()));
							//Log.v("allen:URL is", url.toString());
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
							//Log.v("allen: in thread->try", "hi->2");
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
							String image_exist = null;
							XmlPullParser parser = Xml.newPullParser();
							Log.v("allen: in xml parser", "hi->3");
							try{
								//parser.setInput(new ByteArrayInputStream(string.substring(1).getBytes("UTF-8")), "UTF-8");
								parser.setInput(is,"UTF-8");
								int eventType = parser.getEventType();
								Log.v("allen: in xml parser", "hi->4");
								while (eventType != XmlPullParser.END_DOCUMENT){
									Log.v("allen: in xml parser", "hi->5");
									Log.v("allen:loc1,eventType is", valueOf(eventType));
									if(eventType == XmlPullParser.START_TAG){
										Log.v("allen: in xml parser", "hi->6");
										if("image_exist".equals(parser.getName())){
											image_exist = parser.nextText();
											Log.v("allen: image_exist is", image_exist);
										}
									}
									eventType = parser.next();
									Log.v("allen:loc2,eventType is", valueOf(eventType));
								}
							}catch (XmlPullParserException e){
								e.printStackTrace();
								System.out.println(e);
							}catch (IOException e){
								e.printStackTrace();
								System.out.println(e);
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
}
