package me.suwash.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

import me.suwash.util.constant.UtilMessageConst;
import me.suwash.util.exception.UtilException;

import org.apache.commons.lang3.StringUtils;

/**
 * ファイル操作関連ユーティリティ。
 */
public final class FileUtils {

    /**
     * ユーティリティ向けに、コンストラクタはprivate宣言。
     */
    private FileUtils() {}

    /** ファイルタイプ。 */
    private enum FileType {
        /** アスキーファイル。 */
        ASCII,
        /** バイリナリファイル。 */
        BINARY
    }

    /**
     * ファイル読み込み共通チェック。
     *
     * @param filePath ファイルパス
     * @param charset 文字コード
     */
    public static void readCheck(final String filePath, final String charset) {
        readCheckMain(FileType.ASCII, filePath, charset);
    }

    /**
     * バイナリファイル読み込み共通チェック。
     *
     * @param filePath ファイルパス
     */
    public static void readBinaryCheck(final String filePath) {
        readCheckMain(FileType.BINARY, filePath, null);
    }

    /**
     * ファイル読み込みチェックの本処理。
     *
     * @param fileType ファイルタイプ
     * @param filePath ファイルパス
     * @param charset 文字コード
     */
    private static void readCheckMain(final FileType fileType, final String filePath, final String charset ) {
        // ファイルパス
        if (StringUtils.isEmpty(filePath)) {
            throw new UtilException(UtilMessageConst.CHECK_NOTNULL, new Object[] {
                "filePath"
            });
        }
        final File file = new File(filePath);
        if (!file.exists()) {
            throw new UtilException(UtilMessageConst.CHECK_NOTEXIST, new Object[] {
                file
            });
        }
        if (!file.isFile()) {
            throw new UtilException(UtilMessageConst.FILE_CHECK, new Object[] {
                file
            });
        }

        // ファイルタイプを確認
        if (FileType.ASCII.equals(fileType)) {
            // アスキーファイルの場合
            // 文字コード
            if (StringUtils.isEmpty(charset)) {
                throw new UtilException(UtilMessageConst.CHECK_NOTNULL, new Object[] {
                    "charset"
                });
            }
            try {
                Charset.forName(charset);
            } catch (Exception e) {
                throw new UtilException(UtilMessageConst.UNSUPPORTED_PATTERN, new Object[] {
                    "charset", charset
                }, e);
            }
        }
    }

    /**
     * ファイル読み込み共通チェック（クラスパス）。
     *
     * @param filePath ファイルパス（クラスパス）
     * @param charset 文字コード
     * @param type 検索先クラスパスと同一のクラスローダで読み込まれるクラス ※呼び出し元クラスなど
     */
    public static void readCheckByClasspath(final String filePath, final String charset, final Class<?> type) {
        // ファイルパス
        if (StringUtils.isEmpty(filePath)) {
            throw new UtilException(UtilMessageConst.CHECK_NOTNULL, new Object[] {
                "filePath"
            });
        }
        Class<?> checkTargetType = FileUtils.class;
        if (type != null) {
            checkTargetType = type;
        }
        final URL resource = checkTargetType.getResource(filePath);
        if (resource == null) {
            throw new UtilException(UtilMessageConst.CHECK_NOTEXIST, new Object[] {
                filePath
            });
        }
        final File file = new File(resource.getPath());
        if (!file.isFile()) {
            throw new UtilException(UtilMessageConst.FILE_CHECK, new Object[] {
                file
            });
        }

        // 文字コード
        if (StringUtils.isEmpty(charset)) {
            throw new UtilException(UtilMessageConst.CHECK_NOTNULL, new Object[] {
                "charset"
            });
        }
        try {
            Charset.forName(charset);
        } catch (Exception e) {
            throw new UtilException(UtilMessageConst.UNSUPPORTED_PATTERN, new Object[] {
                "charset", charset
            }, e);
        }
    }

    /**
     * ファイル書き出し共通チェック。
     *
     * @param filePath ファイルパス
     * @param charset 文字コード
     */
    public static void writeCheck(final String filePath, final String charset) {
        // ファイルパス
        if (StringUtils.isEmpty(filePath)) {
            throw new UtilException(UtilMessageConst.CHECK_NOTNULL, new Object[] {
                "filePath"
            });
        }
        final File file = new File(filePath);
        if (file.isDirectory()) {
            throw new UtilException(UtilMessageConst.FILE_CHECK, new Object[] {
                "filePath"
            });
        }

        // 文字コード
        if (StringUtils.isEmpty(charset)) {
            throw new UtilException(UtilMessageConst.CHECK_NOTNULL, new Object[] {
                "charset"
            });
        }
        try {
            Charset.forName(charset);
        } catch (Exception e) {
            throw new UtilException(UtilMessageConst.UNSUPPORTED_PATTERN, new Object[] {
                "charset", charset
            }, e);
        }
    }

    /**
     * 指定されたディレクトリを再帰的に作成します。
     *
     * @param dirPath 作成対象ディレクトリ
     * @return 作成に成功した または すでに存在する場合、true
     */
    public static boolean mkdirs(final String dirPath) {
        final File dir = new File(dirPath);
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return true;
    }

    /**
     * 指定されたディレクトリを再帰的に削除します。
     *
     * @param dirPath 削除対象ディレクトリ
     * @return 削除に成功した または 存在しない場合、true
     */
    public static boolean rmdirs(final String dirPath) {
        final File dir = new File(dirPath);
        if (dir.exists()) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * 指定されたディレクトリを初期化します。
     * ディレクトリが存在する場合、配下を再帰的に削除し、再作成します。
     * ディレクトリが存在しない場合、親ディレクトリを再帰的に作成します。
     *
     * @param dirPath 対象ディレクトリ
     */
    public static void initDir(final String dirPath) {
        rmdirs(dirPath);
        mkdirs(dirPath);
    }

    /**
     * ファイル上書き共通処理。
     *
     * @param filePath ファイルパス
     */
    public static void setupOverwrite(final String filePath) {
        // 出力ファイルの存在チェック
        final File file = new File(filePath);
        if (file.isFile() && !file.delete()) {
            throw new UtilException(UtilMessageConst.FILE_CANTDELETE, new Object[] {
                file
            });
        }

        // 出力ディレクトリの作成
        final File outputDir = file.getParentFile();
        if (!mkdirs(outputDir.getPath())) {
            throw new UtilException(UtilMessageConst.DIR_CANTCREATE, new Object[] {
                outputDir
            });
        }
    }


    /**
     * ディレクトリ読み込み共通チェック。
     *
     * @param dirPath ディレクトリパス
     */
    public static void readDirCheck(final String dirPath) {
        // ディレクトリパス
        if (StringUtils.isEmpty(dirPath)) {
            throw new UtilException(UtilMessageConst.CHECK_NOTNULL, new Object[] {
                "dirPath"
            });
        }
        final File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new UtilException(UtilMessageConst.CHECK_NOTEXIST, new Object[] {
                dir
            });
        }
        if (!dir.isDirectory()) {
            throw new UtilException(UtilMessageConst.DIR_CHECK, new Object[] {
                dir
            });
        }
    }

    /**
     * 新規空ファイルを作成します。
     * ファイルが存在する場合、削除して再作成します。
     * 配置ディレクトリが存在しない場合、親ディレクトリを再帰的に作成し、新規ファイルを作成します。
     *
     * @param filePath
     */
    public static void createNewFile(final String filePath) {
        setupOverwrite(filePath);
        final File file = new File(filePath);
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new UtilException(UtilMessageConst.FILE_CANTWRITE, new Object[] { filePath }, e);
        }
    }

}
