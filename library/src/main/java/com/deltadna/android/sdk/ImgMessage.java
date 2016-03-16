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

import android.content.res.Configuration;
import android.graphics.Rect;

import com.deltadna.android.sdk.listeners.RequestListener;
import com.deltadna.android.sdk.net.CancelableRequest;
import com.deltadna.android.sdk.net.NetworkManager;
import com.deltadna.android.sdk.net.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;
import java.util.Vector;

/**
 * Wrapper around an Engage request JSON response that parses and manages the
 * data for convenience.
 * 
 * In particular defines several inner classes to define the popup layout and behaviours.
 * 
 * Can be invoked from the done() callback of a 
 */
public class ImgMessage {
    
	static public final int ERROR_NONE = 0;
	static public final int ERROR_NODATA = -1;
	static public final int ERROR_IMGFETCHFAILED = -2;

	static public final String ACTION_DISMISS = "dismiss";
	static public final String ACTION_ACTION = "action";

	static public final String ALIGN_CENTER = "center";
	static public final String ALIGN_LEFT = "left";
	static public final String ALIGN_RIGHT = "right";
	static public final String ALIGN_TOP = "top";
	static public final String ALIGN_BOTTOM = "bottom";

	static public final String MASK_DIMMED = "dimmed";
	static public final String MASK_CLEAR = "clear";
	
	static public final int METRICTYPE_PIXELS = 0;
	static public final int METRICTYPE_PERCENTAGE = 1;

	private int mCurrentError = ERROR_NONE;

	private PrepareCb mCb = null;

	private boolean mPrepared = false;

	private String mJson = null;

	private String mTransactionID = null;

	private Background mBackground = null;
	private Vector<Button> mButtons = null;
	private Shim mShim = null;

	private String mImgUrl = null;
	private int mImgWidth = -1;
	private int mImgHeight = -1;
	private String mImgFormat = null;

	private JSONObject mParameters = null;
    
    private final NetworkManager manager;
    
    private CancelableRequest request;
    
