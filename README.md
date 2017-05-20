# NotePad
## NotePad的拓展<br>
  *添加查询功能（根据标题）<br>
  *NoteList中显示条目增加时间戳显示<br>
  *更改记事本的背景<br>
  *UI美化<br>
## 添加的代码<br>
  ### *查询功能：添加了addSearchView函数<br>
  ```java
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
  ```
  ### *时间戳显示：在NoteEditor中的updateNote函数中获取到当前时间，再添加到values当中以在显示条目当中显示，最终显示的是最后修改时间  <br>
     1)新建一个notelistitem.xml,修改item，增加显示时间戳的textview
 ```java
  <?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical" android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:id="@+id/ll_notesItems"
    android:background="#ffffff"
    android:layout_marginBottom="5dp"
    >

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:background="#707070"
        android:layout_marginTop="10dp"
        >

    </LinearLayout>
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        >
        <ImageView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="@drawable/app_notes"
            android:layout_marginTop="5dp"
            />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:orientation="vertical"
            >
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Title"
                android:textColor="#000000"
                android:textSize="20sp"
                android:layout_marginTop="5dp"
                android:layout_marginLeft="10dp"
                android:id="@+id/tv_title"
                />
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="2017/4/25 16:25:30"
                android:layout_marginTop="5dp"
                android:layout_marginLeft="10dp"
                android:id="@+id/tv_data"
                android:textColor="#000000"
                />
        </LinearLayout>

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="10dp"
            >


        </LinearLayout>

    </LinearLayout>
</LinearLayout>
  ```
  2)进入notelist.java PROJECTION 契约类的变量值加一列。
  ```java
   private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
    };
  ```
  3)对NoteEditor.java中的updateNote尽兴修改，获取当前时间戳以及将时间戳转化格式
  ```
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
  ```
  3)修改SimpleCursorAdapter的dataColumns和viewIDs的相关值。
  ```java
   String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE ,NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE} ;
        int[] viewIDs = { R.id.tv_title,R.id.tv_data };
  ```
  ### *更改记事本的背景：<br>
   添加浮动按钮来设置当前的背景颜色，所使用的背景渐变颜色来自于自己写的XML文件<br>   
   添加setBackgroundMenu函数<br>
   ```
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
   ```
## 功能演示<br>
  * notepad主界面：设置了五种背景颜色，这里简单给出三种,右下角的是浮动按钮，点击浮动按钮，便会弹出背景颜色以供选择<br>
  ![](https://github.com/hmh35/notepad/blob/master/screen/mainscreen.png)
  ![](https://github.com/hmh35/notepad/blob/master/screen/mainscreen2.png) 
  ![](https://github.com/hmh35/notepad/blob/master/screen/mainscreen3.png)
  * 编辑内容：<br> 
  ![](https://github.com/hmh35/notepad/blob/master/screen/edit.png)
  * 根据标题查询:<br>
  ![](https://github.com/hmh35/notepad/blob/master/screen/search.png)
  ![](https://github.com/hmh35/notepad/blob/master/screen/search.png)
  * 长按item后可进行打开、复制、删除以及编辑标题的操作<br>
  ![](https://github.com/hmh35/notepad/blob/master/screen/edit2.png)
