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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 这个活动处理“编辑”注释，编辑对{ @ link Intent # ACTION_VIEW }(请求查看数据)，编辑一个注释
 * { @ link Intent # ACTION_EDIT }，创建一个注释{ @ link Intent # ACTION_INSERT }，
 * 或创建一个新记录，从当前的剪贴板{ @ link Intent # action_粘贴}。
 *
 * 注意:注意，此活动中的提供者操作在UI线程上进行。这不是一个好的做法。这只是为了让代码更容易阅读。
 * 一个真正的应用程序应该使用{ @ link android. content . content。AsyncQueryHandler }或
 * { @link android.os。AsyncTask }对象在单独的线程上异步执行操作。
 */
public class NoteEditor extends Activity {
    // 用于日志和调试的目的
    private static final String TAG = "NoteEditor";

    /*
     * 创建一个返回注释ID和注释内容的投影。
     */
    private static final String[] PROJECTION =
        new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_NOTE
    };

    // A label for the saved state of the activity
    //活动的保存状态的标签
    private static final String ORIGINAL_CONTENT = "origContent";

    // This Activity can be started by more than one action. Each action is represented
    // as a "state" constant这个活动可以由多个操作开始。每个动作被表示为一个“状态”常量
    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;

    // Global mutable variables 全局变量
    private int mState;
    private Uri mUri;
    private Cursor mCursor;
    private EditText mText;
    private String mOriginalContent;

    /**
     * Defines a custom EditText View that draws lines between each line of text that is displayed.
     * 定义一个自定义EditText视图，它在显示的每一行文本之间画线。
     */
    public static class LinedEditText extends EditText {
        private Rect mRect;
        private Paint mPaint;

        // This constructor is used by LayoutInflater
        //这个构造函数由LayoutInflater使用
        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);

            // Creates a Rect and a Paint object, and sets the style and color of the Paint object.
            //创建一个Rect和一个绘图对象，并设置颜料对象的样式和颜色。
            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }

        /**
         * This is called to draw the LinedEditText object
         * 这被调用来绘制LinedEditText对象
         * @param canvas The canvas on which the background is drawn.
         *               背景的画布。
         */
        @Override
        protected void onDraw(Canvas canvas) {

            // Gets the number of lines of text in the View.
            //获取视图中的文本行数。
            int count = getLineCount();

            // Gets the global Rect and Paint objects
            //获取全局的Rect和绘制对象
            Rect r = mRect;
            Paint paint = mPaint;

            /*
             * Draws one line in the rectangle for every line of text in the EditText
             * 在矩形中为EditText中的每行文本绘制一行
             */
            for (int i = 0; i < count; i++) {

                // Gets the baseline coordinates for the current line of text
                //获取当前文本行的基线坐标
                int baseline = getLineBounds(i, r);

                /*
                 * Draws a line in the background from the left of the rectangle to the right,
                 * at a vertical position one dip below the baseline, using the "paint" object
                 * for details.从矩形的左边绘制一条线到右边，在一个垂直的位置，在基线的下方，使
                 * 用“绘制”对象的细节。
                 */
                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
            }

            // Finishes up by calling the parent method
            //通过调用父方法结束
            super.onDraw(canvas);
        }
    }

    /**
     * This method is called by Android when the Activity is first started. From the incoming
     * Intent, it determines what kind of editing is desired, and then does it.
     * 当活动开始时，这个方法被Android调用。从传入
     意图，它决定什么样的编辑是需要的，然后做它。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Creates an Intent to use when the Activity object's result is sent back to the
         * caller.创建用于将活动对象的结果发送回调用者的意图。
         */
        final Intent intent = getIntent();

        /*
         *  Sets up for the edit, based on the action specified for the incoming Intent.
         *  根据输入意图指定的操作为编辑设置。
         */

        // Gets the action that triggered the intent filter for this Activity
        //获取触发此活动的意图过滤器的操作
        final String action = intent.getAction();

        // For an edit action:一个编辑操作:
        if (Intent.ACTION_EDIT.equals(action)) {

            // Sets the Activity state to EDIT, and gets the URI for the data to be edited.
            //将活动状态设置为EDIT，并获取要编辑的数据的URI。
            mState = STATE_EDIT;
            mUri = intent.getData();

            // For an insert or paste action:对于插入或粘贴操作:
        } else if (Intent.ACTION_INSERT.equals(action)
                || Intent.ACTION_PASTE.equals(action)) {

            // Sets the Activity state to INSERT, gets the general note URI, and inserts an empty record in the provider
            //将活动状态设置为INSERT，获取general note URI，并在提供程序中插入一个空记录
            mState = STATE_INSERT;
            mUri = getContentResolver().insert(intent.getData(), null);

            /*
             * If the attempt to insert the new note fails, shuts down this Activity. The
             * originating Activity receives back RESULT_CANCELED if it requested a result.
             * Logs that the insert failed.
             * 如果插入新注释的尝试失败，关闭该活动。原始活动收到返回的result_取消，如果它请求结果。插入失败的日志。
             */
            if (mUri == null) {

                // Writes the log identifier, a message, and the URI that failed.
                //写日志标识符、消息和失败的URI。
                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());

                // Closes the activity.
                //关闭活动。
                finish();
                return;
            }

            // Since the new entry was created, this sets the result to be returned
            // 由于新条目被创建，因此将返回结果
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

        // If the action was other than EDIT or INSERT:
            //如果动作不是编辑或插入:
        } else {

            // Logs an error that the action was not understood, finishes the Activity, and
            // returns RESULT_CANCELED to an originating Activity.
            //记录操作未被理解的错误，完成活动，并将RESULT_CANCELED为原始活动。
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        /*
         * Using the URI passed in with the triggering Intent, gets the note or notes in
         * the provider.
         * Note: This is being done on the UI thread. It will block the thread until the query
         * completes. In a sample app, going against a simple provider based on a local database,
         * the block will be momentary, but in a real app you should use
         * android.content.AsyncQueryHandler or android.os.AsyncTask.
         * 使用带有触发意图的URI，获取提供程序中的注释或注释。注意:这是在UI线程上完成的。
         * 它将阻塞线程，直到查询完成。在一个示例应用程序中，根据本地数据库对一个简单的提供者
         * 进行攻击，这个块将是短暂的，但是在一个真正的应用程序中，你应该使用
         * android.content。AsyncQueryHandler或android.os.AsyncTask。
         */
        mCursor = managedQuery(
            mUri,         // 从提供者处获取多个notes的URI。
            PROJECTION,   // 为每个音符返回注释ID和注释内容的投影。
            null,         // 没有“where”条款选择标准。
            null,         // 没有“where”子句选择值。
            null          // 使用默认的排序顺序(修改日期，降序)
        );

        //对于粘贴，从剪贴板初始化数据。
        // (必须在m光标初始化之后完成。)
        if (Intent.ACTION_PASTE.equals(action)) {
            // 粘贴的
            performPaste();
            // 切换状态编辑，这样标题就可以修改。
            mState = STATE_EDIT;
        }

        // 设置该活动的布局。见res / layout / note_editor.xml
        setContentView(R.layout.note_editor);

        // 在布局中获取EditText的句柄。
        mText = (EditText) findViewById(R.id.note);

        /*
         * 如果此活动之前停止，它的状态将写入保存的实例状态中的原始内容位置。这得到了状态。
         */
        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
        }
    }

    /**
     * 当活动即将到达前台时，调用此方法。当活动进入任务堆栈的顶部时，或者在第一次启动时，就会出现
     * 这种情况。
     *
     * 移动到列表中的第一个注释，为用户选择的操作设置适当的标题，将注释内容放到TextView中，并将原始
     * 文本保存为备份。
     */
    @Override
    protected void onResume() {
        super.onResume();

        /*
         * m光标被初始化，因为onCreate()总是在任何正在运行的进程的onResume之前。这个测试不是空的，
         * 因为它应该总是包含数据。
         */
        if (mCursor != null) {
            //在暂停(如标题)时，请求发生变化
            mCursor.requery();

            /* 移到第一个记录。在首次访问游标数据之前，总是调用moveToFirst()。
            使用游标的语义是当它被创建时，它的内部索引会在第一个记录之前指向一个“位置”。
             */
            mCursor.moveToFirst();

            // 根据当前活动状态修改活动的窗口标题。
            if (mState == STATE_EDIT) {
                // 将活动的标题设置为包含注释标题
                int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                String title = mCursor.getString(colTitleIndex);
                Resources res = getResources();
                String text = String.format(res.getString(R.string.title_edit), title);
                setTitle(text);
            // 将标题设置为“创建”用于插入
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.title_create));
            }

            /*
             * onResume() may have been called after the Activity lost focus (was paused).
             * The user was either editing or creating a note when the Activity paused.
             * The Activity should re-display the text that had been retrieved previously, but
             * it should not move the cursor. This helps the user to continue editing or entering.
             * onResume()可能在活动失去焦点(暂停)后调用。当活动暂停时，用户可以编辑或创建一个通知。
             * 该活动应该重新显示以前检索过的文本，但是它不应该移动光标。这有助于用户继续编辑或输入。
             */

            // 从游标中获取注释文本并将其放入TextView，但不会更改
            // 文本光标的位置。
            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            String note = mCursor.getString(colNoteIndex);
            mText.setTextKeepState(note);

            //存储原始的注释文本，以允许用户恢复更改。
            if (mOriginalContent == null) {
                mOriginalContent = note;
            }

        /*
         * 什么是错的。游标应该总是包含数据。报告一个错误。
         */
        } else {
            setTitle(getText(R.string.error_title));
            mText.setText(getText(R.string.error_message));
        }
    }

    /**
     * 当活动在正常操作中失去焦点时，该方法被调用，然后在稍后被杀死。该活动有机会保存它的状态，
     * 以便系统能够恢复它。
     *
     * 注意，此方法不是活动生命周期的正常部分。如果用户简单地从活动中导航，就不会调用它。
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // 保存原始文本，如果活动需要暂停，我们仍然保留它。
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }

    /**
     * 当活动失去焦点时调用此方法。
     *
     * 对于编辑信息的活动对象，onPause()可能是保存更改的地方。Android应用程序模型基于“保存”
     * 和“退出”不需要操作的概念。当用户离开某个活动时，他们不应该返回去完成他们的工作。离开
     * 的行为应该把一切都保存下来，并把活动留在一个Android可以在必要的情况下摧毁它的状态。
     *
     * 如果用户没有做任何事情，那么这将删除或清除注释，否则将向提供者写入用户的工作。
     */
    @Override
    protected void onPause() {
        super.onPause();

        /*
         * 测试查看查询操作没有失败(参见onCreate())。即使没有返回任何记录，也会存在游标对象，
         * 除非查询失败，因为有一些异常或错误。
         *
         */
        if (mCursor != null) {

            //获取当前的注释文本.
            String text = mText.getText().toString();
            int length = text.length();

            /*
             * 如果活动正在完成中，当前的注释中没有文本，则返回给调用者的结果，并删除该通知。
             * 即使是在被编辑的时候，这也是可以的，假设用户想要“清除”(删除)注释。
             */
            if (isFinishing() && (length == 0)) {
                setResult(RESULT_CANCELED);
                deleteNote();

                /*
                 * 为提供者写编辑器。如果在编辑器中检索了一个已存在的注释，并且插入了一个新的注释，
                 * 那么这个注释就被编辑了。在后一种情况下，onCreate()将一个新的空音符插入到提供程序
                 * 中，这是正在编辑的新注意事项。
                 */
            } else if (mState == STATE_EDIT) {
                // 创建映射以包含列的新值
                updateNote(text, null);
            } else if (mState == STATE_INSERT) {
                updateNote(text, text);
                mState = STATE_EDIT;
          }
        }
    }

    /**
     * 当用户第一次单击该设备的菜单按钮时，该方法将被调用。Android在一个包含项目的菜单对象中传递。
     *
     * 构建用于编辑和插入的菜单，并添加用于处理此应用程序的MIME类型的替代操作。
     *
     * @param menu 一个菜单对象，应该添加哪些项目。
     * @return 正确显示菜单。
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 从XML资源中增加菜单
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_options_menu, menu);

        // 只需要为保存的笔记添加额外的菜单项
        if (mState == STATE_EDIT) {
            // 将菜单项附加到任何其他可以使用它的活动。对于任何实现我们的数据的替代操作的活动，
            // 这将对系统进行查询，为所找到的每个活动添加一个菜单项。
            Intent intent = new Intent(null, mUri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                    new ComponentName(this, NoteEditor.class), null, intent, 0, null);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // 检查注意是否已经更改并启用/禁用恢复选项
        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
        String savedNote = mCursor.getString(colNoteIndex);
        String currentNote = mText.getText().toString();
        if (savedNote.equals(currentNote)) {
            menu.findItem(R.id.menu_revert).setVisible(false);
        } else {
            menu.findItem(R.id.menu_revert).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * 当选择菜单项时，将调用此方法。Android通过选中的项目。此方法中的switch语句调用适当的方法
     * 来执行用户选择的操作。
     *
     * @param item 选中的菜单项
     * @return 正确地指出该项目已经被处理，并且没有必要进行进一步的工作。
     * 在MenuItem对象中进行进一步的处理是错误的。
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //处理所有可能的菜单操作。
        switch (item.getItemId()) {
        case R.id.menu_save:
            String text = mText.getText().toString();
            updateNote(text, null);
            finish();
            break;
        case R.id.menu_delete:
            deleteNote();
            finish();
            break;
        case R.id.menu_revert:
            cancelNote();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

//BEGIN_INCLUDE(粘贴)
    /**
     * 一个助手方法，用剪贴板的内容替换注释的数据。
     */
    private final void performPaste() {

        // 获取剪贴板管理器的句柄
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        // 获取一个内容解析器实例
        ContentResolver cr = getContentResolver();

        // 从剪贴板获取剪贴板数据
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {

            String text=null;
            String title=null;

            // 从剪贴板数据获取第一项
            ClipData.Item item = clip.getItemAt(0);

            // 试图将条目的内容作为指向注释的URI
            Uri uri = item.getUri();

            // T测试发现这个项目实际上是一个URI，并且URI是指向一个提供者的内容URI，
            // 它的MIME类型与Note pad提供程序支持的MIME类型相同。
            if (uri != null && NotePad.Notes.CONTENT_ITEM_TYPE.equals(cr.getType(uri))) {

                // 剪贴板使用一个注释MIME类型引用数据。这副本。
                Cursor orig = cr.query(
                        uri,            // 内容提供者的URI
                        PROJECTION,     // 获取投影中提到的列
                        null,           // 没有选择变量
                        null,           // 没有选择变量，所以不需要标准
                        null            // 使用默认的排序顺序
                );

                // 如果游标不为空，并且它至少包含一个记录(moveToFirst()返回true)，那么它将从它获取注释数据。
                if (orig != null) {
                    if (orig.moveToFirst()) {
                        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
                        int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                        text = orig.getString(colNoteIndex);
                        title = orig.getString(colTitleIndex);
                    }

                    // 关闭游标.
                    orig.close();
                }
            }

            // 如果剪贴板的内容不是一个注释的引用，那么它就会将任何文本转换为文本。
            if (text == null) {
                text = item.coerceToText(this).toString();
            }

            // 用检索的标题和文本更新当前的注释。
            updateNote(text, title);
        }
    }