    /**
     * Creates an instance from a JSON string.
     * 
     * @throws JSONException if the JSON is invalid
     */
    public ImgMessage(NetworkManager manager, String json)
            throws JSONException {
        
        this.manager = manager;
        
		JSONObject image = null;

		mJson = json;

		if(mJson != null){
			JSONObject data = new JSONObject(mJson);
			if(data != null){
				image = data.getJSONObject("image");
				if(image != null){
					mTransactionID = data.getString("transactionID");
					try{
						mParameters = data.getJSONObject("parameters");
					}catch(JSONException e){}

					mImgWidth = image.getInt("width");
					mImgHeight = image.getInt("height");
					mImgFormat = image.getString("format");
					mImgUrl = image.getString("url");

					JSONObject layout = image.getJSONObject("layout");
					if(layout != null){
						JSONObject spritemap = image.getJSONObject("spritemap");
						if(spritemap != null){
							JSONObject layoutLandscape = null;
							JSONObject layoutPortrait = null;

							try{
								layoutLandscape = layout.getJSONObject("landscape");
							}catch(JSONException e){}
							try{
								layoutPortrait = layout.getJSONObject("portrait");
							}catch(JSONException e){}

							mBackground = new Background(spritemap.getJSONObject("background"),
									(layoutLandscape == null) ? null : layoutLandscape.getJSONObject("background"),
											(layoutPortrait == null) ? null : layoutPortrait.getJSONObject("background"));

							JSONArray buttons = spritemap.getJSONArray("buttons");
							JSONArray buttonLayoutLandscape = (layoutLandscape == null) ? null : layoutLandscape.getJSONArray("buttons");
							JSONArray buttonLayoutPortrait = (layoutPortrait == null) ? null : layoutPortrait.getJSONArray("buttons");
							int cnt = 0;
							while(cnt < buttons.length()){
								if(mButtons == null){
									mButtons = new Vector<Button>();
								}
								mButtons.add(new Button(buttons.getJSONObject(cnt),
										(buttonLayoutLandscape == null) ? null : buttonLayoutLandscape.getJSONObject(cnt),
												(buttonLayoutPortrait == null) ? null : buttonLayoutPortrait.getJSONObject(cnt)));
								++cnt;
							}
						}
					}

					JSONObject shim = image.getJSONObject("shim");
					if(shim != null){
						mShim = new Shim(shim);
					}
				}
			}
		}

		if(image == null){
			mCurrentError = ERROR_NODATA;

		}
	}
	/**
	 * Gets the Error state of the ImgMessage.
	 * 
	 * May be one of ERROR_NONE, ERROR_NODATA, ERROR_IMGFETCHFAILED
	 * 
	 * @return The error state.
	 */
	public int currentError(){
		return mCurrentError;
	}
	/**
	 * Gets the prepared state of the ImgMessage i.e. is it ready to use.
	 * 
	 * @return TRUE if the ImgMessage is ready to use, FALSE otherwise.
	 */
	public boolean prepared(){
		return mPrepared;
	}
	/**
	 * Prepare the ImgMessage for use. Since the ImgMessage will have at least an image
	 * it will have to be fetched before the message can be used.
	 * 
	 * An optional callback interface can be given to catch success/fail for the preparation,
	 * if the ImgMessage is prepared already then PrepareCb.ready() will immediately be called.
	 * 
	 * @param cb Optional callback interface.
	 */
	public void prepare(PrepareCb cb){
		if(!mPrepared){
			mCb = cb;

			// do we have data?
			if(mImgUrl != null){
				// do we have an image?
                final File img = new File(imgFilename());
                if (!img.exists() && request == null) {
                    request = manager.fetch(mImgUrl, img, new FetchCallback());
                } else {
					mPrepared = true;
				}
			}else{
				mCurrentError = ERROR_NODATA;
				if(mCb != null){
					mCb.error(this);
				}
			}
		}

		if(mPrepared){
			if(mCb != null){
				mCb.ready(this);
			}
		}
	}
	/**
	 * Gets the underlying JSON data.
	 * 
	 * @return The json data.
	 */
	public String json(){
		return mJson;
	}
	/**
	 * Gets the Engage transaction id.
	 * 
	 * @return The Engage transaction id.
	 */
	public String transactionID(){
		return mTransactionID;
	}
	/**
	 * Get the Background description object.
	 * 
	 * @return The Background object, null on error.
	 */
	public Background background(){
		return mBackground;
	}
	/**
	 * Gets the number of buttons on the message popup.
	 * 
	 * @return The number of buttons on the popup.
	 */
	public int buttonCount(){
		return mButtons == null ? 0 : mButtons.size();
	}
	/**
	 * Gets the Button description at the given index.
	 * 
	 * @param idx The index of the button to fetch.
	 * 
	 * @return The Button object at the index, null on error.
	 */
	public Button button(int idx){
		return mButtons == null ? null : mButtons.elementAt(idx);
	}
	/**
	 * Get the Shim description object.
	 * 
	 * @return The Shim object, null on error.
	 */
	public Shim shim(){
		return mShim;
	}
	/**
	 * Gets the image URL.
	 * 
	 * @return Gets the image URL, null on error.
	 */
	public String imgUrl(){
		return mImgUrl;
	}
    
	/**
	 * Gets the image filename if it exists.
	 * 
	 * @return The local image filename if it exists.
	 */
    public String imgFilename(){
        return DDNA.instance().getEngageStoragePath()
                + "/engageimg_" + mTransactionID
                + '.' + mImgFormat;
    }
    
	/**
	 * Gets the image width.
	 * 
	 * @return The image width.
	 */
	public int imgWidth(){
		return mImgWidth;
	}
	/**
	 * Gets the image height.
	 * 
	 * @return The image height.
	 */
	public int imgHeight(){
		return mImgHeight;
	}
	/**
	 * Gets the image format string.
	 * 
	 * @return The image format string.
	 */
	public String imgFormat(){
		return mImgFormat;
	}
	/**
	 * Gets the user defined parameters of the message.
	 * 
	 * @return The user parameters as a JSON object.
	 */
	public JSONObject parameters(){
		return mParameters;
	}
	/**
	 * Cleans up resources associated with this ImgMessage.
	 */
	public void cleanUp(){
        if (request != null) {
            request.cancel();
        }
        
		String filename = imgFilename();
		File img = new File(filename);
		if(img.exists()){
			img.delete();
		}
	}
	/**
	 * Recalculates the layouts, assumes that the larger dimension will be portrait
	 * vertical.
	 * 
	 * @param orientation The current device orientation.
	 * @param screenWidth The device screen width.
	 * @param screenHeight The device screen height.
	 */
	public void init(int orientation, int screenWidth, int screenHeight){
		// calculate landscape/portrait based on given widths and heights
		int realWidth = screenWidth < screenHeight ? screenWidth : screenHeight;
		int realHeight = screenHeight > screenWidth ? screenHeight : screenWidth;

		// pass screen width and height to background
		mBackground.init(orientation, realWidth, realHeight);
		
		int cnt = 0;
		while(cnt < mButtons.size()){
			mButtons.elementAt(cnt).init(orientation,
					mBackground.layout(Configuration.ORIENTATION_PORTRAIT),
					mBackground.layout(Configuration.ORIENTATION_LANDSCAPE));
			++cnt;
		}
	}
    
