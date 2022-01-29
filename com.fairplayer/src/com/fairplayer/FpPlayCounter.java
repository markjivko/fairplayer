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
import android.content.ContentResolver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.util.Log;
import java.util.ArrayList;

public class FpPlayCounter extends SQLiteOpenHelper {

    // Table
    private static final String TABLE_PLAYCOUNTS = "fp_database_listen_counter";
    
    // Table columns
    protected static final String COLUMN_TYPE = "type";
    protected static final String COLUMN_TYPE_ID = "type_id";
    protected static final String COLUMN_LISTEN_COUNT = "listen_count";
    protected static final String COLUMN_INDEX_UNIQUE = "idx_unique";
    protected static final String COLUMN_INDEX_TYPE = "idx_unique";
    
    // Context
    private Context ctx;

    public FpPlayCounter(Context context) {
        super(context, TABLE_PLAYCOUNTS + ".db", null, 1);
        ctx = context;
    }

    @Override
    public void onCreate(SQLiteDatabase dbh) {
        // Crate the table
        try {
            dbh.execSQL("CREATE TABLE " 
                + TABLE_PLAYCOUNTS + " ("
                    + COLUMN_TYPE + " INTEGER, "
                    + COLUMN_TYPE_ID + " BIGINT, "
                    + COLUMN_LISTEN_COUNT + " INTEGER" 
                + ");"
            );
        } catch (Exception excCreate) {}
        
        // Create the indexes
        try {
            dbh.execSQL("CREATE UNIQUE INDEX " + COLUMN_INDEX_UNIQUE 
                + " ON " + TABLE_PLAYCOUNTS
                + " (" + COLUMN_TYPE + ", " + COLUMN_TYPE_ID + ");"
            );
        } catch (Exception excIdx){}
        try {
            dbh.execSQL("CREATE INDEX " + COLUMN_INDEX_TYPE 
                + " ON " + TABLE_PLAYCOUNTS
                + " (" + COLUMN_TYPE + ");"
            );
        } catch (Exception excIdx){}
    }

    @Override
    public void onUpgrade(SQLiteDatabase dbh, int oldVersion, int newVersion) {
    }

    /**
     * Counts this song object as 'played'
     */
    public void countSong(FpTrack song) {
        // Invalid song
        if (null == song) {
            return;
        }
        
        // Get the ID
        long id = FpTrack.getId(song);
        
        // Get the database connection
        SQLiteDatabase dbh = getWritableDatabase();
        
        // Create the row
        dbh.execSQL("INSERT OR IGNORE INTO " 
            + TABLE_PLAYCOUNTS 
            + " (" + COLUMN_TYPE + ", " + COLUMN_TYPE_ID + ", " + COLUMN_LISTEN_COUNT + ")" 
            + " VALUES"
            +" (" + FpUtilsMedia.TYPE_SONG + ", " + id + ", 0);"
        );
        
        // Update the play count
        dbh.execSQL("UPDATE " + TABLE_PLAYCOUNTS 
            + " SET " + COLUMN_LISTEN_COUNT + "=" + COLUMN_LISTEN_COUNT + "+1"
            + " WHERE " + COLUMN_TYPE + "=" + FpUtilsMedia.TYPE_SONG + " AND " + COLUMN_TYPE_ID + "=" + id + ";"
        );
        
        // Close the connection
        dbh.close();

        // Garbage collection
        performGC(FpUtilsMedia.TYPE_SONG);
    }

    /**
     * Returns a sorted array list of most often listen song ids
     */
    public ArrayList<Long> getTopSongs(int limit) {
        ArrayList<Long> payload = new ArrayList<Long>();
        SQLiteDatabase dbh = getReadableDatabase();

        // Get the results
        Cursor cursor = dbh.rawQuery(
            "SELECT " + COLUMN_TYPE_ID 
            + " FROM " + TABLE_PLAYCOUNTS 
            + " WHERE " + COLUMN_TYPE + "=" + FpUtilsMedia.TYPE_SONG 
                + " AND " + COLUMN_LISTEN_COUNT + " != 0" 
            + " ORDER BY " + COLUMN_LISTEN_COUNT + " DESC limit " + limit, 
            null
        );

        while (cursor.moveToNext()) {
            payload.add(cursor.getLong(0));
        }

        cursor.close();
        dbh.close();
        return payload;
    }

    /**
     * Picks a random amount of 'type' items from the provided DBH
     * and checks them against Androids media database.
     * Items not found in the media library are removed from the DBH's database
     */
    private int performGC(int type) {
        SQLiteDatabase dbh = getWritableDatabase();
        ArrayList<Long> toCheck = new ArrayList<Long>(); // List of songs we are going to check
        FpUtilsMedia.QueryTask query;                      // Reused query object
        Cursor cursor;                                   // recycled cursor
        int removed = 0;                                 // Amount of removed items

        // We are just grabbing a bunch of random IDs
        cursor = dbh.rawQuery(
            "SELECT " + COLUMN_TYPE_ID 
            + " FROM " + TABLE_PLAYCOUNTS 
            + " WHERE " + COLUMN_TYPE + "=" + type 
            + " ORDER BY RANDOM() LIMIT 10", 
            null
        );
        while (cursor.moveToNext()) {
            toCheck.add(cursor.getLong(0));
        }
        cursor.close();

        for (Long id : toCheck) {
            query = FpUtilsMedia.buildQuery(type, id, null, null);
            cursor = query.runQuery(ctx.getContentResolver());
            if (cursor.getCount() == 0) {
                dbh.execSQL(
                    "DELETE FROM " + TABLE_PLAYCOUNTS 
                    + " WHERE " + COLUMN_TYPE + "=" + type 
                        + " AND " + COLUMN_TYPE_ID + "=" + id
                );
                removed++;
            }
            cursor.close();
        }
        dbh.close();
        return removed;
    }
}

/*EOF*/