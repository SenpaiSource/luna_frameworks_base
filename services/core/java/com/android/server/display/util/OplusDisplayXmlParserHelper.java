package com.android.server.display.util;

import android.util.Slog;
import android.util.Xml;
import com.android.server.display.feature.ambience.events.delegate.DeepThinkerDelegate;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/deps.dex */
public class OplusDisplayXmlParserHelper {
    private static final int BUFFER_LEN_UNIT = 128;
    private static final int DEFAULT_READER_BUFFER_LEN = 8192;
    public static final String TAG = "OplusDisplayXmlParserHelper";
    private static final String TAG_VERSION = "version";
    private long mMaxVersion = -1;
    private String mSysFile;
    private String mVndFile;

    public OplusDisplayXmlParserHelper(String sysFile, String vndFile) {
        this.mSysFile = DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY;
        this.mVndFile = DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY;
        this.mSysFile = sysFile;
        this.mVndFile = vndFile;
    }

    public boolean init(DisplayParser parser) {
        File file;
        boolean isRusDataDir;
        File vndFile = null;
        if (this.mVndFile != null) {
            vndFile = new File(this.mVndFile);
        }
        File sysFile = null;
        if (this.mSysFile != null) {
            sysFile = new File(this.mSysFile);
        }
        boolean vndFileExist = vndFile != null ? vndFile.exists() : false;
        boolean sysFileExist = sysFile != null ? sysFile.exists() : false;
        if (vndFileExist && sysFileExist) {
            long vndVersion = getVersion(parser, readFromFile(vndFile));
            long sysVersion = getVersion(parser, readFromFile(sysFile));
            Slog.d(TAG, "vndVersion=" + vndVersion + ", sysVersion=" + sysVersion);
            file = vndVersion > sysVersion ? vndFile : sysFile;
            this.mMaxVersion = vndVersion > sysVersion ? vndVersion : sysVersion;
        } else if (vndFileExist && !sysFileExist) {
            Slog.w(TAG, this.mSysFile + " is not exists, and " + this.mVndFile + " is exists!");
            file = vndFile;
            this.mMaxVersion = getVersion(parser, readFromFile(vndFile));
        } else {
            if (vndFileExist || !sysFileExist) {
                Slog.w(TAG, this.mVndFile + " is not exists, " + this.mSysFile + " is not exists!");
                return false;
            }
            Slog.w(TAG, this.mSysFile + " is exists, and " + this.mVndFile + " is not exists!");
            file = sysFile;
            this.mMaxVersion = getVersion(parser, readFromFile(sysFile));
        }
        if (parser != null) {
            if (sysFile == null || file != sysFile || !this.mSysFile.contains("/data/system/")) {
                isRusDataDir = false;
            } else {
                isRusDataDir = true;
            }
            parser.parseContent(readFromFile(file), false, isRusDataDir);
        }
        return true;
    }

    public void initData(DisplayParser parser) {
        File vndFile = null;
        if (this.mVndFile != null) {
            vndFile = new File(this.mVndFile);
        }
        boolean vndFileExist = vndFile != null ? vndFile.exists() : false;
        if (vndFileExist && parser != null) {
            parser.parseContent(readFromFile(vndFile));
        } else {
            Slog.w(TAG, this.mVndFile + " is not exists");
        }
    }

    private long getVersion(DisplayParser parser, String content) {
        if (parser != null) {
            try {
                return parser.parseVersion(content);
            } catch (Exception e) {
                Slog.e(TAG, "getVersion error", e);
            }
        }
        return -1L;
    }

    public long getMaxVersion() {
        return this.mMaxVersion;
    }

    private void setFileReadable(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            boolean result = file.setReadable(true, false);
            Slog.i(TAG, "setReadable result :" + result);
        } else {
            Slog.i(TAG, "filename :" + filename + " is not exist");
        }
    }

    private String readFromFile(File path) {
        if (path == null || !path.exists()) {
            return DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY;
        }
        try (InputStream is = new FileInputStream(path);
             BufferedReader in = new BufferedReader(new InputStreamReader(is), 8192)) {
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                buffer.append(line).append("\n");
            }
            return buffer.toString();
        } catch (Exception e) {
            Slog.e(TAG, "readFromFile error", e);
            return null;
        }
    }

    public static class DisplayParser {
        private long mCurrentVersion = -1;

        public long getCurrentVersion() {
            return this.mCurrentVersion;
        }

        public boolean parseContent(String content, boolean isRusUpdte, boolean isRusDataDir) {
            return false;
        }

        public boolean parseContent(String content, boolean isRusUpdte) {
            return parseContent(content, isRusUpdte, false);
        }

        public boolean parseContent(String content) {
            return parseContent(content, false, false);
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public long parseVersion(String content) throws XmlPullParserException, IOException {
            if (content == null) {
                Slog.d(OplusDisplayXmlParserHelper.TAG, "\tcontent is null");
                this.mCurrentVersion = -1L;
                return this.mCurrentVersion;
            }
            StringReader strReader = null;
            try {
                try {
                    try {
                        XmlPullParser parser = Xml.newPullParser();
                        strReader = new StringReader(content);
                        parser.setInput(strReader);
                        for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                            switch (eventType) {
                                case 0:
                                    break;
                                case 1:
                                default:
                                    break;
                                case 2:
                                    String tagName = parser.getName();
                                    parser.next();
                                    String text = parser.getText();
                                    if ("version".equals(tagName) && text != null) {
                                        strReader.close();
                                        String cleanText = text.trim();
                                        Slog.d(OplusDisplayXmlParserHelper.TAG, "fileVersion = " + cleanText);
                                        try {
                                            this.mCurrentVersion = Long.parseLong(cleanText);
                                        } catch (NumberFormatException nfe) {
                                            this.mCurrentVersion = -1L;
                                        }
                                        long j = this.mCurrentVersion;
                                        return j;
                                    }
                                    break;
                                case 3:
                                    break;
                            }
                        }
                    } catch (IOException e) {
                        Slog.e(OplusDisplayXmlParserHelper.TAG, "Got execption parsing permissions.", e);
                        if (strReader != null) {
                        }
                        this.mCurrentVersion = -1L;
                        return this.mCurrentVersion;
                    }
                } catch (XmlPullParserException e2) {
                    Slog.e(OplusDisplayXmlParserHelper.TAG, "Got execption parsing permissions.", e2);
                    if (strReader != null) {
                    }
                    this.mCurrentVersion = -1L;
                    return this.mCurrentVersion;
                }
                strReader.close();
                this.mCurrentVersion = -1L;
                return this.mCurrentVersion;
            } catch (Throwable th) {
                if (strReader != null) {
                    strReader.close();
                }
                throw th;
            }
        }
    }
}
