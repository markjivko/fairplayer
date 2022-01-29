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

import android.app.Activity;
import android.app.Fragment;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

public class FragmentScanner extends Fragment {
	
    public static class ScannerString {

        private static interface SubGenerator {
            public String toString();
        }

        private static class ResourceSubGenerator implements SubGenerator {

            int mResId;

            public ResourceSubGenerator(int resId) {
                mResId = resId;
            }

            public String toString() {
                return ActivityNowplaying.getActivity().getString(mResId);
            }
        }

        private static class StringSubGenerator implements SubGenerator {

            String mString;

            public StringSubGenerator(String string) {
                mString = string;
            }

            public String toString() {
                return mString;
            }
        }

        ArrayList<SubGenerator> mSubGenerators = new ArrayList<SubGenerator>();

        public void addSubGenerator(int resId) {
            mSubGenerators.add(new ResourceSubGenerator(resId));
        }

        public void addSubGenerator(String string) {
            mSubGenerators.add(new StringSubGenerator(string));
        }

        public ScannerString(int resId) {
            addSubGenerator(resId);
        }

        public ScannerString(int resId, String string) {
            addSubGenerator(resId);
            addSubGenerator(string);
        }

        public ScannerString(String string) {
            addSubGenerator(string);
        }

        public ScannerString() {}

        public String toString() {
            StringBuilder toReturn = new StringBuilder();
            Iterator<SubGenerator> iterator = mSubGenerators.iterator();
            while (iterator.hasNext()) {
                toReturn.append(iterator.next().toString());
            }
            return toReturn.toString();
        }
    }

    protected static final String[] MEDIA_PROJECTION = {
    	MediaStore.MediaColumns.DATA,
    	MediaStore.MediaColumns.DATE_MODIFIED,
        MediaStore.MediaColumns.SIZE
    };

    protected static final int DB_RETRIES = 3;

    ArrayList<String> mPathNames;
    TreeSet<File> mFilesToProcess;
    int mLastGoodProcessedIndex;

    protected Handler mHandler = new Handler();

    int mProgressNum;
    ScannerString mProgressText = new ScannerString(R.string.settings_scanner_progress_label_init);
    ScannerString mDebugMessages = new ScannerString();
    boolean mStartButtonEnabled;
    boolean mHasStarted = false;

    /**
     * Callback interface used by the fragment to update the Activity.
     */
    public static interface ScanProgressCallbacks {

        void updateProgressNum(int progressNum);

        void updateProgressText(ScannerString progressText);

        void updatePath(String path);

        void updateStartButtonEnabled(boolean startButtonEnabled);

        void signalFinished();
    }

    private ScanProgressCallbacks mCallbacks;

    private void updateProgressNum(int progressNum) {
        mProgressNum = progressNum;
        if (mCallbacks != null) {
            mCallbacks.updateProgressNum(mProgressNum);
        }
    }

    private void updateProgressText(int resId) {
        updateProgressText(new ScannerString(resId));
    }

    private void updateProgressText(int resId, String string) {
        updateProgressText(new ScannerString(resId, string));
    }

    private void updateProgressText(ScannerString progressText) {
        mProgressText = progressText;
        if (mCallbacks != null) {
            mCallbacks.updateProgressText(mProgressText);
        }
    }

    private void updateStartButtonEnabled(boolean startButtonEnabled) {
        mStartButtonEnabled = startButtonEnabled;
        if (mCallbacks != null) {
            mCallbacks.updateStartButtonEnabled(mStartButtonEnabled);
        }
    }

    private void signalFinished() {
        if (mCallbacks != null) {
            mCallbacks.signalFinished();
        }
    }

    public int getProgressNum() {
        return mProgressNum;
    }

    public ScannerString getProgressText() {
        return mProgressText;
    }

    public ScannerString getDebugMessages() {
        return mDebugMessages;
    }

    public boolean getStartButtonEnabled() {
        return mStartButtonEnabled;
    }

