
package com.example.legendutils.Tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore.Video.Thumbnails;
import android.widget.Toast;

/**
 * Thread-safe? 复制、移动、删除、压缩、解压、抽取缩略图、取得文件类型、文件/文件夹大小计算、文件夹子文件数量计算、
 * 
 * @author NashLegend
 */
@SuppressLint("DefaultLocale")
public class FileUtil {

    /**
     * 普通文件
     */
    public static final int FILE_TYPE_NORMAL = 0;
    /**
     * 文件夹
     */
    public static final int FILE_TYPE_FOLDER = 1;
    /**
     * 声音类型的文件
     */
    public static final int FILE_TYPE_AUDIO = 2;
    /**
     * 图像类型的文件
     */
    public static final int FILE_TYPE_IMAGE = 3;
    /**
     * 视频类型的文件
     */
    public static final int FILE_TYPE_VIDEO = 4;
    /**
     * APK文件
     */
    public static final int FILE_TYPE_APK = 5;
    /**
     * TXT文件
     */
    public static final int FILE_TYPE_TXT = 6;
    /**
     * ZIP文件
     */
    public static final int FILE_TYPE_ZIP = 7;
    /**
     * HTML文件
     */
    public static final int FILE_TYPE_HTML = 8;
    /**
     * WORD文件
     */
    public static final int FILE_TYPE_WORD = 9;
    /**
     * EXCEL文件
     */
    public static final int FILE_TYPE_EXCEL = 10;
    /**
     * PPT文件
     */
    public static final int FILE_TYPE_PPT = 11;
    /**
     * PDF文件
     */
    public static final int FILE_TYPE_PDF = 12;
    /**
     * 电子书文件
     */
    public static final int FILE_TYPE_EBOOK = 13;
    /**
     * 种子文件
     */
    public static final int FILE_TYPE_TORRENT = 14;
    /**
     * CHM文件
     */
    public static final int FILE_TYPE_CHM = 15;

    public static final String[] soundSuffixArray = {
            "mp3", "wav", "ogg", "midi"
    };
    public static final String[] imageSuffixArray = {
            "jpg", "jpeg", "png", "bmp", "gif"
    };
    public static final String[] videoSuffixArray = {
            "mp4", "avi", "rmvb", "flv", "mkv", "wmv",
    };
    public static final String[] apkSuffixArray = {
            "apk"
    };
    public static final String[] txtSuffixArray = {
            "txt", "xml", "java", "c", "cpp", "py", "log", "cs", "json"
    };
    public static final String[] zipSuffixArray = {
            "zip", "rar", "gz", "7z", "jar", "img", "tar"
    };
    public static final String[] wordSuffixArray = {
            "doc", "docx"
    };
    public static final String[] pptSuffixArray = {
            "ppt", "pptx"
    };
    public static final String[] excelSuffixArray = {
            "xsl", "xslx"
    };
    public static final String[] htmlSuffixArray = {
            "html", "htm", "jsp", "asp", "php"
    };
    public static final String[] pdfSuffixArray = {
            "pdf"
    };
    public static final String[] torrentSuffixArray = {
            "torrent"
    };
    public static final String[] chmSuffixArray = {
            "chm"
    };
    public static final String[] ebookSuffixArray = {
            "epub", "caj", "ebk2", "ebk3", "umd"
    };

    // 写文件模式,可组合使用，sourceFile和destFile都是文件夹的情况三种模式都不会删除文件夹
    public static final int Operation_Ski_All = 0x0;// 重名直接跳过
    public static final int Operation_Merge = 0x1;// 只合并文件夹并不替换文件，意味着不删除任何文件
    public static final int Operation_Merge_And_Overwrite = 0x2;// 合并文件夹并替换文件

    interface FileOperationListener {

        public void onComplete();

        public void onProgress();

        public void onError();

    }

    private static final int BUFFER = 8192;

