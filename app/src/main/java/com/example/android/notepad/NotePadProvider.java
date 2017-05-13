/*
 * Copyright (C) 2007 The Android Open Source Project
 *
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

/**
 * NotePad的ContentProvider，NotePad的内容提供者，这里要注意实际上NotePad应用并不允许其他程序共享其数据
 */

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.ContentProvider.PipeDataWriter;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.LiveFolders;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/**
 * Provides access to a database of notes. Each note has a title, the note
 * itself, a creation date and a modified data.
 */
public class NotePadProvider extends ContentProvider implements PipeDataWriter<Cursor> {
    // 用于调试和日志记录
    private static final String TAG = "NotePadProvider";

    /**
     * 提供者用作其底层数据存储的数据库
     */
    private static final String DATABASE_NAME = "note_pad.db";

    /**
     * 数据库版本
     */
    private static final int DATABASE_VERSION = 2;

    /**
     * 用于从数据库中选择列的投影映射
     */
    private static HashMap<String, String> sNotesProjectionMap;

    /**
     * 用于从数据库中选择列的投影映射
     */
    private static HashMap<String, String> sLiveFolderProjectionMap;

    /**
     * 普通音符的有趣列的标准投影。
     */
    private static final String[] READ_NOTE_PROJECTION = new String[] {
            NotePad.Notes._ID,               // 投影位置0，笔记的id
            NotePad.Notes.COLUMN_NAME_NOTE,  // 投影位置1，笔记的内容
            NotePad.Notes.COLUMN_NAME_TITLE, //投影位置2，笔记的标题

    };
    private static final int READ_NOTE_NOTE_INDEX = 1;
    private static final int READ_NOTE_TITLE_INDEX = 2;

    /*
     * Uri matcher使用的常量根据传入Uri的模式选择操作
     */
    // 传入URI与Notes URI模式匹配
    private static final int NOTES = 1;

    // 传入URI与Note ID URI模式匹配
    private static final int NOTE_ID = 2;

    //传入URI与Live文件夹URI模式相匹配
    private static final int LIVE_FOLDER_NOTES = 3;

    /**
     * UriMatcher实例
     */
    private static final UriMatcher sUriMatcher;

    //处理一个新的数据库助手。
    private DatabaseHelper mOpenHelper;


    /**
     *实例化并设置静态对象的块
     */
    static {

        /*
         *创建并初始化URI matcher
         */
        // 创建一个新实例
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // 添加一个模式，将uri终止与notes操作的“notes”终止
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);

