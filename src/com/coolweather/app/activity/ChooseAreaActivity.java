package com.coolweather.app.activity;


import java.util.ArrayList;
import java.util.List;

import com.coolweather.app.R;
import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.Country;
import com.coolweather.app.model.Province;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAreaActivity extends Activity{
	
	/**
	 * 是否从WeatherActivity中跳转过来
	 */
	private boolean isFromWeatherActivity;
	
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTRY  = 2;
	
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> dataList = new ArrayList<String>();
	private ProgressDialog progressDialog;
	/**
	 * 省、市、县列表
	 */
	private List<Province> provinceList;
	private List<City> cityList;
	private List<Country> countryList;
	/**
	 * 选中的省、市、县
	 */
	private Province selectedProvince;
	private City selectedCity;
	private Country selectedCountry;
	/**
	 * 当前选中的级别
	 */
	private int currentLevel;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//已经选择了城市并且不是从WeatherActivity中跳转过来，才会直接跳转到WeatherActivity
		if(prefs.getBoolean("city_selected", false) && !isFromWeatherActivity){
			Intent intent = new Intent(this,WeatherActivity.class);
			startActivity(intent); //选择过城市直接跳转到WeatherActivity即可
			finish();
			return ;
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		titleText = (TextView) findViewById(R.id.title_text);
		listView = (ListView) findViewById(R.id.list_view);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
		listView.setAdapter(adapter);
		coolWeatherDB = CoolWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index, long arg3) {
				// TODO Auto-generated method stub
				if(currentLevel == LEVEL_PROVINCE){
					selectedProvince = provinceList.get(index);
					queryCities();					
				} else if(currentLevel == LEVEL_CITY){
					selectedCity = cityList.get(index);
					queryCountries();
				} else if (currentLevel == LEVEL_COUNTRY){
					String countryCode = countryList.get(index).getCountryCode();
					Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
					intent.putExtra("country_code", countryCode);
					startActivity(intent);
					finish();
				}
			}	
		});
		queryProvinces(); //加载省级数据
	}
		
	/**
	 * 查询全国所有的省，优先从数据库查询，如果没有再到服务器上查询
	 */
	private void queryProvinces(){
		provinceList = coolWeatherDB.loadProvince();
		if(provinceList.size() > 0){
			dataList.clear();
			for(Province province:provinceList){
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel = LEVEL_PROVINCE;
		} else{
			queryFromServer(null, "province");
		}
	}
	
	/**
	 * 查询选中省的所有市，优先从数据库查询，如果没有再到服务器上查询
	 */
	private void queryCities(){
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());
		if(cityList.size() > 0){
			dataList.clear();
			for(City city:cityList){
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		} else {
			queryFromServer(selectedProvince.getProvinceCode(),"city");
		}
	}
	
	/**
	 * 查询选中市的所有县，优先从数据库查询，如果没有再到服务器上查询
	 */
	private void queryCountries(){
		countryList = coolWeatherDB.loadCountry(selectedCity.getId());
		if(countryList.size() > 0){
			dataList.clear();
			for(Country country:countryList){
				dataList.add(country.getCountryName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTRY;
		} else {
			queryFromServer(selectedCity.getCityCode(),"country");
		}
	}
	
	/**
	 * 根据传入的代号和类型从服务器上查询省市县数据
	 */
	private void queryFromServer(final String code,final String type){
		String address;
		if(!TextUtils.isEmpty(code)){
			address = "http://www.weather.com.cn/data/list3/city"+code+".xml";
		} else {
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener(){
			@Override
			public void onFinish(String response) {
				// TODO Auto-generated method stub
				boolean result = false;
				if("province".equals(type)){
					result = Utility.handleProvincesResponse(coolWeatherDB, response);
				} else if("city".equals(type)){
					result = Utility.handleCitiesResponse(coolWeatherDB, response, selectedProvince.getId());
				} else if("country".equals(type)){
					result = Utility.handleCountriesResponse(coolWeatherDB, response, selectedCity.getId());
				}
				if(result){
					//通过runOnUiThread方法回到主线程处理逻辑!因为涉及到UI操作，所以必须在主线程中完成
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							// TODO Auto-generated method stub
							closeProgressDialog();
							if("province".equals(type)){
								queryProvinces();
							} else if("city".equals(type)){
								queryCities();
							} else if("country".equals(type)){
								queryCountries();
							}
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
						// TODO Auto-generated method stub
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
					}					
				});
			}			
		});
		
	}
	
	/**
	 * 显示进度对话框
	 */
	private void showProgressDialog(){
		if(progressDialog == null){
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	/**
	 * 关闭进度对话框
	 */
	private void closeProgressDialog(){
		if(progressDialog != null){
			progressDialog.dismiss();
		}
	}
	
	/**
	 * 捕获Back键，根据当前级别来判断返回市列表、省列表还是直接退出
	 */
	@Override
	public void onBackPressed() {
		//super.onBackPressed();
		if(currentLevel == LEVEL_COUNTRY){
			queryCities();
		} else if(currentLevel == LEVEL_CITY){
			queryProvinces();
		} else{
			if(isFromWeatherActivity){
				Intent intent = new Intent(this,WeatherActivity.class);
				startActivity(intent);
			}
			finish();
		}
		/*String out;
		if(isFromWeatherActivity == true)
			out = "true";
		else
			out ="false";
		Log.d("ChooseAreaActivity", out);*/
	}
}
