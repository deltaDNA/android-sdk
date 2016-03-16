/*
 * Copyright (c) 2016 deltaDNA Ltd. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.deltadna.android.sdk;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.deltadna.android.sdk.listeners.EngageListener;
import com.deltadna.android.sdk.listeners.ImageMessageResultListener;

/**
 * This {@link Activity} is intended as the default method to display
 * an Image Messaging request.
 * <p>
 * You can start the request using
 * {@link DDNA#requestEngagement(String, EngageListener)} or
 * {@link DDNA#requestEngagement(String, JSONObject, EngageListener)}
 * and use an instance of the
 * {@link com.deltadna.android.sdk.listeners.ImageMessageListener}
 * class for the listener which by default will open this activity,
 * for example:
 * <pre><code>
 * DDNA.inst().requestEngagement(
 *     "imageMessage",
 *     new ImageMessageListener(MyActivity.this, MY_REQUEST_CODE)) {
 *         {@literal@}Override
 *         public void onFailure(ResponseException e) {
 *             // error handling code
 *         }
 *     }
 * }
 * </code></pre>
 * <p>
 * Following this request you will need to override
 * {@link #onActivityResult(int, int, Intent)} in your {@link Activity}
 * class to handle the result, for example:
 * <pre><code>
 * {@literal@}Override
 * public void onActivityResult(int requestCode, int resultCode, Intent data) {
 *     super.onActivityResult(requestCode, resultCode, data);
 *     
 *     if (requestCode == MY_REQUEST_CODE) {
 *         ImageMessageActivity.handleResult(
 *             resultCode, data, new ImageMessageResultListener() {
 *                 {@literal@}Override
 *                 public void onAction(String value, String params) {
 *                     // do something with value and params
 *                 }
 *                 
 *                 {@literal@}Override
 *                 public void onCancelled() {
 *                     // perform an alternative action
 *                 }
 *             });
 *     }
 * }
 * </code></pre>
 * 
 * @see DDNA#requestEngagement(String, EngageListener)
 * @see DDNA#requestEngagement(String, JSONObject, EngageListener)
 * @see com.deltadna.android.sdk.listeners.ImageMessageListener
 */
public class ImageMessageActivity extends Activity {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + ImageMessageActivity.class.getSimpleName();
    
    private static final String EXTRA_IMG_MSG = "img_msg";
    private static final String EXTRA_VALUE = "value";
    private static final String EXTRA_PARAMS = "params";
    
	private ImgMessage mImgMessageData = null;

	private ProgressBar mSpinner = null;
	private ImgMessageView mImgMessageView = null;

	private Bitmap mImgBitmap = null;
	private ImgMessage.Background mMsgBg = null;
	private ImgMessage.Shim mMsgShim = null;

