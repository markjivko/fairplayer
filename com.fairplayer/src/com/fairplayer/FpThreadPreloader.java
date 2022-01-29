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

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class FpThreadPreloader implements Handler.Callback {

    /**
     * How many bytes we are going to read per run
     */
    private static final int BYTES_PER_READ = 32768;
    /**
     * How many milliseconds to wait between reads. 125*32768 = ~256kb/s. This
     * should be fast enough for flac files
     */
    private static final int MS_DELAY_PER_READ = 125;
    /**
     * Our message handler
     */
    private Handler mHandler;
    /**
     * The global (current) file input stream
     */
    private FileInputStream mFis;
    /**
     * The filesystem path used to create the current mFis
     */
    private String mPath;
    /**
     * Scratch space to read junk data
     */
    private byte[] mScratch;

    public FpThreadPreloader() {
        mScratch = new byte[BYTES_PER_READ];
        HandlerThread handlerThread = new HandlerThread(FpThreadPreloader.class.getSimpleName(), Process.THREAD_PRIORITY_LOWEST);
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper(), this);
    }

    /**
     * Aborts all current in-flight RPCs, pausing the readahead operation
     */
    public void pause() {
        mHandler.removeMessages(MSG_SET_PATH);
        mHandler.removeMessages(MSG_READ_CHUNK);
    }

    /**
     * Starts a new readahead operation. Will resume if `path' equals
     * the currently open file
     *
     * @param path The path to read ahead
     */
    public void setSource(String path) {
        pause(); // cancell all in-flight rpc's
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SET_PATH, path), 1000);
    }

    private static final int MSG_SET_PATH = 1;
    private static final int MSG_READ_CHUNK = 2;

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_SET_PATH: {
                String path = (String) message.obj;

                if (mFis != null && mPath.equals(path) == false) {
                    // current file does not match requested one: clean it
                    try {
                        mFis.close();
                    } catch (IOException e) {
                        Log.e(Constants.LOG_TAG, "Failed to close file: " + e);
                    }
                    mFis = null;
                    mPath = null;
                }

                if (mFis == null) {
                    // need to open new input stream
                    try {
                        FileInputStream fis = new FileInputStream(path);
                        mFis = fis;
                        mPath = path;
                    } catch (FileNotFoundException e) {
                        Log.e(Constants.LOG_TAG, "Failed to open file " + path + ": " + e);
                    }
                }

                if (mFis != null) {
                    mHandler.sendEmptyMessage(MSG_READ_CHUNK);
                }
                break;
            }
            case MSG_READ_CHUNK: {
                int bytesRead = -1;
                try {
                    bytesRead = mFis.read(mScratch);
                } catch (IOException e) {
                    // fs error or eof: stop in any case
                }
                if (bytesRead >= 0) {
                    mHandler.sendEmptyMessageDelayed(MSG_READ_CHUNK, MS_DELAY_PER_READ);
                } else {
                    Log.d(Constants.LOG_TAG, "Readahead for " + mPath + " finished");
                }
            }
            default: {
                break;
            }
        }
        return true;
    }

}