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
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.RelativeLayout;

import com.deltadna.android.sdk.listeners.EngageListener;
import com.deltadna.android.sdk.listeners.ImageMessageResultListener;

import java.util.Iterator;

/**
 * {@link Activity} which displays in Image Message request and handles the
 * user interaction with the Image Message.
 *
 * @see DDNA#requestEngagement(Engagement, EngageListener)
 */
public final class ImageMessageActivity extends Activity {
    
    private enum Action { ACTION, LINK }
    
    private static final String EXTRA_IMG_MSG = "img_msg";
    private static final String EXTRA_VALUE = "value";
    private static final String EXTRA_PARAMS = "params";
    
    private ImageMessage imageMessage;
    
    private Bitmap bitmap;
    
    int screenWidth = 0;
    int screenHeight = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        imageMessage = (ImageMessage) getIntent().getSerializableExtra(
                EXTRA_IMG_MSG);
        if (!imageMessage.prepared()) {
            throw new IllegalStateException(
                    "Image Message must be prepared first");
        }
        
        final RelativeLayout layout = new RelativeLayout(this);
        
        final FrameLayout holder = new FrameLayout(this);
        layout.addView(holder, new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));
        
        final ImageMessageView viewImageMessage = new ImageMessageView(this);
        holder.addView(viewImageMessage, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        
        final RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        
        setContentView(layout);
    }
    
    /**
     * Handles an action defined by the Delta DNA message.
     *
     * @param source    the source of the action
     * @param action    the action to handle
     */
    private void performAction(String source, ImageMessage.Action action){
        if (action != null) {
            final Event event = new Event("imageMessageAction")
                    .putParam("responseDecisionpointName", imageMessage.decisionPoint)
                    .putParam("responseTransactionID", imageMessage.transactionId)
                    .putParam("imActionName", source)
                    .putParam("imActionType", action.type);
            
            if (action.type.equalsIgnoreCase(ImageMessage.ACTION_ACTION)) {
                final Intent intent = new Intent();
                intent.setAction(Action.ACTION.name());
                intent.putExtra(EXTRA_VALUE, action.value);
                if (imageMessage.parameters() != null) {
                    intent.putExtra(
                            EXTRA_PARAMS,
                            imageMessage.parameters().toString());
                }
                
                event.putParam("imActionValue", action.value);
                
                setResult(Activity.RESULT_OK, intent);
            } else if (action.type.equalsIgnoreCase(ImageMessage.ACTION_LINK)) {
                final Intent intent = new Intent();
                intent.setAction(Action.LINK.name());
                intent.putExtra(EXTRA_VALUE, action.value);
                if (imageMessage.parameters() != null) {
                    intent.putExtra(
                            EXTRA_PARAMS,
                            imageMessage.parameters().toString());
                }
                
                event.putParam("imActionValue", action.value);
                
                setResult(Activity.RESULT_OK, intent);
            } else if (action.type.equalsIgnoreCase(ImageMessage.ACTION_DISMISS)) {
                imageMessage.cleanUp();
                
                setResult(Activity.RESULT_CANCELED);
            }
            
            DDNA.instance().recordEvent(event);
            
            finish();
        }
    }
    
    public static Intent createIntent(Context context, ImageMessage msg) {
        return new Intent(context, ImageMessageActivity.class)
                .putExtra(EXTRA_IMG_MSG, msg);
    }
    
    public static void handleResult(
            int resultCode,
            Intent data,
            ImageMessageResultListener callback) {
        
        if (resultCode == RESULT_OK) {
            final Action action = Action.valueOf(data.getAction());
            final Bundle extras = data.getExtras();
            
            switch (action) {
                case ACTION:
                    callback.onAction(
                            extras.getString(EXTRA_VALUE),
                            extras.getString(EXTRA_PARAMS));
                    break;
                
                case LINK:
                    callback.onLink(
                            extras.getString(EXTRA_VALUE),
                            extras.getString(EXTRA_PARAMS));
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            callback.onCancelled();
        }
    }
    
    private class ImageMessageView extends View implements
            View.OnTouchListener {
        
        public ImageMessageView(Context context) {
            super(context);
            setOnTouchListener(this);
        }
        
        public ImageMessageView(Context context, AttributeSet attrs) {
            super(context, attrs);
            setOnTouchListener(this);
        }
        
        @Override
        protected void onLayout(
                boolean changed,
                int left,
                int top,
                int right,
                int bottom) {
            
            screenWidth = right;
            screenHeight = bottom;
            
            if (bitmap == null) {
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                bitmap = BitmapFactory.decodeFile(
                        imageMessage.getImageFile().getPath(),
                        options);
            }
            
            imageMessage.init(
                    getResources().getConfiguration().orientation,
                    screenWidth,
                    screenHeight);
            
            super.onLayout(changed, left, top, right, bottom);
        }
        @Override
        protected void onDraw(Canvas canvas){
            if (bitmap != null) {
                if (ImageMessage.MASK_DIMMED.equalsIgnoreCase(imageMessage.shim.mask)) {
                    canvas.drawARGB(0x66, 0x0, 0x0, 0x0);
                } else {
                    canvas.drawARGB(0x0, 0x0, 0x0, 0x0);
                }
                
                final int orientation = getContext().getResources()
                        .getConfiguration().orientation;
                
                // draw background
                canvas.drawBitmap(
                        bitmap,
                        imageMessage.background.imageRect.asRect(),
                        imageMessage.background.layout(orientation).frame().asRect(),
                        null);
                
                // draw buttons
                final Iterator<ImageMessage.Button> buttons =
                        imageMessage.buttons();
                while (buttons.hasNext()) {
                    final ImageMessage.Button button = buttons.next();
                    canvas.drawBitmap(
                            bitmap,
                            button.imageRect.asRect(),
                            button.layout(orientation).frame().asRect(),
                            null);
                }
            } else {
                canvas.drawARGB(0x0, 0x0, 0x0, 0x0);
            }
        }
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()){
                case MotionEvent.ACTION_UP:{
                    final int orientation = getContext().getResources()
                            .getConfiguration().orientation;
                    
                    String source = null;
                    ImageMessage.Action action = null;
                    
                    // if the touch is on the popup then test buttons
                    if (imageMessage.background.layout(orientation).frame()
                            .contains((int) event.getX(), (int) event.getY())) {
                        final Iterator<ImageMessage.Button> buttons =
                                imageMessage.buttons();
                        
                        while (buttons.hasNext()) {
                            final ImageMessage.Button button = buttons.next();
                            if (button.layout(orientation).frame().contains(
                                    (int) event.getX(),
                                    (int )event.getY())) {
                                source = "button";
                                action = button.action(orientation);
                                break;
                            }
                        }
                        
                        if (action == null) {
                            source = "background";
                            action = imageMessage.background.action(orientation);
                        }
                    } else {
                        // touch is outside the popup so use shim action
                        source = "shim";
                        action = imageMessage.shim.action;
                    }
                    
                    performAction(source, action);
                }
            }
            
            return true;
        }
    }
}
