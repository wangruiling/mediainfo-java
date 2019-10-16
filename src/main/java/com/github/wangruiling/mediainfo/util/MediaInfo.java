package com.github.wangruiling.mediainfo.util;
/*  Copyright (c) MediaArea.net SARL. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license that can
 *  be found in the License.html file in the root of the source tree.
 */

// Note: the original stuff was well packaged with Java style,
// but I (the main developer) prefer to keep an easiest for me
// way to have all sources and example in the same place
// Removed stuff:
// "package net.sourceforge.mediainfo;"
// directory was /net/sourceforge/mediainfo
import com.sun.jna.*;
import java.io.Closeable;

import static java.util.Collections.singletonMap;

public class MediaInfo implements Closeable {

    static String LibraryPath = "mediainfo";
    static String LibraryPathLinux = "libmediainfo.so.0";

    static {

        if (Platform.isLinux()) {
            try {
                //方法一、不需要在系统中安装软件：mediainfo、libmediainfo、libzen，但是需要libmediainfo.so.0文件
                LibraryPath = LibraryPathLinux;
                NativeLibrary.getInstance(LibraryPath);


                //方法二、需要在系统中安装软件：mediainfo、libmediainfo、libzen，但是不需要libmediainfo.so.0文件
                // libmediainfo for linux depends on libzen
                // We need to load dependencies first, because we know where our native libs are (e.g. Java Web Start Cache).
                // If we do not, the system will look for dependencies, but only in the library path.
                //NativeLibrary.getInstance("zen");
            } catch (LinkageError e) {
                e.printStackTrace();
                //logger.warn("Failed to preload libzen");
            }
        }
    }

    /**
     * 功能描述: <br>
     * MediaInfoDLL_Internal
     * @author: wangrl
     * @create: 2019-3-13 16:14
     */
    interface MediaInfoLibrary extends Library {
        MediaInfoLibrary INSTANCE = Native.loadLibrary(LibraryPath, MediaInfoLibrary.class, singletonMap(OPTION_FUNCTION_MAPPER,
                (FunctionMapper) (lib, method) -> "MediaInfo_" + method.getName()
        ));


        //Constructor/Destructor
        Pointer New();

        void Delete(Pointer Handle);

        //File
        int Open(Pointer Handle, WString file);

        int Open_Buffer_Init(Pointer handle, long length, long offset);

        int Open_Buffer_Continue(Pointer handle, byte[] buffer, int size);

        long Open_Buffer_Continue_GoTo_Get(Pointer handle);

        int Open_Buffer_Finalize(Pointer handle);

        void Close(Pointer Handle);

        //Infos
        WString Inform(Pointer Handle, int Reserved);

        WString Get(Pointer Handle, int StreamKind, int StreamNumber, WString parameter, int infoKind, int searchKind);

        WString GetI(Pointer Handle, int StreamKind, int StreamNumber, int parameterIndex, int infoKind);

        int Count_Get(Pointer Handle, int StreamKind, int StreamNumber);

        //Options
        WString Option(Pointer Handle, WString option, WString value);
    }

    private Pointer handle;

    public enum StreamKind {
        General,
        Video,
        Audio,
        Text,
        Other,
        Image,
        Menu;
    }

    //Enums
    public enum InfoKind {
        /**
         * Unique name of parameter.
         */
        Name,

        /**
         * Value of parameter.
         */
        Text,

        /**
         * Unique name of measure unit of parameter.
         */
        Measure,

        Options,

        /**
         * Translated name of parameter.
         */
        Name_Text,

        /**
         * Translated name of measure unit.
         */
        Measure_Text,

        /**
         * More information about the parameter.
         */
        Info,

        /**
         * How this parameter is supported, could be N (No), B (Beta), R (Read only), W
         * (Read/Write).
         */
        HowTo,

        /**
         * Domain of this piece of information.
         */
        Domain;
    }

    public enum Status {
        None(0x00),
        Accepted(0x01),
        Filled(0x02),
        Updated(0x04),
        Finalized(0x08);

        private int value;

        private Status(int value) {
            this.value = value;
        }

        public int getValue(int value) {
            return value;
        }
    }

    //Constructor/Destructor
    public MediaInfo() {
        handle = MediaInfoLibrary.INSTANCE.New();
    }

