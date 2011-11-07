package org.mobilelite.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mobilelite.annotation.Service;

import android.os.Build;
import android.util.Log;
import android.webkit.WebView;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class PageEventDispatcher {
	
	/** The web view which need to communicate with js. */
	protected WebView webView;
	
	protected Map<String, ServiceBean> beans = new HashMap<String, ServiceBean>();
	
	protected Gson gson = new Gson();
	
	public PageEventDispatcher(WebView webView) {
		this.webView = webView;
	}
	
	public void invokeBeanAction(String beanName, String methodName, String params, String callback) {
		Log.d("BeanAction", beanName);
		Log.d("methodName", methodName);
		Log.d("params", params);
		Log.d("callback", callback == null? "null" : callback);
		//webView.loadUrl("javascript:liteEngine.dispatchEvent(" +event + "," + serializeData(data) + ")");
		
		try {
			JsonParser jsonParser = new JsonParser();
			JsonElement je = jsonParser.parse(params);
			
			_invokeBeanAction(beanName, methodName, je, callback);
		} catch (JsonSyntaxException e) {
			Log.e("invokeBeanAction", "json parameter format error", e);
		}
	}

	void _invokeBeanAction(String beanName, String methodName, JsonElement jsonParams, String callback) {
		ServiceBean bean = beans.get(beanName);
		try {
			Object result = bean.invoke(methodName, jsonParams);
			if (callback != null) {
				Log.d("invokeBeanAction", "before gson to json: " + result);
				if (result != null) {
					result = gson.toJson(result);
				}
				Log.d("invokeBeanAction", "after gson to json: " + result);
				webView.loadUrl("javascript:mobileLite.doCallback(" + result + ", " + callback + ")");
			}
		} catch (SecurityException e) {
		} catch (JsonSyntaxException e) {
		} catch (IllegalArgumentException e) {
		}
	}

	public void definePageBean(String name, Object bean) {
		if (bean.getClass().isAnnotationPresent(Service.class)) {
			beans.put(name, new ServiceBean(name, bean));
		}
	}
	
	/*
	public void onPageReady() {
		//webView.loadUrl("javascript:liteEngine.initBeanProxy(" + gson.toJson(beans.keySet()) + ")");
		Log.d("onPageReady:", gson.toJson(getServiceBeanDefinitions()));
   		//Toast.makeText(webView.getContext(), "test", 200).show();
		webView.loadUrl("javascript:mobileLite.initBeans(" + gson.toJson(getServiceBeanDefinitions()) + ")");
	}
	*/
	
	List<ServiceBeanDefinition> getServiceBeanDefinitions() {
		List<ServiceBeanDefinition> defs = new ArrayList<ServiceBeanDefinition>();
		for (ServiceBean serviceBean : beans.values()) {
			defs.add(serviceBean.getServiceBeanDefinition());
		}
		return defs;
	}
	
	String getServiceBeanDefinitionJson() {
		return gson.toJson(getServiceBeanDefinitions());
	}
	
	public void loadUrl(String url) {
		webView.getSettings().setJavaScriptEnabled(true);
		if(Build.VERSION.RELEASE.startsWith("2.3")) {
			webView.setWebViewClient(new GingerbreadWebViewClient(this));
		}
		else {
			webView.setWebViewClient(new LiteWebViewClient(this));
			webView.addJavascriptInterface(this, "_mobileLiteProxy_");
		}
		webView.setWebChromeClient(new LiteWebChromeClient());
		webView.loadUrl(url);
	}
	
}