//END_INCLUDE(粘贴)

    /**
     * 用文本和标题作为参数替换当前的注释内容。
     * @param text 新的注释内容要使用。
     * @param title 使用的新注释标题
     */
    private final void updateNote(String text, String title) {

        // 设置映射，以包含在提供程序中更新的值。
        ContentValues values = new ContentValues();
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        //values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,dateString);

        //如果操作是插入一个新注释，那么这将为它创建一个初始标题。
        if (mState == STATE_INSERT) {

            // 如果没有提供任何标题作为参数，则从注释文本中创建一个标题。
            if (title == null) {
  
                // 注意的长度
                int length = text.length();

                // 通过获取一个文本的子串来设置标题，它的长度是31个字符，或者是注释中的字符数+ 1，哪个更小。
                title = text.substring(0, Math.min(30, length));
  
                // 如果产生的长度超过30个字符，则可以砍掉任何尾随空格
                if (length > 30) {
                    int lastSpace = title.lastIndexOf(' ');
                    if (lastSpace > 0) {
                        title = title.substring(0, lastSpace);
                    }
                }
            }
            // 在值映射中，设置标题的值
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        } else if (title != null) {
            // 在值映射中，设置标题的值
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        }

        // 这将所需的notes文本放入映射中。
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);

        /*
         * 用映射中的新值更新提供程序。ListView自动更新。提供者通过设置向传入URI的查询光标对象的
         * 通知URI来设置这个。因此，当URI更改时，将自动通知内容解析器，并更新UI。
         * 注意:这是在UI线程上完成的。它将阻塞线程，直到更新完成。在一个示例应用程序中，根据本地
         * 数据库对一个简单的提供者进行攻击，这个块将是短暂的，但是在一个真正的应用程序中，你应该
         * 使用android.content。AsyncQueryHandler或android.os.AsyncTask。
         */
        getContentResolver().update(
                mUri,    // 记录更新的URI。
                values,  // 列名称和新值的映射适用于它们。
                null,    //没有使用选择标准，因此不需要列。
                null     // 不使用列，所以不需要参数。
            );


    }

    /**
     * 这种辅助方法可以取消在注释上完成的工作。如果它是新创建的，或者返回到注释i的原始文本，它就会删除它
     */
    private final void cancelNote() {
        if (mCursor != null) {
            if (mState == STATE_EDIT) {
                // 将原始的注释文本返回到数据库中
                mCursor.close();
                mCursor = null;
                ContentValues values = new ContentValues();
                values.put(NotePad.Notes.COLUMN_NAME_NOTE, mOriginalContent);
                getContentResolver().update(mUri, values, null, null);
            } else if (mState == STATE_INSERT) {
                // 我们插入一个空音符，确保删除它
                deleteNote();
            }
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * 注意删除笔记。简单地删除条目。
     */
    private final void deleteNote() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            getContentResolver().delete(mUri, null, null);
            mText.setText("");
        }
    }
}