	/**
	 * An interface for reporting status of ImgMessage preparation. Implementation should
	 * be passed to prepare().
	 */
	static public interface PrepareCb{
		/**
		 * Called when an ImgMessage is successfully prepared.
		 * @param src The source image message.
		 */
		public abstract void ready(ImgMessage src);
		/**
		 * Called on error.
		 * @param src The source image message.
		 */
		public abstract void error(ImgMessage src);
	}
	/**
	 * Description of an image popup background.
	 */
	static public class Background extends ImageBase{
		/**
		 * Layout data for a background.
		 */
		public class Layout{
			private String mType = "cover";

			private String mHAlign = ALIGN_CENTER;
			private String mVAlign = ALIGN_CENTER;

			private int mPadLeft = 0;
			private int mPadLeftUnits = METRICTYPE_PIXELS;
			private int mPadRight = 0;
			private int mPadRightUnits = METRICTYPE_PIXELS;
			private int mPadTop = 0;
			private int mPadTopUnits = METRICTYPE_PIXELS;
			private int mPadBottom = 0;
			private int mPadBottomUnits = METRICTYPE_PIXELS;

			private float mScale = 1.0f;
			
			private Rect mFrame = null;
			/**
			 * The horizontal background alignment behaviour.
			 * 
			 * @return The horizontal alignment behaviour.
			 */
			public String hAlign(){
				return mHAlign;
			}
			/**
			 * The vertical background alignment behaviour.
			 * 
			 * @return The vertical alignment behaviour.
			 */
			public String vAlign(){
				return mVAlign;
			}

