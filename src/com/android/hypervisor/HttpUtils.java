

package com.android.hypervisor;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;  //if we need to transfer parameter , perhaps need to use http post

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import android.util.Log; 



public class HttpUtils {


    private final String TAG = "HttpUtils"; 


    //url: register user to server: private Uri ISSUE_AUTH_TOKEN_URL = Uri.parse
    private  Uri REGISTER_URL = Uri.parse("http://121.40.35.89:8080/mywebapp/Register");
    //url : user login url
    private  Uri LOGIN_URL = Uri.parse("http://121.40.35.89:8080/mywebapp/Login");

    private static final int HTTP_TIMEOUT_MS = 1000;

    // TODO: this should be defined somewhere
    private static final String HTTP_TIMEOUT = "http.connection-manager.timeout";

    //following three is http result: 0, OK, http status 200; 1, http request fails; 2, network is not OK.  
    private static int  HTTP_OK = 0; 
    private static int  HTTP_FAIL = 1; 
    private static int  HTTP_IO   = 2; 

    private final HttpClient mHttpClient;
   
    public HttpUtils() {

        mHttpClient = new DefaultHttpClient();
        HttpParams params = mHttpClient.getParams();
        params.setLongParameter(HTTP_TIMEOUT, HTTP_TIMEOUT_MS);
    }

 
   /*   
   *  user register ; if it is OK, return HttpResponse; if it fails, return null;  
   *
   */
   public HttpResponse userRegister(String mobile, String password ){

        String url = REGISTER_URL.buildUpon()
                .appendQueryParameter("MOBILE", mobile)
                .appendQueryParameter("PASSWORD", password)
                .build().toString();

        Log.v(TAG, ">>>>chenrui>>>url is: " +  url);

        HttpResponse response = null; 

        try {
            HttpGet method = new HttpGet(url);
            response = mHttpClient.execute(method);

            if (response.getStatusLine().getStatusCode() == 200) {
                return response;
            } else {
                Log.i(TAG, "Suggestion request failed");
                return null;
            }
        } catch (IOException e) {
            Log.w(TAG, "Error", e);
            return null;
        }
   

   }

   
   /**
    *  user login; 
    *  input : parameter: mobile and password , client id and device id ; 
    *  output: httpresponse, about  login status
    */
   public HttpResponse userLogin(String mobile, String password, String clientid, String  device_type ){

       String url = LOGIN_URL.buildUpon()
                .appendQueryParameter("MOBILE", mobile)
                .appendQueryParameter("PASSWORD", password)
                .appendQueryParameter("CLIENTID", clientid)
                .appendQueryParameter("DEVICEID", device_type)
                .build().toString();

        Log.v(TAG, ">>>>chenrui>>>url is: " +  url);

        HttpResponse response = null;

        try {
            HttpGet method = new HttpGet(url);
            response = mHttpClient.execute(method);

          //  String str = response.StatusDescription; 

            if (response.getStatusLine().getStatusCode() == 200) {
                return response;
            } else {
                Log.i(TAG, "Suggestion request failed");
                return null;
            }
        } catch (IOException e) {
            Log.w(TAG, "Error", e);
            return null;
        }


  }
      
      /**
      *  check if the user exists; 
      *  if it exists, return true;  if it does not exist, return false; 
      */
      public int userIsExist(String mobile){
          String url = REGISTER_URL.buildUpon()
                .appendQueryParameter("MOBILE", mobile)
                .build().toString();

           Log.v(TAG, ">>>>chenrui>>>url is: " +  url);

           HttpResponse response = null;

          try {
            HttpGet method = new HttpGet(url);
            response = mHttpClient.execute(method);

            if (response.getStatusLine().getStatusCode() == 200) {
                return HTTP_OK;
            } else {
                Log.i(TAG, "Suggestion request failed");
                return HTTP_FAIL;
            }
        } catch (IOException e) {
            Log.w(TAG, "Error", e);
            return HTTP_IO;
        }


      }





}





   