        // 添加一个模式，该模式以“notes”和一个整数作为一个注释ID的操作终止
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);

        // 添加一个模式，将uri终止与live_folders / notes路由到一个活的文件夹操作
        sUriMatcher.addURI(NotePad.AUTHORITY, "live_folders/notes", LIVE_FOLDER_NOTES);

        /*
         * 创建并初始化返回所有列的投影映射
         */

        // 创建一个新的投影映射实例。该映射返回给定字符串的列名。两者通常是相等的。
        sNotesProjectionMap = new HashMap<String, String>();

        // 将字符串“_ID”映射到列名称“_ID”
        sNotesProjectionMap.put(NotePad.Notes._ID, NotePad.Notes._ID);

        // 将 "title" 映射到 "title"
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_TITLE);

        // 将 "note"映射 "note"
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_NOTE);

        //将"created"映射到"created"
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE,
                NotePad.Notes.COLUMN_NAME_CREATE_DATE);

        // 将 "modified" 映射到"modified"
        sNotesProjectionMap.put(
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);

        /*
         *创建用于处理活文件夹的投影映射
         */

        // 创建一个新的投影映射实例
        sLiveFolderProjectionMap = new HashMap<String, String>();

        // 将“_ID”映射为“_ID”作为live文件夹
        sLiveFolderProjectionMap.put(LiveFolders._ID, NotePad.Notes._ID + " AS " + LiveFolders._ID);

        // 将“名称”映射为“名称”
        sLiveFolderProjectionMap.put(LiveFolders.NAME, NotePad.Notes.COLUMN_NAME_TITLE + " AS " +
            LiveFolders.NAME);
    }

    /**
    *
    * 这个类帮助打开、创建和升级数据库文件。为了测试的目的，设置了包的可见性。
    */
   static class DatabaseHelper extends SQLiteOpenHelper {

       DatabaseHelper(Context context) {

           // 调用超级构造函数，请求默认光标工厂。
           super(context, DATABASE_NAME, null, DATABASE_VERSION);
       }

       /**
        *
        * 使用从NotePad类中获取的表名和列名创建底层数据库。
        */
       @Override
       public void onCreate(SQLiteDatabase db) {
           db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
                   + NotePad.Notes._ID + " INTEGER PRIMARY KEY,"
                   + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
                   + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
                   + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                   + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER"
                   + ");");
       }

       /**
        *
        * 演示提供者必须考虑底层数据存储更改时发生的情况。在这个示例中，数据库通过销毁现有数据升级数据库。
        一个真正的应用程序应该升级数据库。
        */
       @Override
       public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

           // 数据库正在升级的日志
           Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                   + newVersion + ", which will destroy all old data");

           // 杀死表和现有数据
           db.execSQL("DROP TABLE IF EXISTS notes");

           // 用一个新版本重新创建数据库
           onCreate(db);
       }
   }

   /**
    *
    * 通过创建一个新的数据库助手来初始化提供者。onCreate()是在Android为响应来自客户的解析器请求
    * 而创建提供程序时自动调用的。
    */
   @Override
   public boolean onCreate() {

       // 创建一个新的helper对象。请注意，在尝试访问数据库之前，数据库本身并没有打开，而且只有在它不存在时才创建。
       mOpenHelper = new DatabaseHelper(getContext());

       // 假设任何故障将被抛出异常报告。
       return true;
   }

   /**
    * 此方法在客户端调用时调用
    * {@link android.content.ContentResolver#query(Uri, String[], String, String[], String)}.
    *查询数据库并返回包含结果的游标。
    *
    * 返回包含查询结果的游标。游标存在，但是如果查询返回没有结果或异常，则是空的。
    @抛出IllegalArgumentException，如果传入的URI模式无效。
    */
   @Override
   public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
           String sortOrder) {

       // 构造一个新的查询生成器并设置它的表名
       SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
       qb.setTables(NotePad.Notes.TABLE_NAME);

       /**
        * 选择投影，并根据URI模式匹配调整“where”子句。
        */
       switch (sUriMatcher.match(uri)) {
           // If the incoming URI is for notes, chooses the Notes projection
           case NOTES:
               qb.setProjectionMap(sNotesProjectionMap);
               break;

           /* 如果传入URI是由其ID标识的单个音符，则选择note ID的投影，并将“_ID = < noteID >”
           附加到where子句，这样它就会选择单个的音符
            */
           case NOTE_ID:
               qb.setProjectionMap(sNotesProjectionMap);
               qb.appendWhere(
                   NotePad.Notes._ID +    // the name of the ID column
                   "=" +
                   // 注释ID在传入URI中的位置
                   uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION));
               break;

           case LIVE_FOLDER_NOTES:
               // 如果传入URI来自一个活动文件夹，则选择live文件夹投射。
               qb.setProjectionMap(sLiveFolderProjectionMap);
               break;

           default:
               //如果URI不匹配任何已知模式，则抛出异常。
               throw new IllegalArgumentException("Unknown URI " + uri);
       }


       String orderBy;
       // 如果没有指定排序，则使用默认值
       if (TextUtils.isEmpty(sortOrder)) {
           orderBy = NotePad.Notes.DEFAULT_SORT_ORDER;
       } else {
           // 否则，使用传入排序顺序
           orderBy = sortOrder;
       }

       //在“读”模式中打开数据库对象，因为不需要编写任何写操作。
       SQLiteDatabase db = mOpenHelper.getReadableDatabase();

       /*
        * 执行查询。如果在读取数据库时没有出现问题，则返回游标对象;否则，光标变量将包含null。
        * 如果没有选择记录，则光标对象是空的，Cursor.getCount()返回0。
        */
       Cursor c = qb.query(
           db,            // 数据库查询
           projection,    // 从查询返回的列
           selection,     // where子句的列
           selectionArgs, // where子句的值
           null,          // 别组的行
           null,          //不要按行组进行筛选
           orderBy        // 排序顺序
       );

       // 告诉光标什么URI要看，这样它就知道它的源数据何时变化
       c.setNotificationUri(getContext().getContentResolver(), uri);
       return c;
   }

   /**
    * 当客户调用{ @ link android.content. contentresolver # getType(Uri)}时，调用此操作。
    * 返回作为参数的URI的MIME数据类型。
    *
    * @ param uri，它的MIME类型是需要的。
    @返回URI的MIME类型。
    @抛出IllegalArgumentException，如果传入的URI模式无效。
    */
   @Override
   public String getType(Uri uri) {

       /**
        * 根据传入的URI模式选择MIME类型
        */
       switch (sUriMatcher.match(uri)) {

           // 如果模式是用于notes或live文件夹，则返回一般内容类型。
           case NOTES:
           case LIVE_FOLDER_NOTES:
               return NotePad.Notes.CONTENT_TYPE;

           // 如果模式是用于注释ID，则返回注释ID内容类型。
           case NOTE_ID:
               return NotePad.Notes.CONTENT_ITEM_TYPE;

           // 如果URI模式不匹配任何允许的模式，则抛出异常。
           default:
               throw new IllegalArgumentException("Unknown URI " + uri);
       }
    }

