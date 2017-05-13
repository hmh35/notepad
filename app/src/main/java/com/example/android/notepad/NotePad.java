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
 * NotePad是数据契约类，用来提供一种统一的数据访问格式
 */

package com.example.android.notepad;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * 定义一个介于Note Pad内容提供商和它的客户之间的契约。契约定义客户端需要访问提供者作为一个或多个
 * 数据表的信息。契约是一个公共的、不可扩展的(final)类，它包含定义列名称和uri的常量。一个写得好的
 * 客户只依赖于合同中的常数。
 */
public final class NotePad {
    public static final String AUTHORITY = "com.google.provider.NotePad";

    // 这个类不能实例化
    private NotePad() {
    }

    /**
     *指出合同表
     */
    public static final class Notes implements BaseColumns {

        // 这个类不能实例化
        private Notes() {}

        /**
         * 此提供者提供的表名
         */
        public static final String TABLE_NAME = "notes";

        /*
         *URI定义
         */

        /**
         * 此提供者的URI的scheme部分
         */
        private static final String SCHEME = "content://";

        /**
         *uri的路径部分
         */

        /**
         *路径部分为Notes URI
         */
        private static final String PATH_NOTES = "/notes";

        /**
         * 路径部分，用于注意ID URI
         */
        private static final String PATH_NOTE_ID = "/notes/";

        /**
         * 在一个注释ID URI的路径部分的一个注释ID段的0相对位置
         */
        public static final int NOTE_ID_PATH_POSITION = 1;

        /**
         * 路径部分用于Live文件夹URI
         */
        private static final String PATH_LIVE_FOLDER = "/live_folders/notes";

        /**
         * 此表的内容:/ /样式URL
         */
        public static final Uri CONTENT_URI =  Uri.parse(SCHEME + AUTHORITY + PATH_NOTES);

        /**
         * 一个单音符的内容URI基础。调用者必须在这个Uri上附加一个数字注释id来检索注释
         */
        public static final Uri CONTENT_ID_URI_BASE
            = Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID);

        /**
         * 内容URI匹配模式，由其ID指定。使用此模式匹配传入URI或构造意图。
         */
        public static final Uri CONTENT_ID_URI_PATTERN
            = Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID + "/#");

        /**
         * 用于实时文件夹的notes列表的内容Uri模式
         */
        public static final Uri LIVE_FOLDER_URI
            = Uri.parse(SCHEME + AUTHORITY + PATH_LIVE_FOLDER);

        /*
         *MIME类型定义
         */

        /**
         * { @ link # CONTENT_URI }的MIME类型提供了一个notes目录。
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.note";

        /**
         *一个单音调的{ @ link # CONTENT_URI }子目录的MIME类型。
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.note";

        /**
         * 这个表的默认排序顺序
         */
        public static final String DEFAULT_SORT_ORDER = "modified DESC";

        /*
         *列定义
         */

        /**
         * 注意事项名称的列名
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_TITLE = "title";

        /**
         * 注释内容的列名
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_NOTE = "note";

        /**
         * 创建时间戳的列名
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String COLUMN_NAME_CREATE_DATE = "created";

        /**
         * 修改时间戳的列名
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String COLUMN_NAME_MODIFICATION_DATE = "modified";

        public static final String COLUMN_NAME_EDITNOTE="editNote";
    }
}