			/**
			 * The amount to pad to the left of the image.
			 * 
			 * Unit type is obtained from padLeftUnits().
			 * 
			 * @return The amount to pad to the left.
			 */
			public int padLeft(){
				return mPadLeft;
			}
			/**
			 * The unit type for the left pad metric.
			 * 
			 * @return The unit type for the left pad.
			 */
			public int padLeftUnits(){
				return mPadLeftUnits;
			}
			/**
			 * The amount to pad to the right of the image.
			 * 
			 * Unit type is obtained from padRightUnits().
			 * 
			 * @return The amount to pad to the right.
			 */
			public int padRight(){
				return mPadRight;
			}
			/**
			 * The unit type for the right pad metric.
			 * 
			 * @return The unit type for the right pad.
			 */
			public int padRightUnits(){
				return mPadRightUnits;
			}
			/**
			 * The amount to pad to the top of the image.
			 * 
			 * Unit type is obtained from padTopUnits().
			 * 
			 * @return The amount to pad to the top.
			 */
			public int padTop(){
				return mPadTop;
			}
			/**
			 * The unit type for the top pad metric.
			 * 
			 * @return The unit type for the top pad.
			 */
			public int padTopUnits(){
				return mPadTopUnits;
			}
			/**
			 * The amount to pad to the bottom of the image.
			 * 
			 * Unit type is obtained from padBottomUnits().
			 * 
			 * @return The amount to pad to the bottom.
			 */
			public int padBottom(){
				return mPadBottom;
			}
			/**
			 * The unit type for the bottom pad metric.
			 * 
			 * @return The unit type for the bottom pad.
			 */
			public int padBottomUnits(){
				return mPadBottomUnits;
			}
			/**
			 * The overall background scale factor.
			 * 
			 * @return The scale factor normalised to 1.0f
			 */
			public float scale(){
				return mScale;
			}
			/**
			 * The screen frame of the background calculated for a previous call
			 * to init() with screen height and width.
			 * 
			 * @return The screen rect, null if it has not been initialised.
			 */
			public Rect frame(){
				return mFrame;
			}
			/**
			 * Initialises the screen rect using the specified screen width and height
			 * and applying the layout metrics.
			 * 
			 * @param screenWidth The screen width to use.
			 * @param screenHeight The screen height to use.
			 */
			public void init(int screenWidth, int screenHeight){
				if(mFrame == null){
					mFrame = new Rect();

					int tp = 0;
					int lp = 0;
					int bp = 0;
					int rp = 0;

					// if "contain" calculate padding
					if(mType.equalsIgnoreCase("contain")){
						if(mPadTopUnits == METRICTYPE_PIXELS){
							tp = mPadTop;
						}else{
							tp = (int)((mPadTop / 100.0) * screenHeight);
						}
						if(mPadLeftUnits == METRICTYPE_PIXELS){
							lp = mPadLeft;
						}else{
							lp = (int)((mPadLeft / 100.0) * screenWidth);
						}
						if(mPadBottomUnits == METRICTYPE_PIXELS){
							bp = mPadBottom;
						}else{
							bp = (int)((mPadBottom / 100.0) * screenHeight);
						}
						if(mPadRightUnits == METRICTYPE_PIXELS){
							rp = mPadRight;
						}else{
							rp = (int)((mPadRight / 100.0) * screenWidth);
						}
					}
					
					// calculate scales
					float sw = (screenWidth - (lp + rp)) / (float)mImgW;
					float sh = (screenHeight - (tp + bp)) / (float)mImgH;
					
					mScale = sw < sh ? sw : sh;
					
					// calculate the width and height
					int pWidth = (int)(mImgW * mScale);
					int pHeight = (int)(mImgH * mScale);
					
					// calculate alignment
					int x = lp;
					int y = tp;

					if(mHAlign.equalsIgnoreCase(ImgMessage.ALIGN_CENTER)){
						x = lp + ((screenWidth - (pWidth + lp + rp)) / 2);
					}else if(mHAlign.equalsIgnoreCase(ImgMessage.ALIGN_RIGHT)){
						x = screenWidth - (pWidth + rp);
					}
					if(mVAlign.equalsIgnoreCase(ImgMessage.ALIGN_CENTER)){
						y = tp + ((screenHeight - (pHeight + tp + bp)) / 2);
					}else if(mVAlign.equalsIgnoreCase(ImgMessage.ALIGN_BOTTOM)){
						y = screenHeight - (pHeight + bp);
					}
					mFrame.set(x, y, x  + pWidth, y + pHeight);
				}
			}
		}

