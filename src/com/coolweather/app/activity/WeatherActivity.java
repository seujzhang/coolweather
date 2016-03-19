package com.coolweather.app.activity;

import com.coolweather.app.R;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WeatherActivity extends Activity{
	
	private LinearLayout weatherInfoLayout;
	/**
	 * 显示城市名、发布时间、当前日期、天气描述、气温1、气温2
	 */
	private TextView cityNameText;
	private TextView publishText;
	private TextView currentDateText;
	private TextView weatherDestText;
	private TextView temp1;
	private TextView temp2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.weather_layout);
		//初始化各个控件
		weatherInfoLayout = (LinearLayout) findViewById(R.id.weather_info_layout);
		cityNameText = (TextView) findViewById(R.id.city_name);
		publishText = (TextView) findViewById(R.id.publish_text);
		currentDateText = (TextView) findViewById(R.id.current_data);
		weatherDestText = (TextView) findViewById(R.id.weather_dest);
		temp1 = (TextView) findViewById(R.id.temp1);
		temp2 = (TextView) findViewById(R.id.temp2);
		
		String countryCode = getIntent().getStringExtra("country_code");
		if(!TextUtils.isEmpty(countryCode)){
			//有县级代号时就去查询天气
			publishText.setText("同步中...");
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
			queryWeatherCode(countryCode);
		} else {
			//没有县级代号就直接显示本地天气
			showWeather();
		}
	}
	
	/**
	 * 查询县级代号所对应的天气代号
	 */
	private void queryWeatherCode(String countryCode){
		String address = "http://www.weather.com.cn/data/list3/city"+countryCode+".xml";
		queryFromServer(address, "countryCode");
	}
	
	/**
	 * 查询天气代号所对应的天气
	 */
	private void queryWeatherInfo(String weatherCode){
		String address = "http://www.weather.com.cn/data/cityinfo/"+weatherCode+".html";
		queryFromServer(address, "weatherCode");
	}
	
	/**
	 * 根据传入的地址和类型去向服务器查询天气代号或者天气信息
	 */
	private void queryFromServer(final String address,final String type){
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {			
			@Override
			public void onFinish(String response) {
				if("countryCode".equals(type)){
					String[] array = response.split("\\|");
					if(array != null && array.length == 2){
						String weatherCode = array[1];
						queryWeatherInfo(weatherCode);
					}
					
				} else if("weatherCode".equals(type)){
					Utility.handleWeatherResponse(WeatherActivity.this, response);
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							showWeather();
						}						
					});
				}				
			}
			
			@Override
			public void onError(Exception e) {
				// TODO Auto-generated method stub
				runOnUiThread(new Runnable(){
					@Override
					public void run() {
						publishText.setText("同步失败");
					}					
				});
			}
		});
	}	
	
	/**
	 * 从SharedPreferences文件中读取存储的天气信息，并显示到界面上
	 */
	private void showWeather(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		cityNameText.setText(prefs.getString("city_name", ""));
		publishText.setText("今天"+prefs.getString("publish_time", "")+"发布");
		currentDateText.setText(prefs.getString("current_date", ""));
		weatherDestText.setText(prefs.getString("weather_dest", ""));
		temp1.setText(prefs.getString("temp1", ""));
		temp2.setText(prefs.getString("temp2", ""));
		weatherInfoLayout.setVisibility(View.VISIBLE);
		cityNameText.setVisibility(View.VISIBLE);
	}
}