    /**
     * @param sourceFile
     * @param destFile
     */
    public static void zip(File sourceFile, File destFile) {
        if (!sourceFile.exists())
            throw new NullPointerException("sourceFile not exist");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(destFile);
            CheckedOutputStream cos = new CheckedOutputStream(fileOutputStream,
                    new CRC32());
            ZipOutputStream out = new ZipOutputStream(cos);
            String basedir = "";
            zip(sourceFile, out, basedir);
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param sourceFile
     * @param destFile
     */
    public static void zip(File[] sourceFile, File destFile) {
        for (int i = 0; i < sourceFile.length; i++) {
            File file = sourceFile[i];
            if (!file.exists())
                throw new NullPointerException(
                        "one or more sourceFiles not exist");
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(destFile);
            CheckedOutputStream cos = new CheckedOutputStream(fileOutputStream,
                    new CRC32());
            ZipOutputStream out = new ZipOutputStream(cos);
            for (int i = 0; i < sourceFile.length; i++) {
                File file = sourceFile[i];
                zip(file, out, "");
            }
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param sourceFile
     * @param destFile
     * @throws Exception
     */
    public static void unZip(File sourceFile, File destFile) throws Exception {
        if (!sourceFile.exists()) {
            throw new NullPointerException("sourceFile not exist");
        }
        ZipFile zipFile = new ZipFile(sourceFile);
        Enumeration<?> e = zipFile.getEntries();
        while (e.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) e.nextElement();
            if (zipEntry.isDirectory()) {
                String name = zipEntry.getName();
                name = name.substring(0, name.length() - 1);
                File f = new File(destFile, name);
                f.mkdirs();
            } else {
                File f = new File(destFile, zipEntry.getName());
                f.getParentFile().mkdirs();
                f.createNewFile();
                InputStream is = zipFile.getInputStream(zipEntry);
                FileOutputStream fos = new FileOutputStream(f);
                int length = 0;
                byte[] b = new byte[BUFFER];
                while ((length = is.read(b, 0, BUFFER)) != -1) {
                    fos.write(b, 0, length);
                }
                is.close();
                fos.close();
            }
        }

        if (zipFile != null) {
            zipFile.close();
        }
    }

    /**
     * @param file
     * @return
     */
    public static int getNumFilesInFolder(File file) {
        if (file == null) {
            throw new NullPointerException("file cannot be null");
        }
        if (!file.exists()) {
            throw new NullPointerException("file does not exist");
        }
        if (file.isFile()) {
            throw new ClassCastException("file is not a directory");
        }
        return getSubfilesNumberInFolder(file, false, true);
    }

    /**
     * @param file
     * @param includeHiddleFiles
     * @param includeFolder
     * @return
     */
    public static int getNumFilesInFolder(File file,
            boolean includeHiddleFiles, boolean includeFolder) {
        if (file == null) {
            throw new NullPointerException("file cannot be null");
        }
        if (!file.exists()) {
            throw new NullPointerException("file does not exist");
        }
        if (file.isFile()) {
            throw new ClassCastException("file is not a directory");
        }
        return getSubfilesNumberInFolder(file, includeHiddleFiles,
                includeFolder);
    }

    public static long getFileSize(File file) {
        long size = 0L;
        if (file != null && file.exists()) {
            File[] files = {
                    file
            };
            return getFileSize(files);
        }
        return size;
    }

    public static long getFileSize(File[] files) {
        if (files == null) {
            throw new NullPointerException("files cannot be null");
        }
        long size = 0L;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file != null && file.exists()) {
                if (file.isDirectory()) {
                    size += getSilgleFolderSize(file);
                } else {
                    size += file.length();
                }
            }
        }

        return size;
    }

    /**
     * @param sourceFile
     * @param destFile
     * @return
     */
    public static boolean copy2File(File sourceFile, File destFile,
            int operationType) {
        if (ensureSourceAndDestFileValid(sourceFile, destFile, operationType)) {
            if (sourceFile.equals(destFile)) {
                return true;
            }
            if (sourceFile.isFile()) {
                return copy2SingleFile(sourceFile, destFile, operationType);
            } else {
                return copy2SingleFolder(sourceFile, destFile, operationType);
            }
        } else {
            return false;
        }
    }

    public static boolean copy2Directory(File sourceFile, File destFile,
            int operationType) {
        if (sourceFile == null || destFile == null) {
            throw new NullPointerException(sourceFile == null ? "sourceFile"
                    : "destFile" + " cannot be null");
        }
        if (!sourceFile.exists()) {
            throw new NullPointerException("sourceFile does not exist");
        }
        File[] sourceFiles = {
                sourceFile
        };
        return copy2Directory(sourceFiles, destFile, operationType);
    }