	int mScreenWidth = 0;
	int mScreenHeight = 0;
	/**
	 * Custom view that overrides onDraw() to display the bitmap fragments according to the ImgMessage
	 * requirements.
	 */
	private class ImgMessageView extends View implements View.OnTouchListener{
		/**
		 * Constructor override to set the touch listener.
		 * 
		 * @param context The widget context.
		 */
		public ImgMessageView(Context context){
			super(context);
			this.setOnTouchListener(this);
		}
		/**
		 * Constructor override to set the touch listener.
		 * 
		 * @param context The widget context.
		 * @param attrs The widget attributes.
		 */
		public ImgMessageView(Context context, AttributeSet attrs){
			super(context, attrs);
			this.setOnTouchListener(this);
		}
		@Override
		public boolean performClick(){
			return super.performClick();
		}
		@Override
		protected void onLayout(boolean changed, int left, int top, int right, int bottom){
			mScreenWidth = right;
			mScreenHeight = bottom;

			if(mImgBitmap == null){
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPreferredConfig = Bitmap.Config.ARGB_8888;
				mImgBitmap = BitmapFactory.decodeFile(mImgMessageData.imgFilename(), options);
			}

			mImgMessageData.init(getResources().getConfiguration().orientation, mScreenWidth, mScreenHeight);

			mMsgBg = mImgMessageData.background();
			mMsgShim = mImgMessageData.shim();
			
			super.onLayout(changed, left, top, right, bottom);
		}
		@Override
		protected void onDraw(Canvas canvas){
			if(mImgBitmap != null){
				if(ImgMessage.MASK_DIMMED.equalsIgnoreCase(mMsgShim.mask())){
					canvas.drawARGB(0x66, 0x0, 0x0, 0x0);
				}else{
					canvas.drawARGB(0x0, 0x0, 0x0, 0x0);
				}
				int orientation = getContext().getResources().getConfiguration().orientation;

				// draw background
				canvas.drawBitmap(mImgBitmap, mMsgBg.imgRect() , mMsgBg.layout(orientation).frame(), null);

				// draw buttons
				int cnt = 0;
				ImgMessage.Button btn = null;
				while(cnt < mImgMessageData.buttonCount()){
					btn = mImgMessageData.button(cnt);
					canvas.drawBitmap(mImgBitmap, btn.imgRect(), btn.layout(orientation).frame(), null);
					++cnt;
				}

			}else{
				canvas.drawARGB(0x0, 0x0, 0x0, 0x0);
			}
		}
		@SuppressLint("ClickableViewAccessibility")
		@Override		
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getAction()){
			case MotionEvent.ACTION_UP:{
				if(mImgMessageData != null){
					ImgMessage.Action action = null;
					int orientation = getContext().getResources().getConfiguration().orientation;

					// if the touch is on the popup then test buttons
					if(mMsgBg.layout(orientation).frame().contains((int)event.getX(), (int)event.getY())){
						int cnt = 0;
						ImgMessage.Button btn = null;
						while((action == null) && (cnt < mImgMessageData.buttonCount())){
							btn = mImgMessageData.button(cnt);
							if(btn.layout(orientation).frame().contains((int)event.getX(), (int)event.getY())){
								action = btn.action(orientation);
							}
							++cnt;
						}
						
					}else{
						// touch is outside the popup so use shim action
						action = mMsgShim.action();
					}

					performAction(action);
				}
				break;
			}
			default:
				break;
			}
			return true;
		}
	}
	/**
	 * Handles an action defined by the Delta DNA message.
	 * 
	 * @param action The action to handle.
	 */
	protected void performAction(ImgMessage.Action action){
		if(action != null){
			if(action.type().equalsIgnoreCase(ImgMessage.ACTION_ACTION)){
				Intent resultIntent = new Intent();
				resultIntent.putExtra(EXTRA_VALUE, action.value());
				if(mImgMessageData.parameters() != null){
					resultIntent.putExtra(EXTRA_PARAMS, mImgMessageData.parameters().toString());
				}

				setResult(Activity.RESULT_OK, resultIntent);
				finish();

			}else if(action.type().equalsIgnoreCase(ImgMessage.ACTION_DISMISS)){
				mImgMessageData.cleanUp();
				Intent resultIntent = new Intent();

				setResult(Activity.RESULT_CANCELED, resultIntent);
				finish();
			}
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		RelativeLayout contentLayout = new RelativeLayout(this);
		
		FrameLayout holder = new FrameLayout(this);
		contentLayout.addView(holder, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

		mImgMessageView = new ImgMessageView(this);
		holder.addView(mImgMessageView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		mSpinner = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
		mSpinner.setIndeterminate(true);
		mSpinner.setVisibility(View.VISIBLE);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT);
		contentLayout.addView(mSpinner, params);
		
		setContentView(contentLayout);
		
		String json = getIntent().getStringExtra(EXTRA_IMG_MSG);
		if(json != null){
			try {
				mImgMessageData = new ImgMessage(DDNA.instance().getNetworkManager(), json);
				mImgMessageData.prepare(new PrepareCallback());
			} catch (JSONException e) {
                Log.w(TAG, "Image message JSON is invalid: " + json, e);
			}
		}
	}
    
	@Override
	public void onBackPressed(){
		moveTaskToBack(true);
	}
    
    public static Intent createIntent(Context context, JSONObject response) {
        return new Intent(context, ImageMessageActivity.class)
                .putExtra(EXTRA_IMG_MSG, response.toString());
    }
    
    public static void handleResult(
            int resultCode,
            Intent data,
            ImageMessageResultListener action) {
        
        if (resultCode == RESULT_OK) {
            final Bundle extras = data.getExtras();
            
            action.onAction(
                    extras.getString(EXTRA_VALUE),
                    extras.getString(EXTRA_PARAMS));
        } else if (resultCode == RESULT_CANCELED) {
            action.onCancelled();
        }
    }
    
    private final class PrepareCallback implements ImgMessage.PrepareCb {
        
        @Override
        public void ready(ImgMessage src) {
            mSpinner.setVisibility(View.INVISIBLE);
            mImgMessageView.requestLayout();
        }
        
        @Override
        public void error(ImgMessage src) {
            mImgMessageData.cleanUp();
            
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }
}
