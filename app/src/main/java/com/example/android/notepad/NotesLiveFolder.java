/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.example.android.notepad.NotePad;

import android.app.Activity;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;
import android.provider.LiveFolders;

/**
 * 此活动将创建一个活动文件夹，并将其发送回HOME。从意图中的数据中，HOME创建一个live文件夹并在HOME
 * 视图中显示它的图标。当用户单击该图标时，Home使用来自于从一个内容提供方获取信息的意图所获得的数
 * 据，并在视图中显示它。
 *
 * 此活动的意图过滤器设置为action_create_live_文件夹，该文件夹将响应一个长时间的媒体和选择的Live文件夹。
 */
public class NotesLiveFolder extends Activity {

    /**
     * 所有的工作都是在onCreate()中完成的。活动实际上并没有显示UI。相反，它设置了一个意图并将
     * 其返回给它的调用者(家庭活动)。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * 获取传入的意图及其操作。如果传入意图是action_create_live_文件夹，然后创建一个带有
         * 所需数据的传出意图，然后发送OK。否则,返回取消。
         */
        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (LiveFolders.ACTION_CREATE_LIVE_FOLDER.equals(action)) {

            // 创建一个新Intent
            final Intent liveFolderIntent = new Intent();

            /*
             * 下面的语句将数据放入传出的意图中。请参阅{ @link android.provider。
             * live文件夹对这些数据值进行详细描述。从这个数据中，HOME设置了一个活文件夹。
             */
            // 为支持文件夹的内容提供程序设置URI模式。
            liveFolderIntent.setData(NotePad.Notes.LIVE_FOLDER_URI);

            // 添加live文件夹的显示名称作为额外的字符串。
            String foldername = getString(R.string.live_folder_name);
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME, foldername);

            //添加live文件夹的显示图标作为额外的资源。
            ShortcutIconResource foldericon =
                Intent.ShortcutIconResource.fromContext(this, R.drawable.live_folder_notes);
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON, foldericon);

            //将live文件夹的显示模式添加为整数。指定的模式使live文件夹显示为列表。
            liveFolderIntent.putExtra(
                    LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE,
                    LiveFolders.DISPLAY_MODE_LIST);

            /*
             * 为活动文件夹列表中的项目添加一个基本动作，作为一种意图。当用户在列表中单击单个
             * 的注释时，live文件夹就会触发这个意图。
             *
             * 它的操作是ACTION_EDIT，因此它触发注释编辑器活动。它的数据是由它的ID标识的单个注释
             * 的URI模式。live文件夹自动将所选项目的ID值添加到URI模式。
             *
             * 结果，注释编辑器被触发，并得到一个由ID检索的单音符。
             */
            Intent returnIntent
                    = new Intent(Intent.ACTION_EDIT, NotePad.Notes.CONTENT_ID_URI_PATTERN);
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT, returnIntent);

            /* 创建一个ActivityResult对象来传播回HOME。将其结果指示器设置为OK，并将返回的意图
            设置为刚刚构建的live文件夹意图。
             */
            setResult(RESULT_OK, liveFolderIntent);

        } else {

            // 如果最初的操作不是ACTION_CREATE_LIVE_FOLDER，则会创建一个ActivityResult，指示符将
            // 被取消，但不返回一个意图
            setResult(RESULT_CANCELED);
        }

        //关闭活动。ActivityObject被传播回调用方。
        finish();
    }
}