//BEGIN_INCLUDE(stream)
    /**
     * 这描述了支持打开注释URI作为流的MIME类型。
     */
    static ClipDescription NOTE_STREAM_TYPES = new ClipDescription(null,
            new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN });

    /**
     * 返回可用数据流的类型。支持特定的uri。应用程序可以将这样的注释转换为纯文本流。
     *
     * @ param uri用于分析
     * @ param mimeTypeFilter来检查MIME类型。这个方法只返回一个数据流
     类型用于匹配过滤器的MIME类型。目前，只有文本/纯MIME类型匹配。
     @返回数据流MIME类型。目前，只返回文本/计划。
     @抛出IllegalArgumentException，如果URI模式不匹配任何受支持的模式。
     */
    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        /**
         *  根据传入的URI模式选择数据流类型。
         */
        switch (sUriMatcher.match(uri)) {

            // 如果模式是用于notes或live文件夹，则返回null。这种类型的URI不支持数据流。
            case NOTES:
            case LIVE_FOLDER_NOTES:
                return null;

            // 如果模式是用于注释id，而MIME过滤器是文本/ plain，则返回文本/ plain
            case NOTE_ID:
                return NOTE_STREAM_TYPES.filterMimeTypes(mimeTypeFilter);

                //如果URI模式不匹配任何允许的模式，则抛出异常。
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
    }


    /**
     *返回每个支持流类型的数据流。此方法对传入的URI进行查询，然后使用
     * { @ link android. content . content。ContentProvider #
     * openPipeHelper(Uri,String,Bundle,Object,PipeDataWriter)}以启动另一个线程，以将数据转换为流。
     *
     *@ param uri指向数据流的uri模式
     @ param mimeTypeFilter一个包含MIME类型的字符串。这个方法试图得到一个流
     带有此MIME类型的数据。
     @ param选择调用者提供的其他选项。可以解释为
     由内容提供商所要求的。
     * @ return AssetFileDescriptor为文件的句柄。
     * @抛出FileNotFoundException，如果没有与传入URI关联的文件。
     */
    @Override
    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts)
            throws FileNotFoundException {

        // 检查MIME类型过滤器是否匹配支持的MIME类型。
        String[] mimeTypes = getStreamTypes(uri, mimeTypeFilter);

        // 如果支持MIME类型
        if (mimeTypes != null) {

            // 检索该URI的注释。使用为该提供程序定义的查询方法，而不是使用数据库查询方法。
            Cursor c = query(
                    uri,                    // 注释的URI
                    READ_NOTE_PROJECTION,   // 获取包含注释的ID、标题和内容的投影
                    null,                   // 没有WHERE子句，获取所有匹配记录
                    null,                   // 因为没有WHERE子句，没有选择标准
                    null                    // 使用默认的排序顺序(修改日期，降序)
            );


            // 如果查询失败，或者游标为空，停止
            if (c == null || !c.moveToFirst()) {

                // 如果游标是空的，只需关闭游标并返回
                if (c != null) {
                    c.close();
                }

                // 如果游标为空，则抛出异常
                throw new FileNotFoundException("Unable to query " + uri);
            }

            // 启动一个新的线程，将流数据传输回调用者。
            return new AssetFileDescriptor(
                    openPipeHelper(uri, mimeTypes[0], opts, c, this), 0,
                    AssetFileDescriptor.UNKNOWN_LENGTH);
        }

        // 如果不支持MIME类型，则返回一个只读句柄。
        return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    /**
     * 实现{ @link android.content.ContentProvider。PipeDataWriter }执行实际工作，
     * 将一个游标中的数据转换为一个数据流，以便客户端读取数据。
     */
    @Override
    public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType,
            Bundle opts, Cursor c) {
        //我们目前只支持从单个注释条目中获得的conversitext，所以不需要在这里进行游标数据类型检查。
        FileOutputStream fout = new FileOutputStream(output.getFileDescriptor());
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new OutputStreamWriter(fout, "UTF-8"));
            pw.println(c.getString(READ_NOTE_TITLE_INDEX));
            pw.println("");
            pw.println(c.getString(READ_NOTE_NOTE_INDEX));
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "Ooops", e);
        } finally {
            c.close();
            if (pw != null) {
                pw.flush();
            }
            try {
                fout.close();
            } catch (IOException e) {
            }
        }
    }
