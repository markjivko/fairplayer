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

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.FileObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * A list adapter that provides a view of the filesystem. The active directory
 * is set through a FpSerializableLimiter and rows are displayed using MediaViews.
 */
public class AdapterFileSystem
        extends BaseAdapter
        implements AdapterLibrary, View.OnClickListener {

    private static final Pattern SPACE_SPLIT = Pattern.compile("\\s+");
    private static final Pattern FILE_SEPARATOR = Pattern.compile(File.separator);

    /**
     * The owner ActivityLibrary.
     */
    final ActivityLibrary mActivity;
    /**
     * A LayoutInflater to use.
     */
    private final LayoutInflater mInflater;
    /**
     * The currently active limiter, set by a row expander being clicked.
     */
    private FpSerializableLimiter mLimiter;
    /**
     * The files and folders in the current directory.
     */
    private File[] mFiles;
    /**
     * The folder icon shown for folder rows.
     */
    private final Drawable mFolderIcon;
    /**
     * The currently active filter, entered by the user from the search box.
     */
    String[] mFilter;
    /**
     * Excludes dot files and files not matching mFilter.
     */
    private final FilenameFilter mFileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            if (filename.charAt(0) == '.') {
                return false;
            }
            if (mFilter != null) {
                filename = filename.toLowerCase();
                for (String term : mFilter) {
                    if (!filename.contains(term)) {
                        return false;
                    }
                }
            }
            return true;
        }
    };
    /**
     * Sorts folders before files first, then sorts alphabetically by name.
     */
    private final Comparator<File> mFileComparator = new Comparator<File>() {
        @Override
        public int compare(File a, File b) {
            boolean aIsFolder = a.isDirectory();
            boolean bIsFolder = b.isDirectory();
            if (bIsFolder == aIsFolder) {
                return a.getName().compareToIgnoreCase(b.getName());
            } else {
                if (bIsFolder) {
                    return 1;
                }
            }
            return -1;
        }
    };
    /**
     * The Observer instance for the current directory.
     */
    private Observer mFileObserver;

    /**
     * Create a AdapterFileSystem.
     *
     * @param activity The ActivityLibrary that will contain this adapter.
     * Called on to requery this adapter when the contents of the directory
     * change.
     * @param limiter An initial limiter to set. If none is given, will be set
     * to the external storage directory.
     */
    public AdapterFileSystem(ActivityLibrary activity, FpSerializableLimiter limiter) {
        mActivity = activity;
        mLimiter = limiter;
        mFolderIcon = Theme.Resources.getDrawable(R.drawable.fp_library_folder);
        mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (limiter == null) {
            limiter = buildLimiter(FpUtilsFilesystem.getFilesystemBrowseStart(activity));
        }
        setLimiter(limiter);
    }

    @Override
    public Object query() {
        File file = mLimiter == null ? new File("/") : (File) mLimiter.data;

        if (mFileObserver == null) {
            mFileObserver = new Observer(file.getPath());
        }

        File[] files = file.listFiles(mFileFilter);
        if (files != null) {
            Arrays.sort(files, mFileComparator);
        }
        return files;
    }

    @Override
    public void commitQuery(Object data) {
        mFiles = (File[]) data;
        notifyDataSetChanged();
    }

    @Override
    public void clear() {
        mFiles = null;
        notifyDataSetInvalidated();
    }

    @Override
    public int getCount() {
        if (mFiles == null) {
            return 0;
        }
        return mFiles.length;
    }

    @Override
    public Object getItem(int pos) {
        return mFiles[pos];
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View view;
        FpElementRowHolder holder;

        if (convertView == null) {
            view = mInflater.inflate(R.layout.fp_row_expandable, null);
            holder = new FpElementRowHolder();
            holder.text = (TextView) view.findViewById(R.id.fp_text);
            
            // Set the text color
            holder.text.setTextColor(Theme.Resources.getColor(R.color.fp_color_row_title));
            
            holder.cover = (ElementSmallCover) view.findViewById(R.id.fp_cover);
            holder.arrow = (ImageView) view.findViewById(R.id.fp_more);
            holder.arrow.setImageDrawable(Theme.Resources.getDrawable(R.drawable.fp_row_more));

            holder.cover.setImageDrawable(mFolderIcon);
            holder.arrow.setOnClickListener(this);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (FpElementRowHolder) view.getTag();
        }

        File file = mFiles[pos];
        boolean isDirectory = file.isDirectory();
        holder.id = pos;
        holder.text.setText(file.getName());
        holder.arrow.setVisibility(isDirectory ? View.VISIBLE : View.GONE);
        holder.cover.setVisibility(isDirectory ? View.VISIBLE : View.GONE);
        return view;
    }

    @Override
    public void setFilter(String filter) {
        if (filter == null) {
            mFilter = null;
        } else {
            mFilter = SPACE_SPLIT.split(filter.toLowerCase());
        }
    }

    @Override
    public void setLimiter(FpSerializableLimiter limiter) {
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
        }
        mFileObserver = null;
        mLimiter = limiter;
    }

    @Override
    public FpSerializableLimiter getLimiter() {
        return mLimiter;
    }

    /**
     * Builds a limiter from the given folder. Only files contained in the
     * given folder will be shown if the limiter is set on this adapter.
     *
     * @param file A File pointing to a folder.
     *
     * @return A limiter describing the given folder.
     */
    public static FpSerializableLimiter buildLimiter(File file) {
        String[] fields = FILE_SEPARATOR.split(file.getPath().substring(1));
        return new FpSerializableLimiter(FpUtilsMedia.TYPE_FILE, fields, file);
    }

    @Override
    public FpSerializableLimiter buildLimiter(long id) {
        return buildLimiter(mFiles[(int) id]);
    }

    @Override
    public int getMediaType() {
        return FpUtilsMedia.TYPE_FILE;
    }

    /**
     * FileObserver that reloads the files in this adapter.
     */
    private class Observer extends FileObserver {

        public Observer(String path) {
            super(path, FileObserver.CREATE | FileObserver.DELETE | FileObserver.MOVED_TO | FileObserver.MOVED_FROM);
            startWatching();
        }

        @Override
        public void onEvent(int event, String path) {
            mActivity.mPagerAdapter.postRequestRequery(AdapterFileSystem.this);
        }
    }

    @Override
    public Intent createData(View view) {
        FpElementRowHolder holder = (FpElementRowHolder) view.getTag();
        File file = mFiles[(int) holder.id];

        Intent intent = new Intent();
        intent.putExtra(AdapterLibrary.DATA_TYPE, FpUtilsMedia.TYPE_FILE);
        intent.putExtra(AdapterLibrary.DATA_ID, holder.id);
        intent.putExtra(AdapterLibrary.DATA_TITLE, holder.text.getText().toString());
        intent.putExtra(AdapterLibrary.DATA_EXPANDABLE, file.isDirectory());

        String path;
        try {
            path = file.getCanonicalPath();
        } catch (IOException e) {
            path = file.getAbsolutePath();
        }
        
        intent.putExtra(AdapterLibrary.DATA_FILE, path);
        return intent;
    }

    @Override
    public void onClick(View view) {
        onHandleRowClick((View) view.getParent());
    }

    public void onHandleRowClick(View view) {
        Intent intent = createData(view);
        boolean isFolder = intent.getBooleanExtra(AdapterLibrary.DATA_EXPANDABLE, false);

        if (FpUtilsFilesystem.canDispatchIntent(intent) && FpUtilsFilesystem.dispatchIntent(mActivity, intent)) {
            return;
        }

        if (isFolder) {
            mActivity.onItemExpanded(intent);
        } else {
            mActivity.onItemClicked(intent);
        }
    }
}