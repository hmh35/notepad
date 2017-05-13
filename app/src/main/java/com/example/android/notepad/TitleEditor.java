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

package com.example.android.notepad;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

/**
 * 这个活动允许用户编辑一个音符的标题。它显示一个包含EditText的浮动窗口。
 *
 * 注意:注意，此活动中的提供者操作在UI线程上进行。这不是一个好的做法。这只是为了让代码更容易阅读。
 * 一个真正的应用程序应该使用{ @ linkandroid.content . content。AsyncQueryHandler }
 * 或{ @link android.os。AsyncTask }对象在单独的线程上异步执行操作。
 */
public class TitleEditor extends Activity {

    /**
     * 这是一个特殊的意图动作，意思是“编辑一个音符的标题”。
     */
    public static final String EDIT_TITLE_ACTION = "com.android.notepad.action.EDIT_TITLE";

    // 创建一个返回注释ID和注释内容的投影。
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
    };

    //提供者返回的游标中标题列的位置。
    private static final int COLUMN_INDEX_TITLE = 1;

    // 一个游标对象，该对象将包含查询提供程序的结果。
    private Cursor mCursor;

    //保存编辑标题的EditText对象。
    private EditText mText;

    //用于编辑标题的URI对象。
    private Uri mUri;

    /**
     * 当活动开始时，这个方法被Android调用。从传入的意图中，它决定了需要什么样的编辑，然后执行它。
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 为这个活动对象的UI设置视图。
        setContentView(R.layout.title_editor);

        // 获取激活该活动的意图，并从中获取需要编辑的标题的URI。
        mUri = getIntent().getData();

        /*
         * 使用带有触发意图的URI，得到通知。
         *
         * 注意:这是在UI线程上完成的。它将阻塞线程，直到查询完成。在一个示例应用程序中，
         * 根据本地数据库对一个简单的提供者进行攻击，这个块将是短暂的，但是在一个真正的应用程序中，
         * 你应该使用android.content。AsyncQueryHandler或android.os.AsyncTask。
         */

        mCursor = managedQuery(
            mUri,        // 要检索的注释的URI。
            PROJECTION,  // 列检索
            null,        //没有使用选择标准，因此不需要列。
            null,        // 不使用列，所以不需要值。
            null         // 不需要排序。
        );

        // 获取EditText框的视图ID
        mText = (EditText) this.findViewById(R.id.title);
    }

    /**
     * 当活动即将到达前台时，调用此方法。当活动进入任务堆栈的顶部时，或者在第一次启动时，就会出现这种情况。
     *
     * 显示选中的注释的当前标题。
     */
    @Override
    protected void onResume() {
        super.onResume();

        // 验证在onCreate()中生成的查询实际上是有效的。如果它起作用，那么游标对象不是null。
        // 如果它是*空*，那么mCursor.getCount()= = 0。
        if (mCursor != null) {

            // 光标刚刚被检索到，所以它的索引被设置为一个记录* * * * * * *。这使它达到了第一个记录。
            mCursor.moveToFirst();

            // 在EditText对象中显示当前的标题文本。
            mText.setText(mCursor.getString(COLUMN_INDEX_TITLE));
        }
    }

    /**
     *当活动失去焦点时调用此方法。
     *
     * 对于编辑信息的活动对象，onPause()可能是保存更改的地方。Android应用程序模型基于“保存”和
     * “退出”不需要操作的概念。当用户离开某个活动时，他们不应该返回去完成他们的工作。离开的行为
     * 应该把一切都保存下来，并把活动留在一个Android可以在必要的情况下摧毁它的状态。
     *
     * Updates the note with the text currently in the text box.
     */
    @Override
    protected void onPause() {
        super.onPause();

        // 验证在onCreate()中生成的查询实际上是有效的。如果它起作用，那么游标对象不是null。
        // 如果它是*空*，那么mCursor.getCount()= = 0。

        if (mCursor != null) {

            //创建用于更新提供者的值映射。
            ContentValues values = new ContentValues();

            // 在values映射中，将标题设置为编辑框的当前内容。
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, mText.getText().toString());

            /*
             * 使用note的新标题更新提供程序。
             *
             * 注意:这是在UI线程上完成的。它将阻塞线程，直到更新完成。在一个示例应用程序中，根据
             * 本地数据库对一个简单的提供者进行攻击，这个块将是短暂的，但是在一个真正的应用程序中，
             * 你应该使用android.content。AsyncQueryHandler或android.os.AsyncTask。
             */
            getContentResolver().update(
                mUri,    // 
                values,  // 值映射包含要更新的列和要使用的值。
                null,    // 没有使用选择标准，因此不需要“where”列。
                null     //不使用“where”列，所以不需要“where”值。
            );

        }
    }

    public void onClickOk(View v) {
        finish();
    }
}