		private Layout mLandscape = null;
		private Layout mPortrait = null;
		/**
		 * Constructor.
		 * 
		 * @param sprite JSON data for the img sprite.
		 * @param layoutLandscape JSON data for the landscape layout.
		 * @param layoutPortrait JSON data for the portrait layout.
		 */
		protected Background(JSONObject sprite, JSONObject layoutLandscape, JSONObject layoutPortrait){
			super(sprite, layoutLandscape, layoutPortrait);
			JSONObject tempObj = null;
			String tempStr = null;

			if(layoutLandscape != null){
				mLandscape = new Layout();
				try {
					tempObj = layoutLandscape.getJSONObject("contain");
					mLandscape.mType = "contain";
				} catch (JSONException e) {
					try {
						tempObj = layoutLandscape.getJSONObject("cover");
						mLandscape.mType = "cover";
					} catch (JSONException e2) {}
				}

				if(tempObj != null){
					try {
						mLandscape.mHAlign = tempObj.getString("halign");
					} catch (JSONException e) {}
					try {
						mLandscape.mVAlign = tempObj.getString("valign");
					} catch (JSONException e) {}

					try {
						tempStr = tempObj.getString("left");
						mLandscape.mPadLeft = getInteger(tempStr);
						mLandscape.mPadLeftUnits = getMetricUnit(tempStr);
					} catch (JSONException e) {}
					try {
						tempStr = tempObj.getString("right");
						mLandscape.mPadRight = getInteger(tempStr);
						mLandscape.mPadRightUnits = getMetricUnit(tempStr);
					} catch (JSONException e) {}
					try {
						tempStr = tempObj.getString("top");
						mLandscape.mPadTop = getInteger(tempStr);
						mLandscape.mPadTopUnits = getMetricUnit(tempStr);
					} catch (JSONException e) {}
					try {
						tempStr = tempObj.getString("bottom");
						mLandscape.mPadBottom = getInteger(tempStr);
						mLandscape.mPadBottomUnits = getMetricUnit(tempStr);
					} catch (JSONException e) {}
				}
			}

			if(layoutPortrait != null){
				mPortrait = new Layout();
				try {
					tempObj = layoutPortrait.getJSONObject("contain");
					mLandscape.mType = "contain";
				} catch (JSONException e) {
					try {
						tempObj = layoutPortrait.getJSONObject("cover");
						mLandscape.mType = "cover";
					} catch (JSONException e2) {}
				}

				if(tempObj != null){
					try {
						mPortrait.mHAlign = tempObj.getString("halign");
					} catch (JSONException e) {}
					try {
						mPortrait.mVAlign = tempObj.getString("valign");
					} catch (JSONException e) {}

					try {
						tempStr = tempObj.getString("left");
						mPortrait.mPadLeft = getInteger(tempStr);
						mPortrait.mPadLeftUnits = getMetricUnit(tempStr);
					} catch (JSONException e) {}
					try {
						tempStr = tempObj.getString("right");
						mPortrait.mPadRight = getInteger(tempStr);
						mPortrait.mPadRightUnits = getMetricUnit(tempStr);
					} catch (JSONException e) {}
					try {
						tempStr = tempObj.getString("top");
						mPortrait.mPadTop = getInteger(tempStr);
						mPortrait.mPadTopUnits = getMetricUnit(tempStr);
					} catch (JSONException e) {}
					try {
						tempStr = tempObj.getString("bottom");
						mPortrait.mPadBottom = getInteger(tempStr);
						mPortrait.mPadBottomUnits = getMetricUnit(tempStr);
					} catch (JSONException e) {}
				}
			}
		}
		/**
		 * Returns the layout for the specified orientation. If a layout for the
		 * given orientation is not present any existing layout is returned.
		 * 
		 * @param orientation The requested orientation.
		 * 
		 * @return The layout, null on error.
		 */
		public Layout layout(int orientation){
			if(orientation == Configuration.ORIENTATION_LANDSCAPE){
				return mLandscape != null ? mLandscape : mPortrait;
			}else{
				return mPortrait != null ? mPortrait : mLandscape;
			}
		}
		/**
		 * The units defined by the given layout value.
		 * 
		 * May be one of METRICTYPE_PIXELS or METRICTYPE_PERCENTAGE
		 * 
		 * @param s The data string to parse.
		 * 
		 * @return The unit type.
		 */
		private int getMetricUnit(String s){
			int result = METRICTYPE_PIXELS;

			if(s != null){
				if(s.contains("%")){
					result = METRICTYPE_PERCENTAGE;
				}else if(s.toUpperCase(Locale.getDefault()).contains("px")){
					result = METRICTYPE_PIXELS;
				}
			}

			return result;
		}
		/**
		 * Parses the integer from a layout metric string.
		 * 
		 * @param s The metric string.
		 * 
		 * @return The integer value represented by the string.
		 */
		private int getInteger(String s){
			int result = 0;

			if(s != null){
				String intStr = null;
				int idx = s.indexOf("%");
				if(idx < 0){
					idx = s.indexOf("px");
				}

				if(idx > -1){
					intStr = s.substring(0, idx);
					result = Integer.parseInt(intStr);
				}
			}

			return result;
		}
		/**
		 * Initialises this Background object using the orientation and width/height.
		 * 
		 * @param orientation The current device orientation.
		 * @param screenWidth The screen width.
		 * @param screenHeight The screen height.
		 */
		public void init(int orientation, int screenWidth, int screenHeight){
			if(mPortrait != null){
				if((mLandscape != null) || (orientation == Configuration.ORIENTATION_PORTRAIT)){
					mPortrait.init(screenWidth, screenHeight);
				}else{
					mPortrait.init(screenHeight, screenWidth);
				}
			}
			if(mLandscape != null){
				if((mPortrait != null) || (orientation == Configuration.ORIENTATION_LANDSCAPE)){
					mLandscape.init(screenHeight, screenWidth);
				}else{
					mLandscape.init(screenWidth, screenHeight);
				}
			}
		}
	}
	/**
	 * Description of an image message button.
	 */
	static public class Button extends ImageBase{
		/**
		 * Layout data for a button.
		 */
		public class Layout{
			private int mX = -1;
			private int mY = -1;

