# NotePad
This is an AndroidStudio rebuild of google SDK sample NotePad

1、NotePad的拓展
   a、添加查询功能（根据标题）
   
   b、NoteList中显示条目增加时间戳显示
   
   c、更改记事本的背景
   
   d、UI美化
   
2、添加的代码

   a、查询功能：添加了addSearchView函数
   
   b、时间戳显示：在NoteEditor中的updateNote函数中获取到当前时间，再添加到values当中以在显示条目当中显示，
   
      最终显示的是最后修改时间
      
   c、更改记事本的背景：
   
      添加浮动按钮来设置当前的背景颜色，所使用的背景渐变颜色来自于自己写的XML文件
      
3、功能演示

  a）notepad主界面：设置了五种背景颜色，这里简单给出三种,右下角的是浮动按钮，点击浮动按钮，便会弹出背景颜色以供选择
  
  ![](https://github.com/hmh35/notepad/blob/master/screen/mainscreen.png)
  ![](https://github.com/hmh35/notepad/blob/master/screen/mainscreen2.png) 
  ![](https://github.com/hmh35/notepad/blob/master/screen/mainscreen3.png)
  
  b)编辑内容：
  
  ![](https://github.com/hmh35/notepad/blob/master/screen/edit.png);
  
  c)根据标题查询:
  
  ![](https://github.com/hmh35/notepad/blob/master/screen/search.png);
  ![](https://github.com/hmh35/notepad/blob/master/screen/search.png);
  
  d)长按item后可进行打开、复制、删除以及编辑标题的操作
  
  ![](https://github.com/hmh35/notepad/blob/master/screen/edit2.png);
