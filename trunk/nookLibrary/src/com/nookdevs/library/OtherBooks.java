/* 
 * Copyright 2010 nookDevs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.nookdevs.library;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.bravo.ecmscannerservice.ScannedFile;
import com.nookdevs.common.nookBaseActivity;

public class OtherBooks extends SQLiteOpenHelper {
    public static final String BOOKS_DB = "myBooks.db";
    public static final String CREATE_BOOKS_TABLE =
        " create table books ( id integer primary key autoincrement, ean text, titles text not null, "
            + "authors text not null, desc text, keywords text, publisher text, cover text, published long, created long, path text not null unique, series text)";
    public static final String CREATE_LOG_TABLE = "create table log ( last_update long )";
    private NookLibrary nookLib;
    public static final int VERSION = 2;
    private List<ScannedFile> m_Files = new ArrayList<ScannedFile>(100);
    private SQLiteDatabase m_Db = null;
    List<String> m_DeleteBooks = new ArrayList<String>(10);
    List<File> m_UpdatedFiles = new ArrayList<File>(100);
    
    public OtherBooks(NookLibrary context) {
        super(context, BOOKS_DB, null, VERSION);
        nookLib = context;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_BOOKS_TABLE);
            db.execSQL(CREATE_LOG_TABLE);
            db.beginTransaction();
            db.execSQL("insert into log values(0)");
            db.setTransactionSuccessful();
            db.endTransaction();
        } catch (Exception ex) {
            db.endTransaction();
            Log.e("OtherBooks", "exception while creating tables", ex);
        }
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS BOOKS");
        db.execSQL("DROP TABLE IF EXISTS LOG");
        onCreate(db);
    }
    
    private void updateLastScan() {
        try {
            if (m_Db == null) {
                m_Db = getWritableDatabase();
            }
            ContentValues values = new ContentValues();
            values.put("last_update", System.currentTimeMillis());
            m_Db.beginTransaction();
            m_Db.update("LOG", values, null, null);
            m_Db.setTransactionSuccessful();
            m_Db.endTransaction();
            return;
        } catch (Exception ex) {
            Log.e("OtherBooks", "Exception updating last scan time", ex);
        }
    }
    
    private Date getLastScanDate() {
        try {
            if (m_Db == null) {
                m_Db = getWritableDatabase();
            }
            String[] columns = {
                "last_update"
            };
            Cursor cursor = m_Db.query("LOG", columns, null, null, null, null, null);
            Date date;
            if (cursor.getCount() == 1) {
                cursor.moveToFirst();
                date = new Date(cursor.getLong(0));
            } else {
                date = new Date(0);
            }
            cursor.close();
            return date;
        } catch (Exception ex) {
            return new Date(0);
        }
    }
    
    private void deleteBooks() {
        try {
            String[] values = m_DeleteBooks.toArray(new String[1]);
            String whereclause = "";
            for (int i = 0; i < values.length; i++) {
                if (i == 0) {
                    whereclause += "id=?";
                } else {
                    whereclause += " or id=?";
                }
            }
            m_Db.beginTransaction();
            int rows = m_Db.delete("BOOKS", whereclause, values);
            Log.i("OtherBooks", "Rows deleted =" + rows);
            m_Db.setTransactionSuccessful();
            m_Db.endTransaction();
        } catch (Exception ex) {
            Log.e("OtherBooks", "Exception deleting books", ex);
            m_Db.endTransaction();
        }
    }
    
    public void addBookToDB(ScannedFile file) {
        try {
            ContentValues values = new ContentValues();
            values.put("ean", file.getEan());
            values.put("series", file.getSeries());
            values.put("publisher", file.getPublisher());
            values.put("path", file.getPathName());
            values.put("cover", file.getCover());
            if (file.getPublishedDate() != null) {
                values.put("published", file.getPublishedDate().getTime());
            }
            if (file.getCreatedDate() != null) {
                values.put("created", file.getCreatedDate().getTime());
            }
            values.put("desc", file.getDescription());
            List<String> titles = file.getTitles();
            String title = "";
            for (int i = 0; i < titles.size(); i++) {
                if (i == 0) {
                    title += titles.get(i);
                } else {
                    title += "," + titles.get(i);
                }
            }
            values.put("titles", title);
            values.put("authors", file.getAuthor());
            String keyword = "";
            List<String> keywords = file.getKeywords();
            for (int i = 0; i < keywords.size(); i++) {
                if (i != 0) {
                    keyword += ",";
                }
                keyword += keywords.get(i);
            }
            values.put("keywords", keyword);
            m_Db.beginTransaction();
            m_Db.insert("BOOKS", null, values);
            m_Db.setTransactionSuccessful();
            m_Db.endTransaction();
        } catch (Exception ex) {
            Log.e("OtherBooks", "Error adding books to DB");
            m_Db.endTransaction();
        }
    }
    
    private List<ScannedFile> getBooksFromDB() {
        try {
            if (m_Db == null) {
                m_Db = getWritableDatabase();
            }
            m_DeleteBooks.clear();
            Date lastScan = getLastScanDate();
            String query =
                "select id, ean,titles,authors,desc,keywords,publisher,cover,published,created,path,series from books";
            Cursor cursor = m_Db.rawQuery(query, null);
            int size = cursor.getCount();
            cursor.moveToFirst();
            for (int i = 0; i < size; i++) {
                String path = cursor.getString(10);
                File file = new File(path);
                if (!file.exists()) {
                    m_DeleteBooks.add(cursor.getString(0));
                    cursor.moveToNext();
                    continue;
                }
                Date lastModified = new Date(file.lastModified());
                if (lastModified.after(lastScan)) {
                    m_DeleteBooks.add(cursor.getString(0));
                    cursor.moveToNext();
                    continue;
                } else {
                    m_UpdatedFiles.add(file);
                }
                ScannedFile sf = new ScannedFile(path, false);
                sf.setEan(cursor.getString(1));
                sf.setPublisher(cursor.getString(6));
                sf.setCover(cursor.getString(7));
                sf.setPublishedDate(new Date(cursor.getLong(8)));
                sf.setCreatedDate(new Date(cursor.getLong(9)));
                sf.setLastAccessedDate(lastModified);
                String title = cursor.getString(2);
                StringTokenizer token = new StringTokenizer(title, ",");
                List<String> titles = new ArrayList<String>(1);
                while (token.hasMoreTokens()) {
                    titles.add(token.nextToken());
                }
                sf.setTitles(titles);
                String author = cursor.getString(3);
                token = new StringTokenizer(author, ",");
                while (token.hasMoreTokens()) {
                    String auth = token.nextToken();
                    sf.addContributor(auth, "");
                }
                String desc = cursor.getString(4);
                if (desc != null) {
                    sf.setDescription(desc);
                }
                String keywords = cursor.getString(5);
                if (keywords != null) {
                    token = new StringTokenizer(keywords, ",");
                    while (token.hasMoreTokens()) {
                        String keyword = token.nextToken();
                        sf.addKeywords(keyword);
                    }
                }
                sf.setSeries(cursor.getString(11));
                sf.setBookInDB(true);
                m_Files.add(sf);
                cursor.moveToNext();
            }
            cursor.close();
            if (m_DeleteBooks.size() > 0) {
                deleteBooks();
                m_DeleteBooks.clear();
            }
            Log.i("OtherBooks", " adding books from db - count = " + m_Files.size());
            Log.i("OtherBooks", " count of books already updated = " + m_UpdatedFiles.size());
            nookLib.updatePageView(m_Files);
            m_Files.clear();
            updateLastScan();
        } catch (Exception ex) {
            Log.e("OtherBooks", "Exception querying datbase", ex);
        }
        return null;
    }
    
    public void getOtherBooks() {
        try {
            getBooksFromDB();
            File file = new File(nookBaseActivity.SDFOLDER);
            File external = new File(nookBaseActivity.EXTERNAL_SDFOLDER);
            FileFilter filter = new FileFilter() {
                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        if (f.getName().equals("my B&N downloads")) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                    if (m_UpdatedFiles.contains(f)) { return false; }
                    String extension = f.getName().toLowerCase();
                    if (extension.endsWith("epub") || extension.endsWith("htm") || extension.endsWith("txt")
                        || extension.endsWith("html") || extension.endsWith("pdf")) {
                        return true;
                    } else {
                        return false;
                    }
                }
                
            };
            retrieveFiles(file, filter);
            retrieveFiles(external, filter);
            if (m_Files.size() > 0) {
                nookLib.updatePageView(m_Files);
                m_Files.clear();
            }
            m_UpdatedFiles.clear();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }
    
    private void retrieveFiles(File base, FileFilter filter) {
        File skipFile = new File(base, ".skip");
        if (skipFile.exists()) { return; }
        
        File[] files = base.listFiles(filter);
        if (files == null) { return; }
        for (File file : files) {
            if (file.isDirectory()) {
                retrieveFiles(file, filter);
            } else {
                if (file.getName().startsWith(".")) {
                    continue;
                }
                ScannedFile file1 = new ScannedFile(file.getAbsolutePath());
                file1.setLastAccessedDate(new Date(file.lastModified()));
                file1.setBookInDB(false);
                m_Files.add(file1);
                if (m_Files.size() % 100 == 0) {
                    nookLib.updatePageView(m_Files);
                    m_Files.clear();
                }
            }
        }
        return;
    }
    
}