//END_INCLUDE(stream)

    /**
     * 这在客户端调用时调用
     * { @link android.content。ContentValues ContentResolver #插入(Uri)}。
     *将新行插入到数据库中。该方法为不包含在传入映射中的任何列设置默认值。
     *如果插入行，则通知侦听器更改。
     @返回插入行的行ID。
      * @抛出SQLException，如果插入失败。
     */
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {

        // 验证传入的URI。只有完整的提供者URI才允许插入。
        if (sUriMatcher.match(uri) != NOTES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // 保存新记录的值的映射。
        ContentValues values;

        // 如果传入的值映射不是null，则使用它为新值。
        if (initialValues != null) {
            values = new ContentValues(initialValues);

        } else {
            //否则，创建一个新的值映射
            values = new ContentValues();
        }

        // 以毫秒为单位获取当前系统时间
        Long now = Long.valueOf(System.currentTimeMillis());

        // 如果值映射不包含创建日期，则将值设置为当前时间。
        if (values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE) == false) {
            values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, now);
        }

        // 如果值映射不包含修改日期，则将值设置为当前时间。
        if (values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE) == false) {
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
        }

        //如果值映射不包含标题，则将值设置为默认的标题。
        if (values.containsKey(NotePad.Notes.COLUMN_NAME_TITLE) == false) {
            Resources r = Resources.getSystem();
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, r.getString(android.R.string.untitled));
        }

        //如果值映射不包含注释文本，则将值设置为空字符串。
        if (values.containsKey(NotePad.Notes.COLUMN_NAME_NOTE) == false) {
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, "");
        }

        // 在“写”模式中打开数据库对象。
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        // 执行插入并返回新记录的ID。
        long rowId = db.insert(
            NotePad.Notes.TABLE_NAME,        //要插入的表。
            NotePad.Notes.COLUMN_NAME_NOTE,  //当值为空时，SQLite将此列值设置为null。
            values                           // 列名称的映射，以及插入到列中的值。
        );

        // 如果插入成功，行ID就存在。
        if (rowId > 0) {
            //创建带有注释ID模式的URI，并添加新的行ID。
            Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, rowId);

            // 向在此提供者注册的观察者通知，数据发生了变化。
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        // 如果插入没有成功，那么rowID是< = 0。将抛出一个异常。
        throw new SQLException("Failed to insert row into " + uri);
    }

    /**
     * 当客户调用{ @ link android. content . content时，调用此操作。ContentResolver #
     * 删除(Uri,String,String[])}。删除数据库中的记录。如果传入URI与note ID URI模式相匹配，
     * 此方法将删除URI中ID指定的一个记录。否则，它将删除一组记录。
     * 记录或记录也必须与在where和where中指定的输入选择条件相匹配。
     *
     * 如果删除行，则通知侦听器更改。
     @ return如果使用“where”子句，则返回受影响的行数，否则返回0。要删除所有行并获取行数，使用“1”作为where子句。
     @抛出IllegalArgumentException，如果传入的URI模式无效。
     */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        // 在“写”模式中打开数据库对象。
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalWhere;

        int count;

        // 根据传入的URI模式执行删除操作。
        switch (sUriMatcher.match(uri)) {

            // 如果传入模式与notes的一般模式相匹配，则根据传入的“where”列和参数执行删除操作。
            case NOTES:
                count = db.delete(
                    NotePad.Notes.TABLE_NAME,  // 数据库表名
                    where,                     // 传入where子句列名
                    whereArgs                  // 传入where子句的值
                );
                break;

                // 如果传入的URI匹配单个的注释ID，则根据传入的数据执行删除操作，但修改where子句
                // 将其限制为特定的注释ID。
            case NOTE_ID:
                /*
                 * 通过将其限制在需要的注释ID上，开始最后的WHERE子句。
                 */
                finalWhere =
                        NotePad.Notes._ID +                              // ID列名称
                        " = " +                                          // 测试平等
                        uri.getPathSegments().                           // 传入的注意ID
                            get(NotePad.Notes.NOTE_ID_PATH_POSITION)
                ;

                // 如果有其他选择标准，将它们附加到决赛
                // WHERE 语句
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                // 执行删除。
                count = db.delete(
                    NotePad.Notes.TABLE_NAME,  // 数据库表名。
                    finalWhere,                // 最后一个WHERE子句
                    whereArgs                  // 传入where子句的值。
                );
                break;

            //如果传入模式无效，则抛出异常。
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        /**获取当前上下文的内容解析器对象的句柄，并通知传入URI更改。对象将此传递给解析器框架，
        并通知为提供者注册的观察者。
         */
        getContext().getContentResolver().notifyChange(uri, null);

        // 返回删除的行数。
        return count;
    }

    /**
     * 当客户调用{ @ link android.content. contentresolver #
     * update(Uri,ContentValues,String,String[])}更新记录时，就会调用这个调用。
     * 值映射中的键所指定的列名称将被更新，并使用映射中值指定的新数据。
     * 如果传入URI与note ID URI模式相匹配，那么该方法将更新URI中ID指定的一个记录;
     * 否则，它将更新一组记录。记录或记录必须与在where和whereArgs指定的输入选择条件相匹配。
     * 如果更新了行，则通知侦听器更改。
     *
     *@ param uri用于匹配和更新的uri模式。
     @ param值一个列名称(键)和新值(值)的映射。
     @ param，其中一个SQL“where”子句根据其列值选择记录。如果这
     *是null，然后选择匹配URI模式的所有记录。
     @ param在这个地方有一系列的选择标准。如果“where”param包含值占位符(“?”)，那么每个占位符将被数组中的对应元素替换。
     * @返回更新的行数。
     @抛出IllegalArgumentException，如果传入的URI模式无效。
     */
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {

        // 在“写”模式中打开数据库对象。
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;

        // 更新是否基于传入的URI模式
        switch (sUriMatcher.match(uri)) {

            // 如果传入的URI与一般的notes模式匹配，那么根据传入数据进行更新。
            case NOTES:

                // 执行更新并返回更新的行数。
                count = db.update(
                    NotePad.Notes.TABLE_NAME, // 数据库表名。
                    values,                   // 使用的列名称和新值的映射。
                    where,                    // where子句列名。
                    whereArgs                 // where子句的列值选择。
                );
                break;

            // 如果传入的URI匹配单个的注释ID，则根据传入的数据进行更新，但是修改where子句将其限制为特定的注释ID。
            case NOTE_ID:
                // 从传入的URI中获取注释ID
                String noteId = uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION);

                /*
                 * 开始创建最终的WHERE子句，将其限制为传入的注释ID。
                 */
                finalWhere =
                        NotePad.Notes._ID +                              // ID列名称
                        " = " +                                          // 测试平等
                        uri.getPathSegments().                           //传入的注意ID
                            get(NotePad.Notes.NOTE_ID_PATH_POSITION)
                ;

                // 如果有其他选择标准，将它们附加到最后的WHERE子句
                if (where !=null) {
                    finalWhere = finalWhere + " AND " + where;
                }


                // 执行更新并返回更新的行数。
                count = db.update(
                    NotePad.Notes.TABLE_NAME, // 数据库表名。
                    values,                   // 使用的列名称和新值的映射。
                    finalWhere,               // 使用占位符的最后WHERE子句
                    whereArgs                 // 如果值在where参数中，则在where子句列值选择on或null。
                );
                break;
            // 如果传入模式无效，则抛出异常。
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        /*获取当前上下文的内容解析器对象的句柄，并通知传入URI更改。对象将此传递给解析器框架，
        并通知为提供者注册的观察者。
         */
        getContext().getContentResolver().notifyChange(uri, null);

        //返回更新的行数。
        return count;
    }

    /**
     * 一个测试包可以调用此方法来获取潜在的NotePadProvider数据库的句柄，因此它可以将测试数据插入
     * 到数据库中。测试用例类负责在测试环境中实例化提供程序;
     * { @ link android.test。ProviderTestCase2 }在调用setUp()期间执行此操作
     *
     * 为提供者的数据返回数据库助手对象的句柄。
     */
    DatabaseHelper getOpenHelperForTest() {
        return mOpenHelper;
    }
}