    public static boolean copy2Directory(File[] sourceFiles, File destFile,
            int operationType) {

        if (sourceFiles == null || destFile == null) {
            throw new NullPointerException(sourceFiles == null ? "sourceFile"
                    : "destFile" + " cannot be null");
        }

        for (int i = 0; i < sourceFiles.length; i++) {
            File file = sourceFiles[i];
            if (file == null || !file.exists()) {
                throw new NullPointerException(
                        "one or more sourceFiles not exist");
            }
        }

        if (ensureFileIsDirectory(destFile)) {
            for (int i = 0; i < sourceFiles.length; i++) {
                File sourceFile = sourceFiles[i];
                File finalFile = new File(destFile.getAbsolutePath(),
                        sourceFile.getName());
                if (!copy2File(sourceFile, finalFile, operationType)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 将sourceFile文件移动成为destFile
     * 
     * @param sourceFile
     * @param destFile
     * @return
     */
    public static boolean move2File(File sourceFile, File destFile,
            int operationType) {
        if (ensureSourceAndDestFileValid(sourceFile, destFile, operationType)) {
            // 这里不必检查是否sourceFile和destFile是同一个文件，renameTo自然会返回true
            // renameTo当存在重名文件时将返回false,所以不可用。
            // 移动不是复制，所以只要deskFile不存在，直接renameTo即可，不必一个一个递归
            // TODO
            if (sourceFile.equals(destFile)) {
                return true;
            }
            if (sourceFile.isFile()) {
                return move2SingleFile(sourceFile, destFile, operationType);
            } else {
                return move2SingleFolder(sourceFile, destFile, operationType);
            }
        } else {
            return false;
        }
    }

    /**
     * 将文件转移到某个目录，文件名不变
     * 
     * @param sourceFile
     * @param destFile
     * @return
     */
    public static boolean move2Directory(File sourceFile, File destFile,
            int operationType) {
        if (sourceFile == null || destFile == null) {
            throw new NullPointerException(sourceFile == null ? "sourceFile"
                    : "destFile" + " cannot be null");
        }
        if (!sourceFile.exists()) {
            throw new NullPointerException("sourceFile does not exist");
        }
        File[] sourceFiles = {
                sourceFile
        };
        return move2Directory(sourceFiles, destFile, operationType);
    }

    public static boolean move2Directory(File[] sourceFiles, File destFile,
            int operationType) {
        if (sourceFiles == null || destFile == null) {
            throw new NullPointerException(sourceFiles == null ? "sourceFile"
                    : "destFile" + " cannot be null");
        }

        for (int i = 0; i < sourceFiles.length; i++) {
            File file = sourceFiles[i];
            if (file == null || !file.exists()) {
                throw new NullPointerException(
                        "one or more sourceFiles not exist");
            }
        }
        if (ensureFileIsDirectory(destFile)) {
            for (int i = 0; i < sourceFiles.length; i++) {
                File sourceFile = sourceFiles[i];
                File finalFile = new File(destFile.getAbsolutePath(),
                        sourceFile.getName());
                if (!move2File(sourceFile, finalFile, operationType)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean delete(File file) {
        if (file == null) {
            throw new NullPointerException("file is null");
        }
        if (file.isDirectory()) {
            return deleteFolder(file);
        }
        return file.delete();

    }

    // 异步操作

    public static Runnable copy2FileAsync(File sourceFile, File destFile) {
        return null;
    }

    public static Runnable copy2DirectoryAsync(File sourceFile, File destFile) {
        return null;
    }

    public static Runnable move2FileAsync(File sourceFile, File destFile) {
        return null;
    }

    public static Runnable move2DirectoryAsync(File sourceFile, File destFile) {
        return null;
    }

    public static Runnable deleteAsync(File file) {
        return null;
    }

    /**
     * 提取文件缩略图，默认大小96 x 96. 如果没有指定context，将不会取到apk文件的缩略图
     * 
     * @param file
     * @return
     */
    public static Bitmap extractFileThumbnail(File file, Context context) {
        int type = getFileType(file);
        Bitmap thumb = null;
        switch (type) {
            case FILE_TYPE_IMAGE:// 获取图像文件缩略图
                thumb = getImageFileThumbnail(file, 96, 96);
                break;
            case FILE_TYPE_VIDEO:// 获取视频文件缩略图
                thumb = getVideoFileThumbnail(file, 96, 96);
                break;
            case FILE_TYPE_AUDIO:// 获取音乐文件缩略图，
                if (getFileSuffix(file).toLowerCase().equals("mp3")) {
                    thumb = getMP3Thumbnail(file);
                }
                break;
            case FILE_TYPE_APK:// 获取apk文件缩略图
                thumb = getApkIcon(context, file.getAbsolutePath());
                break;

            default:
                break;
        }
        return thumb;
    }

    /**
     * 提取文件缩略图,指定缩略图大小, 如果没有指定context，将不会取到apk文件的缩略图
     * 
     * @param file
     * @param width
     * @param height
     * @return
     */
    public static Bitmap extractFileThumbnail(File file, int width, int height,
            Context context) {
        int type = getFileType(file);
        Bitmap thumb = null;
        switch (type) {
            case FILE_TYPE_IMAGE:// 获取图像文件缩略图
                thumb = getImageFileThumbnail(file, width, height);
                break;
            case FILE_TYPE_VIDEO:// 获取视频文件缩略图
                thumb = getVideoFileThumbnail(file, width, height);
                break;
            case FILE_TYPE_AUDIO:// 获取音乐文件缩略图，
                if (getFileSuffix(file).toLowerCase().equals("mp3")) {
                    Bitmap tmp = getMP3Thumbnail(file);
                    thumb = ThumbnailUtils.extractThumbnail(tmp, width, height);
                }
                break;
            case FILE_TYPE_APK:// 获取apk文件按指定尺寸缩放过的缩略图，
                thumb = getApkResizedIcon(context, file.getAbsolutePath(), width,
                        height);
                break;

            default:
                break;
        }
        return thumb;
    }

    /**
     * 获取图片文件的缩略图
     * 
     * @param file
     * @param width
     * @param height
     * @return
     */
    public static Bitmap getImageFileThumbnail(File file, int width, int height) {
        Bitmap bitmap = null;
        String path = file.getAbsolutePath();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        // 只获取这个图片的宽和高
        bitmap = BitmapFactory.decodeFile(path, options);
        options.inJustDecodeBounds = false;
        // 计算缩放比,出现错误的时候有可能为-1
        int h = options.outHeight;
        int w = options.outWidth;
        if (h > 0 && w > 0) {
            int beWidth = w / width;
            int beHeight = h / height;
            int be = 1;
            if (beWidth < beHeight) {
                be = beWidth;
            } else {
                be = beHeight;
            }
            if (be <= 0) {
                be = 1;
            }
            options.inSampleSize = be;
            // 重新读入图片，读取缩放后的bitmap，注意这次要把options.inJustDecodeBounds 设为 false
            bitmap = BitmapFactory.decodeFile(path, options);
            // 利用ThumbnailUtils来创建缩略图，这里要指定要缩放哪个Bitmap对象
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        }

        return bitmap;
    }

    public static Bitmap getVideoFileThumbnail(File file, int width, int height) {
        // MINI_KIND: 512 x 384 ； MICRO_KIND: 96 x 96
        Bitmap thumb = null;
        if (width > 96 || height > 96) {
            // 大于96 x 96，则取MINI_KIND的缩略图并在此基础上再次抽取
            thumb = ThumbnailUtils.extractThumbnail(ThumbnailUtils
                    .createVideoThumbnail(file.getAbsolutePath(),
                            Thumbnails.MINI_KIND), width, height,
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        } else if (width == 96 && height == 96) {
            thumb = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(),
                    Thumbnails.MICRO_KIND);
        } else {
            // 小于96 x 96，则取MICRO_KIND的缩略图并在此基础上再次抽取
            thumb = ThumbnailUtils.extractThumbnail(ThumbnailUtils
                    .createVideoThumbnail(file.getAbsolutePath(),
                            Thumbnails.MICRO_KIND), width, height,
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        }

        return thumb;
    }

    /**
     * http://blog.csdn.net/toni001/article/details/6724785
     * 
     * @param file
     * @return
     */
    public static Bitmap getMP3Thumbnail(File file) {
        int buffSize = 204800;//
        FileInputStream mp3ips = null;
        Bitmap bitmap = null;
        try {
            mp3ips = new FileInputStream(file);
            if (buffSize > mp3ips.available()) {
                buffSize = mp3ips.available();
            }
            byte[] buff = new byte[buffSize];
            mp3ips.read(buff, 0, buffSize);
            if (indexOf("ID3".getBytes(), buff, 1, 512) == -1) {
                // No ID3V2
                return null;
            }
            if (indexOf("APIC".getBytes(), buff, 1, 512) != -1) {
                int searLen = indexOf(new byte[] {
                        (byte) 0xFF, (byte) 0xFB
                },
                        buff);
                int imgStart = indexOf(new byte[] {
                        (byte) 0xFF, (byte) 0xD8
                },
                        buff);
                int imgEnd = lastIndexOf(
                        new byte[] {
                                (byte) 0xFF, (byte) 0xD9
                        }, buff, 1,
                        searLen) + 2;
                byte[] imgb = cutBytes(imgStart, imgEnd, buff);
                bitmap = BitmapFactory.decodeByteArray(imgb, 0, imgb.length);
            } else {
                // No APIC
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mp3ips != null) {
                try {
                    mp3ips.close();
                } catch (Exception e2) {
                }
            }
        }

        return bitmap;
    }

    /**
     * 正向索引
     */
    public static int indexOf(byte[] tag, byte[] src) {
        return indexOf(tag, src, 1, src.length);
    }

    /**
     * 获取第index个的位置<br />
     * index从1开始
     */
    private static int indexOf(byte[] tag, byte[] src, int index, int len) {
        if (len > src.length) {
            try {
                throw new Exception("大于总个数");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int tagLen = tag.length;
        byte[] tmp = new byte[tagLen];
        for (int j = 0; j < len - tagLen + 1; j++) {
            for (int i = 0; i < tagLen; i++) {
                tmp[i] = src[j + i];
            }
            // 判断是否相等
            for (int i = 0; i < tagLen; i++) {
                if (tmp[i] != tag[i])
                    break;
                if (i == tagLen - 1) {
                    return j;
                }
            }

        }
        return -1;
    }

    /**
     * 倒序获取第index个的位置<br />
     * index从1开始
     */
    private static int lastIndexOf(byte[] tag, byte[] src, int index, int len) {
        if (len > src.length) {
            try {
                throw new Exception("大于总个数");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int size = 0;
        int tagLen = tag.length;
        byte[] tmp = new byte[tagLen];
        for (int j = len - tagLen; j >= 0; j--) {
            for (int i = 0; i < tagLen; i++) {
                tmp[i] = src[j + i];

            }
            for (int i = 0; i < tagLen; i++) {
                if (tmp[i] != tag[i])
                    break;
                if (i == tagLen - 1) {
                    size++;
                    return j;
                }
            }

        }
        return -1;
    }

    /**
     * 截取byte[]
     */
    private static byte[] cutBytes(int start, int end, byte[] src) {
        if (end <= start || start < 0 || end > src.length) {
            return null;
        }
        byte[] tmp = new byte[end - start];
        for (int i = 0; i < end - start; i++) {
            tmp[i] = src[start + i];
        }
        return tmp;
    }

    /**
     * 获取apk文件按指定尺寸缩放过的缩略图
     * 
     * @param context
     * @param apkPath
     * @return
     */
    public static Bitmap getApkResizedIcon(Context context, String apkPath,
            int width, int height) {
        Bitmap thumb = getApkIcon(context, apkPath);
        if (thumb != null) {
            return ThumbnailUtils.extractThumbnail(thumb, width, height,
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        }
        return thumb;
    }

    /**
     * 获取apk文件缩略图将不会改变icon大小
     * 
     * @param context
     * @param apkPath
     * @return
     */
    public static Bitmap getApkIcon(Context context, String apkPath) {
        Bitmap thumb = null;
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apkPath,
                PackageManager.GET_ACTIVITIES);
        if (info != null) {
            ApplicationInfo appInfo = info.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            try {
                thumb = ((BitmapDrawable) appInfo.loadIcon(pm)).getBitmap();
            } catch (OutOfMemoryError e) {

            }
        }
        return thumb;
    }

    /**
     * 打开文件
     * 
     * @param file
     * @param context
     * @return
     */
    public boolean openFile(File file, Context context) {
        int type = getFileType(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        Uri data = Uri.fromFile(file);
        switch (type) {
            case FILE_TYPE_APK:
                intent.setDataAndType(data, "application/vnd.android.package-archive");
                break;
            case FILE_TYPE_IMAGE:
                intent.setDataAndType(data, "image/*");
                break;
            case FILE_TYPE_AUDIO:
                intent.putExtra("oneshot", 0);
                intent.putExtra("configchange", 0);
                intent.setDataAndType(data, "audio/*");
                break;
            case FILE_TYPE_TXT:
                intent.setDataAndType(data, "text/plain");
                break;
            case FILE_TYPE_VIDEO:
                intent.putExtra("oneshot", 0);
                intent.putExtra("configchange", 0);
                intent.setDataAndType(data, "video/*");
                break;
            case FILE_TYPE_ZIP:
                intent.setDataAndType(data, "application/zip");
                break;
            case FILE_TYPE_WORD:
                intent.setDataAndType(data, "application/msword");
                break;
            case FILE_TYPE_PPT:
                intent.setDataAndType(data, "application/vnd.ms-powerpoint");
                break;
            case FILE_TYPE_EXCEL:
                intent.setDataAndType(data, "application/vnd.ms-excel");
                break;
            case FILE_TYPE_HTML:
                intent.setDataAndType(data, "text/html");
                break;
            case FILE_TYPE_PDF:
                intent.setDataAndType(data, "application/pdf");
                break;
            case FILE_TYPE_TORRENT:
                intent.setDataAndType(data, "torrent/*");
                break;
            case FILE_TYPE_CHM:
                intent.setDataAndType(data, "application/mshelp");
                break;

            default:
                break;
        }
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    /**
     * 根据后缀取得文件类型,FILE_TYPE_IMAGE,FILE_TYPE_SOUND......
     */
    public static int getFileType(File file) {
        if (file.isDirectory()) {
            return FILE_TYPE_FOLDER;
        } else {
            String suffix = getFileSuffix(file).toLowerCase();
            if (isArrayContains(apkSuffixArray, suffix)) {
                return FILE_TYPE_APK;
            } else if (isArrayContains(imageSuffixArray, suffix)) {
                return FILE_TYPE_IMAGE;
            } else if (isArrayContains(soundSuffixArray, suffix)) {
                return FILE_TYPE_AUDIO;
            } else if (isArrayContains(videoSuffixArray, suffix)) {
                return FILE_TYPE_VIDEO;
            } else if (isArrayContains(txtSuffixArray, suffix)) {
                return FILE_TYPE_TXT;
            } else if (isArrayContains(zipSuffixArray, suffix)) {
                return FILE_TYPE_ZIP;
            } else if (isArrayContains(wordSuffixArray, suffix)) {
                return FILE_TYPE_WORD;
            } else if (isArrayContains(pptSuffixArray, suffix)) {
                return FILE_TYPE_PPT;
            } else if (isArrayContains(excelSuffixArray, suffix)) {
                return FILE_TYPE_EXCEL;
            } else if (isArrayContains(pdfSuffixArray, suffix)) {
                return FILE_TYPE_PDF;
            } else if (isArrayContains(ebookSuffixArray, suffix)) {
                return FILE_TYPE_EBOOK;
            } else if (isArrayContains(torrentSuffixArray, suffix)) {
                return FILE_TYPE_TORRENT;
            } else if (isArrayContains(chmSuffixArray, suffix)) {
                return FILE_TYPE_CHM;
            } else if (isArrayContains(htmlSuffixArray, suffix)) {
                return FILE_TYPE_HTML;
            } else {
                return FILE_TYPE_NORMAL;
            }
        }
    }

    /**
     * 获得文件后缀
     */
    public static String getFileSuffix(File file) {
        String fileName = file.getName();
        String suffix = "";
        int offset = fileName.lastIndexOf(".");
        // -1则没有后缀。0,则表示是一个隐藏文件而没有后缀，offset == fileName.length() -
        // 1，表示"."是最后一个字符，没有后缀
        if (offset > 0 && offset < fileName.length() - 1) {
            suffix = fileName.substring(offset + 1);
        }
        return suffix;
    }

    /**
     * @param file
     * @param includeHiddleFiles
     * @param includeFolder
     * @return
     */
    private static int getSubfilesNumberInFolder(File file,
            boolean includeHiddleFiles, boolean includeFolder) {
        int size = 0;
        File[] files = file.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file2 = files[i];
                if (!includeHiddleFiles && file2.isHidden()) {
                    continue;
                }
                if (file2.isDirectory()) {
                    size += getSubfilesNumberInFolder(file2,
                            includeHiddleFiles, includeFolder)
                            + (includeFolder ? 1 : 0);
                } else {
                    size += 1;
                }
            }
        }
        return size;
    }

    private static long getSilgleFolderSize(File file) {
        long size = 0L;
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file2 = files[i];
            if (file2.isDirectory()) {
                size += getSilgleFolderSize(file2);
            } else {
                size += file2.length();
            }
        }
        // 文件夹占据4k,或者更多，取决于里面文件数量
        size += file.length();
        return size;
    }

    private static boolean deleteFolder(File file) {
        if (file.isDirectory()) {
            File[] subFiles = file.listFiles();
            if (subFiles == null || subFiles.length == 0) {
                return file.delete();
            } else {
                for (int i = 0; i < subFiles.length; i++) {
                    File sFile = subFiles[i];
                    if (sFile.isDirectory()) {
                        if (!deleteFolder(sFile)) {
                            return false;
                        }
                    } else {
                        if (!sFile.delete()) {
                            return false;
                        }
                    }
                }
            }
            // 全部删除后还要再删除自己
            return file.delete();
        } else {
            throw new ClassCastException("file is not a directory");
        }
    }

    /**
     * 复制单个文件夹而不是文件。私有静态方法，由copy2File(File sourceFile, File destFile)调用。
     * 在调用前就已经确保参数合法，不必检查，sourceFile一定存在且是文件夹，sourceFile和destFile也一定不是同一个文件。
     * destFile不一定不存在。若存在，有可能是文件也有可能是文件夹 Folder To Folder
     * 
     * @param operationType
     * @return
     */
    private static boolean copy2SingleFolder(File sourceFile, File destFile,
            int operationType) {
        if (destFile.exists()) {
            switch (operationType) {
                case Operation_Ski_All:
                    // 不会删除文件，有重名直接跳过
                    return false;
                case Operation_Merge:
                    // 只合并文件夹，不会删除文件，只有当sourceFile和destFile都是文件夹时才能通过
                    if (destFile.isFile()) {
                        return false;
                    }
                    break;
                case Operation_Merge_And_Overwrite:
                    // 合并文件夹，删除同名文件。
                    if (destFile.isFile() && !delete(destFile)) {
                        return false;
                    }
                    break;
                default:
                    break;
            }
        }
        // 至此destFile仍然可能存在,如果存在，一定是文件夹 TODO
        if (destFile.exists() || destFile.mkdirs()) {
            File[] files = sourceFile.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    File sourceSubFile = files[i];
                    File destSubFile = new File(destFile.getAbsolutePath(),
                            sourceSubFile.getName());
                    if (sourceSubFile.isDirectory()) {
                        if (!copy2SingleFolder(sourceSubFile, destSubFile,
                                operationType)) {
                            return false;
                        }
                    } else {
                        if (!copy2SingleFile(sourceSubFile, destSubFile,
                                operationType)) {
                            return false;
                        }

                    }
                }
            }
        }
        return true;
    }

    /**
     * 复制单个文件而不是文件夹。私有静态方法，由copy2File或者copy2SingleFolder调用。
     * 在调用前就已经确保参数合法，不必进行检查,destFile必然有父文件夹，sourceFile一定存在且是文件，
     * sourceFile和destFile也一定不是同一个文件。deskFile有可能存在也有可能不存在。若存在，有可能是文件也有可能是文件夹
     * File To File
     * 
     * @param operationType
     * @return
     */
    private static boolean copy2SingleFile(File sourceFile, File destFile,
            int operationType) {
        if (destFile.exists()) {
            if (operationType == Operation_Merge_And_Overwrite) {
                // 合并文件夹，删除同名文件。只有这种情况才有可能不返回false
                if (!delete(destFile)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        // 此处destFile一定不存在
        boolean copyOK = true;
        BufferedInputStream inputStream = null;
        BufferedOutputStream outputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(
                    sourceFile));
            outputStream = new BufferedOutputStream(new FileOutputStream(
                    destFile));
            byte[] buffer = new byte[1024 * 5];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            copyOK = false;
            e.printStackTrace();
        } catch (IOException e) {
            copyOK = false;
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                copyOK = false;
                e.printStackTrace();
            }
        }

        return copyOK;
    }

    /**
     * 使用FileChannel复制文件，速度略快
     * 
     * @param sourceFile
     * @param destFile
     * @return
     */
    @SuppressWarnings("unused")
    private static boolean copy2SingleFileByChannel(File sourceFile,
            File destFile) {
        boolean copyOK = true;
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputStream = new FileInputStream(sourceFile);
            outputStream = new FileOutputStream(destFile);
            inputChannel = inputStream.getChannel();
            outputChannel = outputStream.getChannel();
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
        } catch (Exception e) {
            copyOK = false;
        } finally {
            try {
                inputChannel.close();
                inputStream.close();
                outputChannel.close();
                outputStream.close();
            } catch (IOException e) {
                copyOK = false;
                e.printStackTrace();
            }
        }
        return copyOK;
    }

    /**
     * 移动单个文件夹而不是文件。私有静态方法，由move2File(File sourceFile, File destFile)调用。
     * 在调用前就已经确保参数合法，不必检查，sourceFile一定存在且是文件夹，sourceFile和destFile也一定不是同一个文件。
     * destFile不一定不存在。若存在，有可能是文件也有可能是文件夹 Folder To Folder
     * 
     * @param operationType
     * @return
     */
    private static boolean move2SingleFolder(File sourceFile, File destFile,
            int operationType) {
        if (destFile.exists()) {
            switch (operationType) {
                case Operation_Ski_All:
                    // 不会删除文件，有重名直接跳过
                    return false;
                case Operation_Merge:
                    // 只合并文件夹，不会删除文件，只有当sourceFile和destFile都是文件夹时才能通过
                    if (destFile.isFile()) {
                        return false;
                    }
                    break;
                case Operation_Merge_And_Overwrite:
                    // 合并文件夹，删除同名文件。
                    if (destFile.isFile() && !delete(destFile)) {
                        return false;
                    }
                    break;
                default:
                    return false;
            }
        }
        // 至此destFile仍然可能存在,如果存在，一定是文件夹
        if (destFile.exists()) {
            // 一级一级向下来，不需要mkdirs
            File[] files = sourceFile.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    File sourceSubFile = files[i];
                    File destSubFile = new File(destFile.getAbsolutePath(),
                            sourceSubFile.getName());
                    if (sourceSubFile.isDirectory()) {
                        if (!move2SingleFolder(sourceSubFile, destSubFile,
                                operationType)) {
                            return false;
                        }
                    } else {
                        if (!move2SingleFile(sourceSubFile, destSubFile,
                                operationType)) {
                            return false;
                        }
                    }
                }
            }
        } else {
            return sourceFile.renameTo(destFile);
        }
        return true;
    }

    /**
     * 移动单个文件而不是文件夹。私有静态方法，由move2File或者move2SingleFolder调用。
     * 在调用前就已经确保参数合法，不必进行检查,destFile必然有父文件夹，sourceFile一定存在且是文件，
     * sourceFile和destFile也一定不是同一个文件。deskFile有可能存在也有可能不存在。若存在，有可能是文件也有可能是文件夹
     * File To File
     * 
     * @param operationType
     * @return
     */
    private static boolean move2SingleFile(File sourceFile, File destFile,
            int operationType) {
        if (destFile.exists()) {
            if (operationType == Operation_Merge_And_Overwrite) {
                // 合并文件夹，删除同名文件。只有这种情况才有可能不返回false
                if (!delete(destFile)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        // 此处destFile一定不存在
        return sourceFile.renameTo(destFile);
    }

    /**
     * 用于确定某个方法中父目录的存在性，确保destFile是个目录，如果不存在，则创建，如果存在但不是目录，则返回false。
     * 
     * @param destFile
     * @return
     */
    private static boolean ensureFileIsDirectory(File destFile) {
        if (destFile == null) {
            throw new NullPointerException("destFile cannot be null");
        }
        // 如果一个文件不存在，则isDirectory必然为false，而当mkdirs后isDirectory就是true了
        if (destFile.exists()) {
            // 如果destFile存在且不是个文件则直接返回false，还是删除？ TODO
            if (!destFile.isDirectory()) {
                return false;
            }
        } else {
            // 如果destFile不存在则创建文件夹，创建失败则返回false，有可能是没有权限也有可能是路径不对
            // 也有可能destFile的父级目录却是个文件而不是目录 TODO
            if (!destFile.mkdirs()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 确保源文件和目标文件的合法性。
     * 
     * @param sourceFile 要复制或者移动的文件，必须存在。
     * @param destFile
     *            要移动到的目标文件（不是要移动到的目录！），不一定存在，若存在则删除，若其向上某一级得到的结果有可能是文件，则不合法，
     *            因为这样此文件就不能创建，mkdirs不会成功 TODO
     * @return 若返回true，则：destFile不存在，destFile的父级目录存在，只等复制过去
     */
    private static boolean ensureSourceAndDestFileValid(File sourceFile,
            File destFile, int operationType) {
        if (sourceFile == null || destFile == null) {
            throw new NullPointerException(sourceFile == null ? "sourceFile"
                    : "destFile" + " cannot be null");
        }

        if (!sourceFile.exists()) {
            throw new NullPointerException("sourceFile does not exist");
        }

        if (destFile.exists()) {
            switch (operationType) {
                case Operation_Ski_All:
                    // 不会删除文件，有重名直接跳过
                    return false;
                case Operation_Merge:
                    // 只合并文件夹，不会删除文件，只有当sourceFile和destFile都是文件夹时才能通过
                    if (sourceFile.isFile() || destFile.isFile()) {
                        return false;
                    }
                    break;
                case Operation_Merge_And_Overwrite:
                    // 合并文件夹，删除同名文件。
                    if (sourceFile.isFile() || destFile.isFile()) {
                        if (!delete(destFile)) {
                            return false;
                        }
                    }
                    break;

                default:
                    break;
            }
        }

        return ensureFileIsDirectory(destFile.getParentFile());
    }

    private static boolean isArrayContains(String[] strs, String suffix) {
        if (strs == null || suffix == null) {
            return false;
        }
        for (int i = 0; i < strs.length; i++) {
            if (suffix.equals(strs[i])) {
                return true;
            }
        }
        return false;
    }

    private static void zip(File file, ZipOutputStream out, String basedir) {
        if (file.isDirectory()) {
            zipDirectory(file, out, basedir);
        } else {
            zipFile(file, out, basedir);
        }
    }

    private static void zipDirectory(File dir, ZipOutputStream out,
            String basedir) {
        if (!dir.exists())
            return;

        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            zip(files[i], out, basedir + dir.getName() + "/");
        }
    }

    private static void zipFile(File file, ZipOutputStream out, String basedir) {
        if (!file.exists()) {
            return;
        }
        try {
            BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(file));
            ZipEntry entry = new ZipEntry(basedir + file.getName());
            out.putNextEntry(entry);
            int count;
            byte data[] = new byte[BUFFER];
            while ((count = bis.read(data, 0, BUFFER)) != -1) {
                out.write(data, 0, count);
            }
            bis.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}