    public void dispose() {
        if (handle == null) {
            throw new IllegalStateException();
        }

        MediaInfoLibrary.INSTANCE.Delete(handle);
        handle = null;
    }

    @Override
    protected void finalize() throws Throwable {
        if (handle != null) {
            dispose();
        }
    }

    //File

    /**
     * Open a file and collect information about it (technical information and tags).
     *
     * @param file full name of the file to open
     * @return 1 if file was opened, 0 if file was not not opened
     */
    public int open(String file) {
        return MediaInfoLibrary.INSTANCE.Open(handle, new WString(file));
    }

    public int openBufferInit(long length, long offset) {
        return MediaInfoLibrary.INSTANCE.Open_Buffer_Init(handle, length, offset);
    }

    /**
     * Open a stream and collect information about it (technical information and tags) (By buffer, Continue)
     *
     * @param buffer pointer to the stream
     * @param size   Count of bytes to read
     * @return a bitfield
     * bit 0: Is Accepted (format is known)
     * bit 1: Is Filled (main data is collected)
     * bit 2: Is Updated (some data have beed updated, example: duration for a real time MPEG-TS stream)
     * bit 3: Is Finalized (No more data is needed, will not use further data)
     * bit 4-15: Reserved
     * bit 16-31: User defined
     */
    public int openBufferContinue(byte[] buffer, int size) {
        return MediaInfoLibrary.INSTANCE.Open_Buffer_Continue(handle, buffer, size);
    }

    public long openBufferContinueGotoGet() {
        return MediaInfoLibrary.INSTANCE.Open_Buffer_Continue_GoTo_Get(handle);
    }

    public int openBufferFinalize() {
        return MediaInfoLibrary.INSTANCE.Open_Buffer_Finalize(handle);
    }

    /**
     * Close a file opened before with Open().
     */
    @Override
    public void close() {
        MediaInfoLibrary.INSTANCE.Close(handle);
    }

    //Information

    /**
     * get all details about a file.
     *
     * @return All details about a file in one string
     */
    public String inform() {
        return MediaInfoLibrary.INSTANCE.Inform(handle, 0).toString();
    }

    /**
     * get a piece of information about a file (parameter is a string).
     *
     * @param streamKind   Kind of Stream (general, video, audio...)
     * @param streamNumber Stream number in Kind of Stream (first, second...)
     * @param parameter    Parameter you are looking for in the Stream (Codec, width, bitrate...),
     *                     in string format ("Codec", "Width"...)
     * @return a string about information you search, an empty string if there is a problem
     */
    public String get(StreamKind streamKind, int streamNumber, String parameter) {
        return get(streamKind, streamNumber, parameter, InfoKind.Text, InfoKind.Name);
    }


    /**
     * get a piece of information about a file (parameter is a string).
     *
     * @param streamKind   Kind of Stream (general, video, audio...)
     * @param streamNumber Stream number in Kind of Stream (first, second...)
     * @param parameter    Parameter you are looking for in the Stream (Codec, width, bitrate...),
     *                     in string format ("Codec", "Width"...)
     * @param infoKind     Kind of information you want about the parameter (the text, the measure,
     *                     the help...)
     //* @param searchKind   Where to look for the parameter
     */
    public String get(StreamKind streamKind, int streamNumber, String parameter, InfoKind infoKind) {
        return get(streamKind, streamNumber, parameter, infoKind, InfoKind.Name);
    }


    /**
     * get a piece of information about a file (parameter is a string).
     *
     * @param streamKind   Kind of Stream (general, video, audio...)
     * @param streamNumber Stream number in Kind of Stream (first, second...)
     * @param parameter    Parameter you are looking for in the Stream (Codec, width, bitrate...),
     *                     in string format ("Codec", "Width"...)
     * @param infoKind     Kind of information you want about the parameter (the text, the measure,
     *                     the help...)
     * @param searchKind   Where to look for the parameter
     * @return a string about information you search, an empty string if there is a problem
     */
    public String get(StreamKind streamKind, int streamNumber, String parameter, InfoKind infoKind, InfoKind searchKind) {
        return MediaInfoLibrary.INSTANCE.Get(handle, streamKind.ordinal(), streamNumber, new WString(parameter), infoKind.ordinal(), searchKind.ordinal()).toString();
    }


