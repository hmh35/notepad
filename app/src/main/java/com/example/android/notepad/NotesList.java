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
 * 应用程序入口，是一个ListActivity，以列表方式显示笔记本条目。
 */

package com.example.android.notepad;

import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * 显示注释列表。将显示在传入意图中提供的{ @ link Uri }的注释，否则它将默认显示
 * { @ link NotePadProvider }的内容。
 *
 * 注意:注意，此活动中的提供者操作在UI线程上进行。这不是一个好的做法。这只是为了让代码更容易阅读。
 * 一个真正的应用程序应该使用{ @ link android. content . content。AsyncQueryHandler }
 * 或{ @link android.os。AsyncTask }对象在单独的线程上异步执行操作。
 */
public class NotesList extends ListActivity {


    // 日志和调试
    private static final String TAG = "NotesList";

    /*private EditText searchText=(EditText) findViewById(R.id.et_Search);
    private ImageView searchNotes=(ImageView) findViewById(R.id.iv_searchnotes);
    private AutoCompleteTextView searchNotes=(AutoCompleteTextView)findViewById(R.id.searchNotes);*/

    /**
     * 游标适配器需要的列
     * 第一个PROJECTION字段指明了“日志列表“所关注的数据库中的字段（即只需要ID和Title就可以了）
     */
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,

    };

    private View view;
    private SimpleCursorAdapter adapter;
    private Cursor cursor;

    private SharedPreferences appbackground ;

    private void addSearchView() {
        //给listview添加头部(search)
        view =View.inflate(this, R.layout.notelistheader,null);

        getListView().addHeaderView(view);
        //给搜索框添加搜索功能
        final EditText et_Search=(EditText) view.findViewById(R.id.et_search);
        et_Search.addTextChangedListener(new TextWatcherForSearch(){
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                super.onTextChanged(charSequence, i, i1, i2);
                if (charSequence.length()!=0 && et_Search.getText().toString().length()!=0){
                    String str_Search = et_Search.getText().toString();
                    Cursor search_cursor = managedQuery(
                            getIntent().getData(),            // Use the default content URI for the provider.
                            PROJECTION,                       // Return the note ID and title for each note.
                            NotePad.Notes.COLUMN_NAME_TITLE+" like ?",                             // No where clause, return all records.
                            new String[]{"%"+str_Search+"%"}, //匹配字符串条件                            // No where clause, therefore no where column values.
                            NotePad.Notes.DEFAULT_SORT_ORDER  // Use the default sort order.
                    );
                    adapter.swapCursor(search_cursor);//刷新listview

                }else {
                    if (cursor!=null)//删除搜索框中的text后刷新listview
                        adapter.swapCursor(cursor);//刷新listview
                }
            }
        });
    }

    /** 标题栏的索引
     * title字段在数据表中的索引。
     * */
    private static final int COLUMN_INDEX_TITLE = 1;

    /**
     * 当Android从头开始这个活动时，就会调用onCreate。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.notelist);
        // 用户不需要按住键来使用菜单快捷键。
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);




        /* 如果没有在启动该活动的意图中给出数据，则该活动是在意图筛选器匹配一个主要操作时启动的。
        我们应该使用默认的提供者URI。
         */
        // 得到启动该活动的意图。
        Intent intent = getIntent();

        // 如果没有与intent相关联的数据，则将数据设置为默认URI，该URI访问一个notes列表。
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }


        /*
         * 为ListView设置上下文菜单激活的回调。侦听器将被设置为该活动。
         * 其效果是在ListView中为项目启用上下文菜单，而上下文菜单由NotesList中的方法处理。
         */
        getListView().setOnCreateContextMenuListener(this);

        /* 执行一个查询管理。活动处理在需要时关闭和请求游标。
         *
         *请参阅关于在UI线程上执行提供者操作的介绍性说明。
         */
        Cursor cursor = managedQuery(
            getIntent().getData(),            // 为提供者使用默认的内容URI，notes数据表。
            PROJECTION,                       // 返回每个note的note ID和标题。
            null,                             // 没有where子句，返回所有记录。
            null,                             // 没有where子句，因此没有列值。
            NotePad.Notes.DEFAULT_SORT_ORDER  // 使用默认的排序顺序，指明了结果的排序规则。
        );


        /*
         * 下面的两个数组在游标中的列和ListView中的项目的视图id之间创建一个“映射”。dataColumns数组
         * 中的每个元素都表示一个列名;viewID数组中的每个元素代表一个视图的ID。SimpleCursorAdapter将
         * 它们映射为升序，以确定ListView中每个列值的位置。
         */

        // 在视图中显示的游标列的名称，初始化为标题列
        String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE ,NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE} ;

        // 将显示游标列的视图id，初始化为noteslist_item. xml中的TextView
        //int[] viewIDs = { android.R.id.text1 };
        int[] viewIDs = { R.id.tv_title,R.id.tv_data };


        // 为ListView创建支持适配器。
        adapter
            = new SimpleCursorAdapter(
                      this,                             // ListView的上下文
                      R.layout.noteslistitem,          // 指向列表项的XML
                      cursor,                           // 用于获取项目的游标
                      dataColumns,
                      viewIDs
              );

        // 将ListView的适配器设置为刚刚创建的游标适配器.
        setListAdapter(adapter);
        addSearchView();
        setBackgroundMenu();

    }

    private void setBackgroundMenu() {
        //修改listview的背景
        //1.设置背景颜色数组
        final int[] colorArray={
                R.drawable.blue,
                R.drawable.gray,
                R.drawable.green,
                R.drawable.red,
                R.drawable.purple

        };
        appbackground = getSharedPreferences("noteapp", MODE_PRIVATE);
        NoteAttribute.snoteBackground=appbackground.getInt(NoteAttribute.NOTEBACKGROUND,colorArray[0]);//默认
        getListView().setBackgroundResource(NoteAttribute.snoteBackground);
        view.setBackgroundResource(NoteAttribute.headcolor);
        //修改头部背景
        final int[] coseArray={
                R.drawable.headblue,
                R.drawable.headgray,
                R.drawable.headgreen,
                R.drawable.headred,
                R.drawable.headpurple
        };
        NoteAttribute.headcolor=appbackground.getInt(NoteAttribute.HEADCOLOR,coseArray[0]);

//悬浮按钮

        final ImageView icon = new ImageView(this); // Create an icon
       // icon.setImageDrawable(getDrawable(R.drawable.color));
        FloatingActionButton actionButton = new FloatingActionButton.Builder(this)
                .setBackgroundDrawable(getDrawable(R.drawable.color))
                .setContentView(icon)//其实这个LayoutParams类是用于child view（子视图） 向 parent view（父视图）传达自己的意愿的一个东西（孩子想变成什么样向其父亲说明）
                .build();

//弹出按钮
// repeat many times:
        final int[] backdrawableArray={
                R.drawable.blue1,
                R.drawable.gray1,
                R.drawable.green1,
                R.drawable.red1,
                R.drawable.purple1};

        FloatingActionMenu.Builder menuBuilder = new FloatingActionMenu.Builder(this);
        SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);
        for(int i=0;i<backdrawableArray.length;i++){
            ImageView itemIcon = new ImageView(this);
            SubActionButton button = itemBuilder.setContentView(itemIcon)
                    .setBackgroundDrawable(getDrawable(backdrawableArray[i]))
                    .build();
            final int finalI = i;
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NoteAttribute.snoteBackground=colorArray[finalI];
                    NoteAttribute.headcolor=coseArray[finalI];
                    getListView().setBackgroundResource(NoteAttribute.snoteBackground);
                    //icon.setBackgroundDrawable(getDrawable(coseArray[finalI]));
                    //把背景颜色添加到SharedPreferences中
                    appbackground
                            .edit()
                            .putInt(NoteAttribute.NOTEBACKGROUND,colorArray[finalI])
                            .putInt(NoteAttribute.HEADCOLOR,coseArray[finalI])
                            .apply();
                    view.setBackgroundResource(NoteAttribute.headcolor);
                }
            });
            menuBuilder.addSubActionView(button);
        }
        //联系起来
        menuBuilder.attachTo(actionButton)
                .build();

    }



    /**
     * 当用户第一次单击该设备的菜单按钮时调用该活动。Android在一个包含项目的菜单对象中传递。
     *
     * 设置提供Insert选项的菜单，并为该活动提供其他操作的列表。其他想要处理notes的应用程序可以通过
     * 提供一个意图过滤器来“注册”自己在Android上，它包括类别选择和
     * mimeTYpe notepad. notes.content_type。如果他们这样做，onCreateOptionsMenu()的代码将添加包含
     * 意图过滤器的活动到它的选项列表中。实际上，菜单将为用户提供可以处理注释的其他应用程序。
     @ param菜单是一个菜单对象，菜单项应该被添加。
     @return真的,永远。应该显示菜单。
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 从XML资源中增加菜单
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        // 生成可以在整个列表中执行的任何其他操作。在正常的安装中，这里没有额外的操作，
        // 但是这允许其他应用程序用自己的操作扩展菜单。
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }




    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // 如果剪贴板上有数据，就启用粘贴菜单项。
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);


        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        // 如果剪贴板包含一个项目，则在菜单上启用粘贴选项。
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            // 如果剪贴板为空，则禁用菜单的粘贴选项。
            mPasteItem.setEnabled(false);
        }

        // 获取当前显示的notes的数量。
        final boolean haveItems = getListAdapter().getCount() > 0;

        // 如果列表中有任何注释(这意味着其中一个被选中)，那么我们需要生成可以在当前选择上执行的操作。
        // 这将是我们自己的具体操作和可以找到的扩展的组合。
        if (haveItems) {

            // 这是选中的项目。
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // 创建一个元素数组。这将用于根据所选的菜单项发送意图。
            Intent[] specifics = new Intent[1];

            //将数组的意图设置为在选中的注释的URI上进行编辑操作。
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            // 创建带有一个元素的菜单项的数组。这将包含编辑选项。
            MenuItem[] items = new MenuItem[1];

            // 使用选定的注释的URI创建一个没有特定操作的意图.
            Intent intent = new Intent(null, uri);

            /* 添加意图的类别替代，将注释ID URI作为其数据。这样就可以将意图作为在菜单中组合其他选项的地方。
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * 在菜单中添加其他选项
             */
            menu.addIntentOptions(
                Menu.CATEGORY_ALTERNATIVE,  // 将意图添加到替代组中。
                Menu.NONE,                  // 不需要一个唯一的项目ID。
                Menu.NONE,                  // 备选方案不需要按顺序排列。
                null,                       // 调用者的名字并没有被排除在组之外。
                specifics,                  // 这些特定选项必须首先出现。
                intent,                     // 这些意图对象映射到具体的选项。
                Menu.NONE,                  // 不需要标记。
                items                       // 由特定于意图映射生成的菜单项
            );
                // 如果编辑菜单项存在，则为它添加快捷方式。
                if (items[0] != null) {

                    // 将编辑菜单项的快捷方式设置为数值“1”，字母“e”
                    items[0].setShortcut('1', 'e');
                }
            } else {
                // 如果列表为空，则从菜单中删除任何现有的替代操作
                menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
            }

        // 显示菜单
        return true;
    }

    /**
     * 当用户从菜单中选择一个选项时，该方法被调用，但是列表中没有选择的项。如果该选项是INSERT，
     * 那么将使用action ACTION_INSERT发送一个新的意图。来自传入意图的数据被放入新的意图中。
     * 实际上，这会触发NotePad应用程序中的NoteEditor活动。
     *
     * 如果该项目没有插入，那么很可能它是另一个应用程序的替代选项。调用父方法来处理项目。
     @ param项目是由用户选择的菜单项
     如果插入菜单项被选中，则@ return True;否则，调用父方法的结果。
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_add:
          /*
           * 使用意图启动一个新的活动。活动的意图过滤器必须具有action ACTION_INSERT。不设置类别，
           * 因此假定默认。实际上，这将启动NotePad中的NoteEditor活动。
           */
           startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
           return true;
        case R.id.menu_paste:
          /*
           * 使用意图启动一个新的活动。活动的意图过滤器必须具有action action_粘贴。
           * 不设置类别，因此假定默认。实际上，这将启动NotePad中的NoteEditor活动。
           */
          startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
          return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * 当用户上下文单击列表中的注释时调用此方法。NotesList在其ListView中作为上下文菜单的处理程序
     * (在onCreate()中完成)。
     *
     * 唯一可用的选项是复制和删除。
     *
     * 上下文单击相当于长时间。
     *
     * @ param菜单的上下文菜单对象应该添加哪些项目。
     @ param查看正在构造的上下文菜单的视图。
     @ param menuInfo数据与视图关联。
     @throws ClassCastException
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // 来自菜单项的数据。
        AdapterView.AdapterContextMenuInfo info;

        // 试图在列表视图中获取被长期压缩的项目的位置。
        try {
            // 将传入的数据对象转换为适合于AdapterView对象的类型。
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // 如果菜单对象不能被浇铸，则记录错误。
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        /*
         * 获取与所选位置的项目相关的数据。getItem()返回ListView与该项目关联的任何支持适配器。
         * 在NotesList中，适配器将一个注释的所有数据与它的列表项关联起来。作为一个结果,
            getItem()将数据作为游标返回。
         */
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position-1);

        // 如果游标是空的，那么由于某些原因，适配器无法从提供者处获得数据，因此返回null给调用者。
        if (cursor == null) {
            // 由于某些原因，请求的项不可用，什么都不做
            return;
        }

        //从XML资源中增加菜单
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        // 将菜单头设置为选中的音符的标题。
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // 将菜单项附加到任何其他可以使用它的活动。对于任何实现我们的数据的替代操作的活动，
        // 这将对系统进行查询，为所找到的每个活动添加一个菜单项。
        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(), 
                                        Integer.toString((int) info.id) ));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    /**
     * 当用户从上下文菜单中选择一个项目时调用这个方法(参见onCreateContextMenu())。
     * 实际上处理的唯一菜单项是DELETE和COPY。其他任何东西都是另一种选择，默认处理应该是这样。
     *
     *@ param项目选中的菜单项
     * @返回True，如果菜单项是删除的，并且没有默认处理是需要的，否则是假的，
     *触发该项目的默认处理。
     * @throws ClassCastException
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // 来自菜单项的数据。
        AdapterView.AdapterContextMenuInfo info;

        /*
         * 从菜单项获取额外信息。当Notes列表中的注释被长期压缩时，将出现上下文菜单。菜单上的菜单项
         * 会自动获取与被长期压缩的注释相关联的数据。数据来自支持列表的提供者。
         *
         * 在ContextMenuInfo对象中，该通知的数据传递到上下文菜单创建例程。
         *
         * 当单击其中一个上下文菜单项时，将通过项目参数传递相同的数据，以及注释ID，以对上下文进行选择()。
         */
        try {
            // 将项目中的数据对象转换为适合于AdapterView对象的类型。
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            // 如果对象不能被转换，则记录错误
            Log.e(TAG, "bad menuInfo", e);

            // 触发菜单项的默认处理。
            return false;
        }
        //将所选的注释的ID附加到带有传入意图的URI。
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        /*
         *获取菜单项的ID并将其与已知的操作进行比较。
         */
        switch (item.getItemId()) {
        case R.id.context_open:
            // 启动活动来查看/编辑当前选中的项目
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
            return true;
//BEGIN_INCLUDE(copy)
        case R.id.context_copy:
            // 获取剪贴板服务的句柄。
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
  
            //将notes URI复制到剪贴板。实际上，它本身就会复制
            clipboard.setPrimaryClip(ClipData.newUri(   //保存URI的新剪贴板项
                    getContentResolver(),               // 解析器检索URI信息
                    "Note",                             // 标签的剪辑
                    noteUri)                            // the URI
            );
  
            // 返回调用者并跳过进一步处理。
            return true;
//END_INCLUDE(copy)
        case R.id.context_delete:
  
            // 通过在注释ID格式中传递URI来删除提供者的通知。请参阅关于在UI线程上执行提供者操作的
            // 介绍性说明。
            getContentResolver().delete(
                noteUri,  // 提供者的URI
                null,     // 不需要任何where子句，因为只传入一个单一的注释ID。
                null      // 不使用where子句，所以不需要参数。
            );
  
            //返回调用者并跳过进一步处理。
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    /**
     * 当用户在显示的列表中单击一个注释时调用此方法。
     *
     * 此方法处理选择(从提供者获取数据)或GET_CONTENT(获取或创建数据)的传入操作。如果传入的操作
     * 是编辑，此方法将发送一个新的意图来启动NoteEditor。
     * @ param l的ListView包含单击的项
     * @ param v对单个项目的视图
     @ param位置v在显示列表中的位置
     * @ param id为单击项的行id
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        //从传入URI和行ID构造一个新URI
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // 从传入意图获取操作
        String action = getIntent().getAction();

        // 处理要注意的数据的请求
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // 将结果设置为返回调用该活动的组件。结果包含新URI
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            // 发送一个意图来启动一个可以处理ACTION_EDIT的活动。意图的数据是注释ID URI。其作用是调用NoteEdit。
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}
