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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.annotation.Nullable;
import android.util.Log;

import com.deltadna.android.sdk.helpers.ClientInfo;
import com.deltadna.android.sdk.helpers.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

public final class ImageMessage implements Serializable {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + ImageMessage.class.getSimpleName();
    
    private static final String ALIGN_CENTER = "center";
    private static final String ALIGN_RIGHT = "right";
    private static final String ALIGN_BOTTOM = "bottom";
    
    private static final String ACTION_NONE = "none";
    private static final String ACTION_DISMISS = "dismiss";
    private static final String ACTION_ACTION = "action";
    private static final String ACTION_LINK = "link";
    private static final String ACTION_STORE = "store";
    
    static final String MASK_DIMMED = "dimmed";
    
    private static final int METRICTYPE_PIXELS = 0;
    private static final int METRICTYPE_PERCENTAGE = 1;
    
    final String eventParams;
    private final String parameters;
    
    private final String imageUrl;
    private File imageFile;
    
    final Background background;
    private final Vector<Button> buttons;
    final Shim shim;
    
    /**
     * Creates an instance from a JSON response.
     *
     * @param json the response
     *
     * @throws JSONException if the JSON is invalid
     */
    ImageMessage(JSONObject json) throws JSONException {
        eventParams = (json.has("eventParams")
                ? json.getJSONObject("eventParams")
                : new JSONObject())
                .toString();
        parameters = (json.has("parameters")
                ? json.optJSONObject("parameters")
                : new JSONObject()).toString();
        
        final JSONObject image = json.getJSONObject("image");
        imageUrl = image.getString("url");
        
        final JSONObject layout = image.getJSONObject("layout");
        final JSONObject spritemap = image.getJSONObject("spritemap");
        final JSONObject layoutLandscape = layout.optJSONObject("landscape");
        final JSONObject layoutPortrait = layout.optJSONObject("portrait");
        
        background = new Background(
                spritemap.getJSONObject("background"),
                (layoutLandscape == null)
                        ? null
                        : layoutLandscape.getJSONObject("background"),
                (layoutPortrait == null)
                        ? null
                        : layoutPortrait.getJSONObject("background"));
        
        buttons = new Vector<>();
        final JSONArray buttons = spritemap.has("buttons")
                ? spritemap.getJSONArray("buttons")
                : new JSONArray();
        final JSONArray buttonLayoutLandscape =
                (layoutLandscape == null || buttons.length() == 0)
                        ? null
                        : layoutLandscape.getJSONArray("buttons");
        final JSONArray buttonLayoutPortrait =
                (layoutPortrait == null || buttons.length() == 0)
                        ? null
                        : layoutPortrait.getJSONArray("buttons");
        for (int i = 0; i < buttons.length(); i++) {
            this.buttons.add(new Button(
                    buttons.getJSONObject(i),
                    (buttonLayoutLandscape == null)
                            ? null
                            : buttonLayoutLandscape.getJSONObject(i),
                    (buttonLayoutPortrait == null)
                            ? null
                            : buttonLayoutPortrait.getJSONObject(i)));
        }
        
        shim = new Shim(image.getJSONObject("shim"));
    }
    
    /**
     * Gets the prepared state.
     *
     * @return {@code true} if ready to use, else {@code false}
     */
    public boolean prepared() {
        synchronized (this) {
            if (imageFile == null) {
                imageFile = DDNA.instance().getImageMessageStore()
                        .getOnlyIfCached(imageUrl);
            }
        }
        
        return imageFile != null && imageFile.exists();
    }
    
    /**
     * Prepares the Image Message for use, by downloading the image.
     *
     * @param listener  the listener for receiving prepared state
     */
    public void prepare(final PrepareListener listener) {
        if (prepared()) {
            listener.onPrepared(this);
        } else {
            DDNA.instance().getImageMessageStore().getAsync(
                    imageUrl,
                    new ImageMessageStore.Callback<File>() {
                        @Override
                        public void onCompleted(File value) {
                            synchronized (this) {
                                imageFile = value;
                            }
                            
                            listener.onPrepared(ImageMessage.this);
                        }
                        
                        @Override
                        public void onFailed(Throwable reason) {
                            synchronized (this) {
                                imageFile = null;
                            }
                            
                            listener.onError(reason);
                        }
                    });
        }
    }
    
    /**
     * Opens the {@link ImageMessageActivity} for showing this Image Message.
     *
     * @param activity      the {@link Activity} from which the request is
     *                      being started
     * @param requestCode   the request code that will be used in
     *                      {@link Activity#onActivityResult(int, int, Intent)}
     *                      for the result
     *
     * @throws IllegalStateException if the Image Message is not prepared
     */
    public void show(Activity activity, int requestCode) {
        if (!prepared()) throw new IllegalStateException(
                "image message has not been prepared yet");
        
        activity.startActivityForResult(
                ImageMessageActivity.createIntent(activity, this),
                requestCode);
    }
    