			private Rect mFrame = null;
			/**
			 * The popup relative x position of the button.
			 * 
			 * @return The popup relative x position.
			 */
			public int x(){
				return mX;
			}
			/**
			 * The popup relative y position of the button.
			 * 
			 * @return The popup relative y position.
			 */
			public int y(){
				return mY;
			}
			/**
			 * The popup relative button frame calculated by init().
			 * 
			 * @return A popup relative frame, null if not calculated yet.
			 */
			public Rect frame(){
				return mFrame;
			}
			/**
			 * Initialises the button frame to the given popup frame and scale.
			 * 
			 * @param frame The popup frame.
			 * @param scale The popup scale.
			 */
			public void init(Rect frame, float scale){
				if(mFrame == null){
					mFrame = new Rect();
					
					int btnX = frame.left;
					int btnY = frame.top;

					btnX = frame.left + (int)(mX * scale);
					btnY = frame.top + (int)(mY * scale);
					mFrame.set(btnX, btnY, btnX  + (int)(mImgW * scale), btnY + (int)(mImgH * scale));
				}
			}

		}

		private Layout mLandscape = null;
		private Layout mPortrait = null;
		/**
		 * Constructor.
		 * 
		 * @param sprite JSON data for the img sprite.
		 * @param layoutLandscape JSON data for the landscape layout.
		 * @param layoutPortrait JSON data for the portrait layout.
		 */
		protected Button(JSONObject sprite, JSONObject layoutLandscape, JSONObject layoutPortrait){
			super(sprite, layoutLandscape, layoutPortrait);
			if(layoutLandscape != null){
				mLandscape = new Layout();
				try {
					mLandscape.mX = layoutLandscape.getInt("x");
				} catch (JSONException e) {}
				try {
					mLandscape.mY = layoutLandscape.getInt("y");
				} catch (JSONException e) {}
			}

			if(layoutPortrait != null){
				mPortrait = new Layout();
				try {
					mPortrait.mX = layoutPortrait.getInt("x");
				} catch (JSONException e) {}
				try {
					mPortrait.mY = layoutPortrait.getInt("y");
				} catch (JSONException e) {}
			}
		}
		/**
		 * Returns the layout for the specified orientation. If a layout for the
		 * given orientation is not present any existing layout is returned.
		 * 
		 * @param orientation The requested orientation.
		 * 
		 * @return The layout, null on error.
		 */
		public Layout layout(int orientation){
			if(orientation == Configuration.ORIENTATION_LANDSCAPE){
				return mLandscape != null ? mLandscape : mPortrait;
			}else{
				return mPortrait != null ? mPortrait : mLandscape;
			}
		}
		/**
		 * Initialises the button to the given orientation and landscape/portrait layouts.
		 * 
		 * @param orientation The current device orientation.
		 * @param portraitBg The portrait popup layout.
		 * @param landscapeBg The landscape popup layout.
		 */
		public void init(int orientation, Background.Layout portraitBg, Background.Layout landscapeBg){
			if(mPortrait != null){
				if((mLandscape != null) || (orientation == Configuration.ORIENTATION_PORTRAIT)){
					mPortrait.init(portraitBg.frame(), portraitBg.scale());
				}else{
					mPortrait.init(portraitBg.frame(), portraitBg.scale());
				}
			}
			if(mLandscape != null){
				if((mPortrait != null) || (orientation == Configuration.ORIENTATION_LANDSCAPE)){
					mLandscape.init(landscapeBg.frame(), landscapeBg.scale());
				}else{
					mLandscape.init(landscapeBg.frame(), landscapeBg.scale());
				}
			}
		}
	}
	/**
	 * The image sprite base for popup render.
	 */
	static private class ImageBase{
		protected int mImgX = -1;
		protected int mImgY = -1;
		protected int mImgW = -1;
		protected int mImgH = -1;

		protected Rect mImgRect = null;

		protected Action mLandscapeAction = null;

