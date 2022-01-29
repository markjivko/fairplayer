/**
 * Copyright 2016 Mark Jivko https://markjivko.com
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://gnu.org/licenses/gpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Redistributions of files must retain the above copyright notice.
 */
package com.fairplayer;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.util.LruCache;
import android.widget.ImageView;

/**
 * ElementSmallCover implements a 'song-aware' ImageView
 *
 * View updates should be triggered via setCover(type, id) to
 * instruct the view to load the cover from its own LRU cache.
 *
 * The cover will automatically be fetched & scaled in a background
 * thread on cache miss
 */
public class ElementSmallCover extends ImageView implements Handler.Callback {

    /**
     * Context of constructor
     */
    private Context mContext;
    
    /**
     * UI Thread handler
     */
    private static Handler sUiHandler;
    
    /**
     * Worker thread handler
     */
    private static Handler sHandler;
    
    /**
     * Our private LRU cache
     */
    private static BitmapLruCache sBitmapLruCache = null;
    
    /**
     * The cover key we are expected to draw
     */
    private FpCoverStore.CoverKey mExpectedKey;

    /**
     * Cover message we are passing around using mHandler
     */
    private static class CoverMsg {

        public FpCoverStore.CoverKey key; // A cache key identifying this RPC
        public ElementSmallCover view;      // The view we are updating

        CoverMsg(FpCoverStore.CoverKey key, ElementSmallCover view) {
            this.key = key;
            this.view = view;
        }

        /**
         * Returns true if the view still requires updating
         */
        public boolean isRecent() {
            return this.key.equals(this.view.mExpectedKey);
        }
    }

    /**
     * Clear the cache
     */
    public static void clearCache() {
        // Cache created
        if (null != sBitmapLruCache) {
            // Clean-up
            sBitmapLruCache.evictAll();
        }
    }
    
    /**
     * Constructor of class inflated from XML
     *
     * @param context The context of the calling activity
     * @param attributes attributes passed by the xml inflater
     */
    public ElementSmallCover(Context context, AttributeSet attributes) {
        super(context, attributes);
        mContext = context;
        if (sBitmapLruCache == null) {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            int lruSize = am.getMemoryClass() / 10; // use ~10% for LRU
            lruSize = lruSize < 2 ? 2 : lruSize; // LRU will always be at least 2MiB
            sBitmapLruCache = new BitmapLruCache(lruSize * 1024 * 1024);
        }
        if (sUiHandler == null) {
            sUiHandler = new Handler(this);
        }
        if (sHandler == null) {
            HandlerThread thread = new HandlerThread(ElementSmallCover.class.getSimpleName());
            thread.start();
            sHandler = new Handler(thread.getLooper(), this);
        }
    }

    /**
     * mHandler and mUiHandler callbacks
     */
    private static final int MSG_CREATE_COVER = 61;
    private static final int MSG_DRAW_COVER = 62;

    @Override
    public boolean handleMessage(Message message) {
        CoverMsg payload = (CoverMsg) message.obj;

        if (!payload.isRecent()) {
            return false;
        }

        switch (message.what) {
            case MSG_CREATE_COVER: {
                // This message was sent due to a cache miss, but the cover might got cached in the meantime
                Bitmap bitmap = sBitmapLruCache.get(payload.key);
                
                // No bitmap found
                if (bitmap == null) {
                    do {
                        // Album
                        if (payload.key.mediaType == FpUtilsMedia.TYPE_ALBUM) {
                            // Valid song
                            FpTrack song = FpUtilsMedia.getSongByTypeId(mContext.getContentResolver(), payload.key.mediaType, payload.key.mediaId);
                            if (null != song) {
                                // Get the song cover
                                bitmap = song.getSmallCover(mContext);
                                
                                // Valid bitmap
                                if (null != bitmap) {
                                    break;
                                }
                            }
                        }
                    
                        // Item has no cover: return a failback
                        bitmap = FpCoverBitmap.generateDefaultCover(mContext, FpCoverStore.SIZE_SMALL, FpCoverStore.SIZE_SMALL);
                    } while (false);
                }
                
                // bitmap is non null: store in LRU cache and draw it
                sBitmapLruCache.put(payload.key, bitmap);
                sUiHandler.sendMessage(sUiHandler.obtainMessage(MSG_DRAW_COVER, payload));
                break;
            }
            case MSG_DRAW_COVER: {
                // draw the cover into view. must be called from ui thread handler
                payload.view.drawFromCache(payload.key, true);
                break;
            }
            default:
                return false;
        }
        return true;
    }

    /**
     * Attempts to set the image of this cover
     * Must be called from an UI thread
     *
     * @param type The Media type
     * @param id The id of this media type to query
     */
    public void setCover(int type, long id) {
        mExpectedKey = new FpCoverStore.CoverKey(type, id, FpCoverStore.SIZE_SMALL);
        if (drawFromCache(mExpectedKey, false) == false) {
            CoverMsg payload = new CoverMsg(mExpectedKey, this);
            sHandler.sendMessage(sHandler.obtainMessage(MSG_CREATE_COVER, payload));
        }
    }

    /**
     * Updates the view with a cached bitmap
     * A fallback image will be used on cache miss
     *
     * @param key The cover message containing the cache key and view to use
     */
    public boolean drawFromCache(FpCoverStore.CoverKey key, boolean fadeIn) {
        boolean cacheHit = true;
        Bitmap bitmap = sBitmapLruCache.get(key);
        if (bitmap == null) {
            cacheHit = false;
        }

        if (fadeIn) {
            TransitionDrawable td = new TransitionDrawable(new Drawable[] {
                getDrawable(),
                (new BitmapDrawable(getResources(), bitmap))
            });
            setImageDrawable(td);
            td.startTransition(120);
        } else {
            setImageBitmap(bitmap);
        }

        return cacheHit;
    }

    /**
     * A LRU cache implementation, using the CoverKey as key to store Bitmap
     * objects
     *
     * Note that the implementation does not override create() in order to
     * enable
     * the use of fetch-if-cached functions: createBitmap() is therefore called
     * by FpCoverStore itself.
     */
    private static class BitmapLruCache extends LruCache<FpCoverStore.CoverKey, Bitmap> {

        /**
         * Creates a new in-memory LRU cache
         *
         * @param size the lru cache size in bytes
         */
        public BitmapLruCache(int size) {
            super(size);
        }

        /**
         * Returns the cache size in bytes, not objects
         */
        @Override
        protected int sizeOf(FpCoverStore.CoverKey key, Bitmap value) {
            return value.getByteCount();
        }
    }
}

/*EOF*/