    /**
     * Recalculates the layouts, assumes that the larger dimension will be
     * portrait vertical.
     */
    void init(int orientation, int screenWidth, int screenHeight) {
        // calculate landscape/portrait based on given widths and heights
        final int realWidth = screenWidth < screenHeight
                ? screenWidth : screenHeight;
        final int realHeight = screenHeight > screenWidth
                ? screenHeight : screenWidth;
        
        // pass screen width and height to background
        background.init(orientation, realWidth, realHeight);
        
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).init(
                    orientation,
                    background.layout(Configuration.ORIENTATION_PORTRAIT),
                    background.layout(Configuration.ORIENTATION_LANDSCAPE));
        }
    }
    
    /**
     * Gets the user defined parameters of the message.
     *
     * @return the user parameters
     */
    JSONObject parameters() {
        try {
            return new JSONObject(parameters);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to serialise JSON parameters", e);
            return new JSONObject();
        }
    }
    
    Iterator<Button> buttons() {
        return buttons.iterator();
    }
    
    /**
     * Gets the image file.
     *
     * @return the local image file.
     */
    File getImageFile() {
        return imageFile;
    }
    
    /**
     * Creates an Image Message from an Engagement once it has been populated
     * with response data after a successful request.
     * <p>
     * {@code null} may be returned in case the Engagement was not set-up to
     * display an Image Message.
     *
     * @param engagement the Engagement with response data
     *
     * @return  the Image Message created from {@code engagement}, else
     *          {@code null}
     */
    @Nullable
    public static ImageMessage create(Engagement engagement) {
        //noinspection ConstantConditions
        if (engagement.isSuccessful() && engagement.getJson().has("image")) {
            try {
                return new ImageMessage(engagement.getJson());
            } catch (JSONException e) {
                Log.w(TAG, "Failed creating image message", e);
                return null;
            }
        }
        
        return null;
    }
    
    public interface PrepareListener {
        
        /**
         * Notifies the listener that the Image Message has been prepared.
         * <p>
         * In most implementations {@link #show(Activity, int)} should be
         * called, if the application is still in an appropriate state to do
         * so.
         *
         * @param src the prepared Image Message
         */
        void onPrepared(ImageMessage src);
        
        /**
         * Notifies the listener that an error has happened during the
         * preparation request.
         * <p>
         * If this method is called {@link #onPrepared(ImageMessage)} will not
         * be called.
         *
         * @param reason the reason for the error
         */
        void onError(Throwable reason);
    }
    
    /**
     * Description of an image popup background.
     *
     * TODO legacy code
     */
    class Background extends ImageBase {
        
        /**
         * Layout data for a background.
         */
        class Layout implements Serializable {
            
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
                    float sw = (screenWidth - (lp + rp)) / (float) imageW;
                    float sh = (screenHeight - (tp + bp)) / (float) imageH;
                    
                    mScale = (sw < sh && mType.equalsIgnoreCase("contain"))
                            ? sw
                            : sh;
                    
                    // calculate the width and height
                    int pWidth = (int)(imageW * mScale);
                    int pHeight = (int)(imageH * mScale);

                    // calculate alignment
                    int x = lp;
                    int y = tp;

                    if(mHAlign.equalsIgnoreCase(ImageMessage.ALIGN_CENTER)){
                        x = lp + ((screenWidth - (pWidth + lp + rp)) / 2);
                    }else if(mHAlign.equalsIgnoreCase(ImageMessage.ALIGN_RIGHT)){
                        x = screenWidth - (pWidth + rp);
                    }
                    if(mVAlign.equalsIgnoreCase(ImageMessage.ALIGN_CENTER)){
                        y = tp + ((screenHeight - (pHeight + tp + bp)) / 2);
                    }else if(mVAlign.equalsIgnoreCase(ImageMessage.ALIGN_BOTTOM)){
                        y = screenHeight - (pHeight + bp);
                    }
                    
                    mFrame.left = x;
                    mFrame.top = y;
                    mFrame.right = x + pWidth;
                    mFrame.bottom = y + pHeight;
                }
            }
        }
        
        private Layout mLandscape = null;
        private Layout mPortrait = null;
        
        protected Background(
                JSONObject sprite,
                JSONObject layoutLandscape,
                JSONObject layoutPortrait) throws JSONException {
            
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
     *
     * TODO legacy code
     */
    static public class Button extends ImageBase {
        /**
         * Layout data for a button.
         */
        public class Layout implements Serializable {
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
                    
                    final int btnX = frame.left + (int)(mX * scale);
                    final int btnY = frame.top + (int)(mY * scale);
                    
                    mFrame.left = btnX;
                    mFrame.top = btnY;
                    mFrame.right = btnX + (int) (imageW * scale);
                    mFrame.bottom = btnY + (int) (imageH * scale);
                }
            }

        }

        private Layout mLandscape = null;
        private Layout mPortrait = null;
        
        protected Button(
                JSONObject sprite,
                JSONObject layoutLandscape,
                JSONObject layoutPortrait) throws JSONException {
            
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
    private static class ImageBase implements Serializable {
        
        final int imageX;
        final int imageY;
        final int imageW;
        final int imageH;
        
        final Rect imageRect;
        
        @Nullable
        private final BaseAction landscapeAction;
        @Nullable
        private final BaseAction portraitAction;
        
        ImageBase(
                JSONObject sprite,
                @Nullable JSONObject layoutLandscape,
                @Nullable JSONObject layoutPortrait) throws JSONException {
            
            imageX = sprite.getInt("x");
            imageY = sprite.getInt("y");
            imageW = sprite.getInt("width");
            imageH = sprite.getInt("height");
            
            imageRect = new Rect(imageX, imageY, imageX + imageW, imageY + imageH);
            
            landscapeAction = (layoutLandscape != null)
                    ? BaseAction.create(
                            layoutLandscape.getJSONObject("action"),
                            DDNA.instance().getPlatform())
                    : null;
            portraitAction = (layoutPortrait != null)
                    ? BaseAction.create(
                            layoutPortrait.getJSONObject("action"),
                            DDNA.instance().getPlatform())
                    : null;
        }
        
        /**
         * The action for the given orientation.
         *
         * @param orientation The device orientation.
         *
         * @return The action.
         */
        BaseAction action(int orientation) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE){
                return landscapeAction != null ? landscapeAction : portraitAction;
            } else {
                return portraitAction != null ? portraitAction : landscapeAction;
            }
        }
    }
    
    /**
     * Description of the screen outside the popup.
     */
    static class Shim implements Serializable {
        
        /**
         * Fill mask type.
         */
        final String mask;
        /**
         * Touch action.
         */
        final BaseAction action;
        
        Shim(JSONObject json) throws JSONException {
            mask = json.getString("mask");
            action = BaseAction.create(
                    json.getJSONObject("action"),
                    DDNA.instance().getPlatform());
        }
    }
    
    /**
     * Encapsulates an Action.
     */
    static abstract class BaseAction<T> implements Serializable {
        
        protected final String type;
        
        private BaseAction(JSONObject json) throws JSONException {
            type = json.getString("type");
        }
        
        @Nullable
        abstract T getValue();
        
        @Nullable
        static BaseAction create(JSONObject json, @Nullable String platform)
                throws JSONException {
            
            switch (json.getString("type")) {
                case ACTION_NONE:
                    return null;
                    
                case ACTION_DISMISS:
                    return new DismissAction(json);
                
                case ACTION_ACTION:
                    return new Action(json);
                
                case ACTION_LINK:
                    return new LinkAction(json);
                
                case ACTION_STORE:
                    return new StoreAction(json, platform);
                
                default:
                    return new Action(json);
            }
        }
    }
    
    static class DismissAction extends BaseAction<Void> implements Serializable {
        
        DismissAction(JSONObject json) throws JSONException {
            super(json);
        }
        
        @Nullable
        @Override
        Void getValue() {
            return null;
        }
    }
    
    static class Action extends BaseAction<String> implements Serializable {
        
        @Nullable
        protected final String value;
        
        Action(JSONObject json) throws JSONException {
            super(json);
            
            value = json.optString("value");
        }
        
        @Nullable
        @Override
        String getValue() {
            return value;
        }
    }
    
    static class LinkAction extends Action implements Serializable {
        
        LinkAction(JSONObject json) throws JSONException {
            super(json);
        }
    }
    
    static class StoreAction extends BaseAction<String> implements Serializable {
        
        private String value;
        
        StoreAction(JSONObject json, @Nullable String platform)
                throws JSONException {
            
            super(json);
            
            value = json.has("value")
                    ? Objects.equals(platform, ClientInfo.PLATFORM_AMAZON)
                    ? json.optJSONObject("value").optString("AMAZON")
                    : json.optJSONObject("value").optString("ANDROID")
                    : null;
        }
        
        @Nullable
        @Override
        String getValue() {
            return value;
        }
    }
    
    static class Rect implements Serializable {
        
        int left;
        int top;
        int right;
        int bottom;
        
        Rect() {}
        
        Rect(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
        
        boolean contains(int x, int y) {
            return asRect().contains(x, y);
        }
        
        android.graphics.Rect asRect() {
            return new android.graphics.Rect(left, top, right, bottom);
        }
    }
}