		protected Action mPortraitAction = null;
		/**
		 * Constructor.
		 * 
		 * @param sprite JSON data for the img sprite.
		 * @param layoutLandscape JSON data for the landscape layout action.
		 * @param layoutPortrait JSON data for the portrait layout action.
		 */
		protected ImageBase(JSONObject sprite, JSONObject layoutLandscape, JSONObject layoutPortrait){
			JSONObject tempObj = null;

			try {
				mImgX = sprite.getInt("x");
			} catch (JSONException e) {}
			try {
				mImgY = sprite.getInt("y");
			} catch (JSONException e) {}
			try {
				mImgW = sprite.getInt("width");
			} catch (JSONException e) {}
			try {
				mImgH = sprite.getInt("height");
			} catch (JSONException e) {}

			mImgRect = new Rect(mImgX, mImgY, mImgX + mImgW, mImgY + mImgH);

			if(layoutLandscape != null){
				try{
					tempObj = layoutLandscape.getJSONObject("action");
					mLandscapeAction = new Action(tempObj);
				} catch (JSONException e) {}
			}

			if(layoutPortrait != null){
				try{
					tempObj = layoutPortrait.getJSONObject("action");
					mPortraitAction = new Action(tempObj);
				} catch (JSONException e) {}
			}
		}
		/**
		 * The x location of the sprite on the base image in pixels.
		 * 
		 * @return The x position on the image.
		 */
		public int imgX(){
			return mImgX;
		}
		/**
		 * The y location of the sprite on the base image in pixels.
		 * 
		 * @return The y position on the image.
		 */
		public int imgY(){
			return mImgY;
		}
		/**
		 * The width of the sprite on the base image in pixels.
		 * 
		 * @return The width on the image.
		 */
		public int imgW(){
			return mImgW;
		}
		/**
		 * The height of the sprite on the base image in pixels.
		 * 
		 * @return The height on the image.
		 */
		public int imgH(){
			return mImgH;
		}
		/**
		 * The sprite rect.
		 * 
		 * @return The sprite rect.
		 */
		public Rect imgRect(){
			return mImgRect;
		}
		/**
		 * The action for the given orientation.
		 * 
		 * @param orientation The device orientation.
		 * 
		 * @return The action.
		 */
		public Action action(int orientation){
			if(orientation == Configuration.ORIENTATION_LANDSCAPE){
				return mLandscapeAction != null ? mLandscapeAction : mPortraitAction;
			}else{
				return mPortraitAction != null ? mPortraitAction : mLandscapeAction;
			}
		}
	}
	/**
	 * Description of the screen outside the popup.
	 */
	static public class Shim{
		private String mMask = null;
		private Action mAction = null;
		/**
		 * Constructor.
		 * 
		 * @param json JSON data describing the shim.
		 */
		protected Shim(JSONObject json){
			JSONObject tempObj = null;
			try {
				mMask = json.getString("mask");
			} catch (JSONException e) {}
			try{
				tempObj = json.getJSONObject("action");
				mAction = new Action(tempObj);
			} catch (JSONException e) {}
		}
		/**
		 * Fill mask type.
		 * 
		 * @return The fill type.
		 */
		public String mask(){
			return mMask;
		}
		/**
		 * The touch action.
		 * 
		 * @return The touch action.
		 */
		public Action action(){
			return mAction;
		}
	}
	/**
	 * Encapsulates an Action.
	 */
	static public class Action{
		private String mType = null;
		private String mValue = null;
		/**
		 * Constructor.
		 * 
		 * @param json JSON data describing an Action.
		 */
		protected Action(JSONObject json){
			try {
				mType = json.getString("type");
			} catch (JSONException e) {}
			try {
				mValue = json.getString("value");
			} catch (JSONException e) {}
		}
		/**
		 * Action type string.
		 * 
		 * @return The action type.
		 */
		public String type(){
			return mType;
		}
		/**
		 * Optional Action value.
		 * 
		 * @return The action value, null if none.
		 */
		public String value(){
			return mValue;
		}
	}
    
    private final class FetchCallback implements RequestListener<Response<File>> {

        @Override
        public void onSuccess(Response<File> result) {
            mCurrentError = ERROR_NONE;
            mPrepared = true;

            if (mCb != null) {
                mCb.ready(ImgMessage.this);
            }
            request = null;
        }

        @Override
        public void onFailure(Throwable t) {
            mCurrentError = ERROR_IMGFETCHFAILED;

            if (mCb != null) {
                mCb.error(ImgMessage.this);
            }
            request = null;
        }
    }
}