    public boolean getHasStarted() {
        return mHasStarted;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ScanProgressCallbacks) {
            mCallbacks = (ScanProgressCallbacks) activity;
        }
    }

    public void setScanProgressCallbacks(ScanProgressCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    public FragmentScanner() {
        super();

        // Set correct initial values.
        mProgressNum = 0;
        mStartButtonEnabled = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void scannerEnded() {
        updateProgressNum(0);
        updateProgressText(R.string.settings_scanner_progress_label_done);
        updateStartButtonEnabled(true);
        signalFinished();
    }

    public void startMediaScanner() {
        if (mPathNames.size() == 0) {
            scannerEnded();
        } else {
            MediaScannerConnection.scanFile(
                ActivityCommon.getContext(),
                mPathNames.toArray(new String[mPathNames.size()]),
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        mHandler.post(new Updater(path));
                    }
                }
            );
        }
    }

    public void startScan(File path, boolean restrictDbUpdate) {
        mHasStarted = true;
        updateStartButtonEnabled(false);
        updateProgressText(R.string.settings_scanner_progress_label_list_init);
        mFilesToProcess = new TreeSet<File>();
        if (path.exists()) {
            this.new PreprocessTask().execute(new ScanParameters(path, restrictDbUpdate));
        } else {
            updateProgressText(R.string.settings_scanner_progress_error_bad_path);
            updateStartButtonEnabled(true);
            signalFinished();
        }
    }

    static class ProgressUpdate {

        public enum Type {
            DATABASE, STATE
        }

        Type mType;

        public Type getType() {
            return mType;
        }

        int mResId;

        public int getResId() {
            return mResId;
        }

        String mString;

        public String getString() {
            return mString;
        }

        int mProgress;

        public int getProgress() {
            return mProgress;
        }

        public ProgressUpdate(Type type, int resId, String string, int progress) {
            mType = type;
            mResId = resId;
            mString = string;
            mProgress = progress;
        }
    }

    static ProgressUpdate databaseUpdate(String file, int progress) {
        return new ProgressUpdate(ProgressUpdate.Type.DATABASE, 0, file, progress);
    }

    static ProgressUpdate stateUpdate(int resId) {
        return new ProgressUpdate(ProgressUpdate.Type.STATE, resId, "", 0);
    }

    static class ScanParameters {

        File mPath;
        boolean mRestrictDbUpdate;

        public ScanParameters(File path, boolean restrictDbUpdate) {
            mPath = path;
            mRestrictDbUpdate = restrictDbUpdate;
        }

        public File getPath() {
            return mPath;
        }

        public boolean shouldScan(File file, boolean fromDb) throws IOException {
            // Empty directory check.
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files == null || files.length == 0) {
                    return false;
                }
            }
            if (!mRestrictDbUpdate && fromDb) {
                return true;
            }
            while (file != null) {
                if (file.equals(mPath)) {
                    return true;
                }
                file = file.getParentFile();
            }
            return false;
        }
    }

    class PreprocessTask extends AsyncTask<ScanParameters, ProgressUpdate, Void> {

        private void recursiveAddFiles(File file, ScanParameters scanParameters)
                throws IOException {
            if (!scanParameters.shouldScan(file, false)) {
                // If we got here, there file was either outside the scan
                // directory, or was an empty directory.
                return;
            }
            if (!mFilesToProcess.add(file)) {
                // Avoid infinite recursion caused by symlinks.
                // If mFilesToProcess already contains this file, add() will 
                // return false.
                return;
            }
            if (file.isDirectory()) {
                boolean nomedia = new File(file, ".nomedia").exists();
                // Only recurse downward if not blocked by nomedia.
                if (!nomedia) {
                    File[] files = file.listFiles();
                    if (files != null) {
                        for (File nextFile : files) {
                            recursiveAddFiles(nextFile.getCanonicalFile(), scanParameters);
                        }
                    } 
                }
            }
        }

        protected void dbOneTry(ScanParameters parameters) {
            Cursor cursor = ActivityCommon.getContext().getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                MEDIA_PROJECTION,
                null,
                null,
                null
            );
            int data_column = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
            int modified_column = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED);
            int size_column = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
            int totalSize = cursor.getCount();
            int currentItem = 0;
            int reportFreq = 0;
            // Used to calibrate reporting frequency
            long startTime = SystemClock.currentThreadTimeMillis();
            while (cursor.moveToNext()) {
                currentItem++;
                try {
                    File file = new File(cursor.getString(data_column)).getCanonicalFile();
                    // Ignore non-file backed playlists (size == 0). Fixes playlist removal on scan
                    // for 4.1
                    boolean validSize = cursor.getInt(size_column) > 0;
                    if (validSize && (!file.exists()
                            || file.lastModified() / 1000L
                            > cursor.getLong(modified_column))
                            && parameters.shouldScan(file, true)) {
                        // Media scanner handles these cases.
                        // Is a set, so OK if already present.
                        mFilesToProcess.add(file);
                    } else {
                        // Don't want to waste time scanning an up-to-date
                        // file.
                        mFilesToProcess.remove(file);
                    }
                    if (reportFreq == 0) {
                        // Calibration phase
                        if (SystemClock.currentThreadTimeMillis() - startTime > 25) {
                            reportFreq = currentItem + 1;
                        }
                    } else {
                        if (currentItem % reportFreq == 0) {
                            publishProgress(databaseUpdate(file.getPath(), (100 * currentItem) / totalSize));
                        }
                    }
                } catch (IOException ex) {
                    // Just ignore it for now.
                }
            }
            // Don't need the cursor any more.
            cursor.close();
        }

        @Override
        protected Void doInBackground(ScanParameters... parameters) {
            try {
                recursiveAddFiles(parameters[0].getPath(), parameters[0]);
            } catch (IOException Ex) {
                // Do nothing.
            }
            // Parse database
            publishProgress(stateUpdate(R.string.settings_scanner_progress_label_database_init));
            boolean dbSuccess = false;
            int numRetries = 0;
            while (!dbSuccess && numRetries < DB_RETRIES) {
                dbSuccess = true;
                try {
                    dbOneTry(parameters[0]);
                } catch (Exception Ex) {
                    // For any of these errors, try again.
                    numRetries++;
                    dbSuccess = false;
                    if (numRetries < DB_RETRIES) {
                        publishProgress(stateUpdate(R.string.settings_scanner_progress_error_retry));
                        SystemClock.sleep(1000);
                    }
                }
            }
            
            // Prepare final path list for processing.
            mPathNames = new ArrayList<String>(mFilesToProcess.size());
            Iterator<File> iterator = mFilesToProcess.iterator();
            while (iterator.hasNext()) {
                mPathNames.add(iterator.next().getPath());
            }
            mLastGoodProcessedIndex = -1;

            return null;
        }

        @Override
        protected void onProgressUpdate(ProgressUpdate... progress) {
            switch (progress[0].getType()) {
                case DATABASE:
                    updateProgressText(R.string.settings_scanner_progress_label_analyzed, " " + progress[0].getString());
                    updateProgressNum(progress[0].getProgress());
                    break;
                    
                case STATE:
                    updateProgressText(progress[0].getResId());
                    updateProgressNum(0);
                    break;
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            startMediaScanner();
        }
    }

    class Updater implements Runnable {

        String mPathScanned;

        public Updater(String path) {
            mPathScanned = path;
        }

        public void run() {
            if (mLastGoodProcessedIndex + 1 < mPathNames.size() && mPathNames.get(mLastGoodProcessedIndex + 1).equals(mPathScanned)) {
                mLastGoodProcessedIndex++;
            } else {
                int newIndex = mPathNames.indexOf(mPathScanned);
                if (newIndex > -1) {
                    mLastGoodProcessedIndex = newIndex;
                }
            }
            int progress = (100 * (mLastGoodProcessedIndex + 1)) / mPathNames.size();
            if (progress == 100) {
                scannerEnded();
            } else {
                updateProgressNum(progress);
                updateProgressText(R.string.settings_scanner_progress_label_analyzed, " " + mPathScanned);
            }
        }
    }
}

/*EOF*/