    /**
     * get a piece of information about a file (parameter is an integer).
     *
     * @param streamKind   Kind of Stream (general, video, audio...)
     * @param streamNumber Stream number in Kind of Stream (first, second...)
     * @param parameterIndex    Parameter you are looking for in the Stream (Codec, width, bitrate...),
     *                     in integer format (first parameter, second parameter...)
     * @return a string about information you search, an empty string if there is a problem
     */
    public String get(StreamKind streamKind, int streamNumber, int parameterIndex) {
        return get(streamKind, streamNumber, parameterIndex, InfoKind.Text);
    }


    /**
     * get a piece of information about a file (parameter is an integer).
     *
     * @param streamKind   Kind of Stream (general, video, audio...)
     * @param streamNumber Stream number in Kind of Stream (first, second...)
     * @param parameterIndex    Parameter you are looking for in the Stream (Codec, width, bitrate...),
     *                     in integer format (first parameter, second parameter...)
     * @param infoKind     Kind of information you want about the parameter (the text, the measure,
     *                     the help...)
     * @return a string about information you search, an empty string if there is a problem
     */
    public String get(StreamKind streamKind, int streamNumber, int parameterIndex, InfoKind infoKind) {
        return MediaInfoLibrary.INSTANCE.GetI(handle, streamKind.ordinal(), streamNumber, parameterIndex, infoKind.ordinal()).toString();
    }

    /**
     * Count of Streams of a Stream kind (StreamNumber not filled), or count of piece of
     * information in this Stream.
     *
     * @param streamKind Kind of Stream (general, video, audio...)
     * @return number of Streams of the given Stream kind
     */
    public int countGet(StreamKind streamKind) {
        //We should use NativeLong for -1, but it fails on 64-bit
        //int countGet(Pointer handle, int StreamKind, NativeLong StreamNumber);
        //return MediaInfoDLL_Internal.INSTANCE.countGet(handle, StreamKind.ordinal(), -1);
        //so we use slower get() with a character string
        String StreamCount = get(streamKind, 0, "StreamCount");
        if (StreamCount == null || StreamCount.length() == 0)
            return 0;
        return Integer.parseInt(StreamCount);
    }


    /**
     * Count of Streams of a Stream kind (StreamNumber not filled), or count of piece of
     * information in this Stream.
     *
     * @param streamKind   Kind of Stream (general, video, audio...)
     * @param streamNumber Stream number in this kind of Stream (first, second...)
     * @return number of Streams of the given Stream kind
     */
    public int countGet(StreamKind streamKind, int streamNumber) {
        return MediaInfoLibrary.INSTANCE.Count_Get(handle, streamKind.ordinal(), streamNumber);
    }


    //Options

    /**
     * Configure or get information about MediaInfo.
     *
     * @param option The name of option
     * @return Depends on the option: by default "" (nothing) means No, other means Yes
     */
    public String option(String option) {
        return MediaInfoLibrary.INSTANCE.Option(handle, new WString(option), new WString("")).toString();
    }

    /**
     * Configure or get information about MediaInfo.
     *
     * @param option The name of option
     * @param value  The value of option
     * @return Depends on the option: by default "" (nothing) means No, other means Yes
     */
    public String option(String option, String value) {
        return MediaInfoLibrary.INSTANCE.Option(handle, new WString(option), new WString(value)).toString();
    }

    /**
     * Configure or get information about MediaInfo (Static version).
     *
     * @param option The name of option
     * @return Depends on the option: by default "" (nothing) means No, other means Yes
     */
    public static String optionStatic(String option) {
        return MediaInfoLibrary.INSTANCE.Option(MediaInfoLibrary.INSTANCE.New(), new WString(option), new WString("")).toString();
    }

    /**
     * Configure or get information about MediaInfo(Static version).
     *
     * @param option The name of option
     * @param value  The value of option
     * @return Depends on the option: by default "" (nothing) means No, other means Yes
     */
    public static String optionStatic(String option, String value) {
        return MediaInfoLibrary.INSTANCE.Option(MediaInfoLibrary.INSTANCE.New(), new WString(option), new WString(value)).toString();
    }
}
