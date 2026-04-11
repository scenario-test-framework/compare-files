package me.suwash.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * テスト関連ユーティリティ。
 */
public final class TestUtils {

    private static final String MSG_DIFF = "[DIFF ]内容に差異があります。 - ";
    private static final String MSG_ACTUAL_ONLY = "[DIFF ]実績値のみに存在します。 - ";
    private static final String MSG_EXPECT_ONLY = "[DIFF ]期待値のみに存在します。 - ";
    private static final String MSG_BOTH_FILE_NOT_EXIST = "[WARN ]期待値、実績値どちらもファイルが存在しません。 - ";
    private static final String MSG_BOTH_NOT_EXIST = "[ERROR]期待値、実績値どちらも存在しません。 - ";
    private static final String FILE_SEPARATOR = "/";
    private static final String DEFAULT_CHARSET = "utf8";

    /**
     * ユーティリティ向けに、コンストラクタはprivate宣言。
     */
    private TestUtils() {}

    /**
     * ディレクトリ構成の完全一致を確認します。
     *
     * @param expectDirPath 期待値ディレクトリ
     * @param actualDirPath 実績値ディレクトリ
     */
    public static void assertDirEquals(final String expectDirPath, final String actualDirPath) {
        //--------------------------------------------------
        // ディレクトリ存在チェック
        //--------------------------------------------------
        final File expectDir = new File(expectDirPath);
        final File actualDir = new File(actualDirPath);

        // 期待値配下のディレクトリリスト
        final List<File> expectDirList = getDirList(expectDirPath);
        // 実績値配下のディレクトリリスト
        final List<File> actualDirList = getDirList(actualDirPath);

        // 期待値のみディレクトリリスト
        final List<File> expectOnlyDirList = new ArrayList<File>();
        // 実績値のみディレクトリリスト
        final List<File> actualOnlyDirList = new ArrayList<File>();
        // 存在するディレクトリ相対パスリスト
        final List<String> existRelPathList = new ArrayList<String>();

        // 期待値→実績値チェック
        for (final File curExpectDir : expectDirList) {
            // 相対パスが一致するディレクトリの存在チェック
            if (isExistSameRelPath(curExpectDir, expectDir, actualDir)) {
                // 存在する場合、存在する相対パスリストに追加
                existRelPathList.add(getRelPath(curExpectDir, expectDir));
            } else {
                // 存在しない場合、期待値のみディレクトリリストに追加
                expectOnlyDirList.add(curExpectDir);
            }
        }

        // 実績値→期待値チェック
        for (final File curActualDir : actualDirList) {
            // 相対パスが一致するディレクトリの存在チェック
            if (isExistSameRelPath(curActualDir, actualDir, expectDir)) {
                // 存在する場合、存在する相対パスリストに追加
                existRelPathList.add(getRelPath(curActualDir, actualDir));
            } else {
                // 存在しない場合
                actualOnlyDirList.add(curActualDir);
            }
        }

        // ディレクトリ存在チェック
        boolean isSame = true;
        if (!expectOnlyDirList.isEmpty()) {
            // 期待値のみディレクトリリストに要素が登録されている場合
            isSame = false;
            for (final File curDir : expectOnlyDirList) {
                System.err.println(MSG_EXPECT_ONLY + curDir);
            }
        }
        if (!actualOnlyDirList.isEmpty()) {
            // 実績値のみディレクトリリストに要素が登録されている場合
            isSame = false;
            for (final File curDir : actualOnlyDirList) {
                System.err.println(MSG_ACTUAL_ONLY + curDir);
            }
        }
        if (! isSame) {
            // ディレクトリの一致チェック
            assertTrue("ディレクトリ構成が一致していること", isSame);
        }

        //--------------------------------------------------
        // ファイル比較
        //--------------------------------------------------
        // 双方に存在するディレクトリリストを全件ループ
        for (final String curRelPath : existRelPathList) {
            // 双方のディレクトリ直下のファイル群を比較
            assertFilesEquals(expectDirPath + FILE_SEPARATOR + curRelPath, actualDirPath + FILE_SEPARATOR + curRelPath, DEFAULT_CHARSET);
        }

    }

    /**
     * 指定ディレクトリ配下のサブディレクトリを再帰的に検索して、リストで返します。
     *
     * @param dirPath 検索対象ディレクトリ
     * @return サブディレクトリリスト
     */
    private static List<File> getDirList(final String dirPath) {
        final File rootDir = new File(dirPath);
        if (! rootDir.isDirectory()) {
            fail(dirPath + " はディレクトリではありません。");
        }

        // 再帰的にディレクトリをリストアップ
        final List<File> finded = new ArrayList<File>();
        toListDirRecursive(finded, rootDir);

        // パスでソート
        Collections.sort(finded);
        return finded;
    }
    /**
     * 再帰呼び出し用 ディレクトリリストアップ。
     *
     * @param finded 検索結果の登録先リスト
     * @param targetDir 対象ディレクトリ
     */
    private static void toListDirRecursive(final List<File> finded, final File targetDir) {
        // ディレクトリ内のファイルをループ
        final File[] children = targetDir.listFiles();
        if (children == null) {
            // nullが返却された場合、ここで終了
            return;
        }

        for (final File curChild : children) {
            // ファイルタイプを確認
            if (curChild.isDirectory()) {
                // ディレクトリの場合、リストに追加
                finded.add(curChild);
                // 再帰呼び出し
                if (curChild.isDirectory()) {
                    toListDirRecursive(finded, curChild);
                }
            }
        }
    }

    /**
     * 2つのディレクトリに同一の相対パスが存在するか確認します。
     *
     * @param targetFile 確認対象
     * @param removeDir 確認対象から削除するディレクトリ
     * @param appendDir 確認対象に追加するディレクトリ
     * @return 存在する場合、true
     */
    private static boolean isExistSameRelPath(final File targetFile, final File removeDir, final File appendDir) {
        final String targetRelPath = getRelPath(targetFile, removeDir);
        final String appendAbsDirPath = appendDir.getAbsolutePath().replace(File.separator, FILE_SEPARATOR);
        final String targetPath = appendAbsDirPath + FILE_SEPARATOR + targetRelPath;
        final File target = new File(targetPath);
        return target.exists();
    }

    /**
     * 取得対象ファイルと、カレントディレクトリから、相対パスを返します。
     *
     * @param targetFile 取得対象ファイル
     * @param currentDir カレントディレクトリ
     * @return 取得対象ファイルの相対パス
     */
    private static String getRelPath(final File targetFile, final File currentDir) {
        // 双方のフルパスを / 区切りで取得
        final String basePath = targetFile.getAbsolutePath().replace(File.separator, FILE_SEPARATOR);
        final String removePath = currentDir.getAbsolutePath().replace(File.separator, FILE_SEPARATOR);

        // 取得対象パスからカレントディレクトリを除去して、相対パスとして返却
        return basePath.replace(removePath + FILE_SEPARATOR, StringUtils.EMPTY);
    }

    /**
     * 指定ディレクトリ直下のファイル群の一致を確認します。
     *
     * @param expectDir 期待値ディレクトリ
     * @param actualDir 実績値ディレクトリ
     * @param charset 読み込み文字コード
     */
    public static void assertFilesEquals(final String expectDir, final String actualDir, final String charset) {
        final File expect = new File(expectDir);
        assertTrue("期待値ディレクトリが存在しません。ディレクトリ：" + expectDir, expect.exists());
        final File actual = new File(actualDir);
        assertTrue("実績値ディレクトリが存在しません。ディレクトリ：" + actualDir, actual.exists());

        // 期待値ファイル配列
        final File[] expectFiles = expect.listFiles();
        // 実績値ファイル配列
        final File[] actualFiles = actual.listFiles();
        if (expectFiles == null & actualFiles == null) {
            // どちらもファイルが存在しない場合、終了
            System.out.println(MSG_BOTH_FILE_NOT_EXIST + "期待値ディレクトリ：" + expectDir + ", 実績値ディレクトリ：" + actualDir);
            return;
        } else if ( expectFiles == null & actualFiles != null ) {
            // 一方のみ存在する場合、失敗
            fail(MSG_ACTUAL_ONLY + actualDir);
        } else if ( expectFiles != null & actualFiles == null ) {
            // 一方のみ存在する場合、失敗
            fail(MSG_EXPECT_ONLY + expectDir);
        }

        // 比較済のファイル名リスト
        final List<String> comparedFileNameList = new ArrayList<String>();

        // 比較結果サマリー
        boolean isSame = true;
        // 期待値ファイル件数
        int expectFileCount = 0;
        // 実績値ファイル件数
        int actualFileCount = 0;
        // 相違件数
        int diffFileCount = 0;

        // 期待値ファイルを全件ループ
        for (int i = 0; i < expectFiles.length; i++) {

            // 期待値ファイルの確認
            final File curExpectFile = expectFiles[i];
            if (curExpectFile.isDirectory() || curExpectFile.isHidden()) {
                // ディレクトリ、隠しファイルの場合、スキップ
                continue;
            }

            // --------------------------------------------------
            //  期待値のみに存在するファイルの確認
            // --------------------------------------------------
            final File curActualFile = getActualFile(actualDir, curExpectFile);
            if (! curActualFile.exists()) {
                System.err.println(MSG_EXPECT_ONLY + curExpectFile);
                isSame = false;
                diffFileCount++;
                continue;
            }

            // --------------------------------------------------
            //  ファイル内容の確認
            // --------------------------------------------------
            // 内容比較
            if (! isSameFile(curExpectFile, curActualFile, charset)) {
                // 不一致の場合
                isSame = false;
                diffFileCount++;
                System.err.println(MSG_DIFF + curActualFile);
            }

            // 期待値件数のインクリメント
            expectFileCount++;

            // 比較済ファイル名リストに追加
            comparedFileNameList.add(curExpectFile.getName());
        }

        // --------------------------------------------------
        //  実績値のみに存在するファイルの確認
        // --------------------------------------------------
        for (final File curActualFile : actualFiles) {
            if (curActualFile.isDirectory() || curActualFile.isHidden()) {
                // ディレクトリ、隠しファイルの場合、スキップ
                continue;
            }

            // 比較していないことの確認
            if (! comparedFileNameList.contains(curActualFile.getName())) {
                // 比較していない場合
                isSame = false;
                diffFileCount++;
                System.err.println(MSG_ACTUAL_ONLY + curActualFile);
            }

            // 実績値件数のインクリメント
            actualFileCount++;
        }

        // --------------------------------------------------
        //  結果確認
        // --------------------------------------------------
        // ファイル数の確認
        assertEquals("ファイル数が一致していること", expectFileCount, actualFileCount);

        // ファイル比較結果
        assertTrue("全ファイルの比較結果が一致していること", isSame);
        assertEquals("全ファイルの比較結果が一致していること", 0, diffFileCount);
    }

    /**
     * 確認用の実績値ファイルオブジェクトを返します。
     *
     * @param actualDir 実績値ディレクトリ
     * @param curExpectFile 対応する期待値ファイル
     * @return 確認用の実績値ファイル
     */
    private static File getActualFile(final String actualDir, final File curExpectFile) {
        return new File(actualDir + File.separator + curExpectFile.getName());
    }

    /**
     * ファイル内容の一致を確認します。
     *
     * @param expectFilePath 期待値ファイルパス
     * @param actualFilePath 実績値ファイルパス
     * @param charset 文字コード
     */
    public static void assertFileEquals(final String expectFilePath, final String actualFilePath, final String charset) {
        // ファイルの存在確認
        final File expectFile = new File(expectFilePath);
        final File actualFile = new File(actualFilePath);

        final String msgFileInfo = "期待値ファイル：" + expectFilePath + ", 実績値ファイル：" + actualFilePath;
        if (expectFile.exists() && ! actualFile.exists()) {
            fail(MSG_EXPECT_ONLY + msgFileInfo);

        } else if (! expectFile.exists() && actualFile.exists()) {
            fail(MSG_ACTUAL_ONLY + msgFileInfo);

        } else if (! expectFile.exists() && ! actualFile.exists()) {
            fail(MSG_BOTH_NOT_EXIST + msgFileInfo);

        }

        // 内容比較
        if (! isSameFile(expectFile, actualFile, charset)) {
            fail(MSG_DIFF + msgFileInfo);
        }
    }

    /**
     * ファイル内容が一致しているか否かを返します。
     *
     * @param expectFile 期待値ファイル
     * @param actualFile 実績値ファイル
     * @param charset 文字コード
     * @return サイズ・文字列が一致している場合、true
     */
    private static boolean isSameFile(final File expectFile, final File actualFile, final String charset) {
        // サイズ比較
        final long expectSize = expectFile.getTotalSpace();
        final long actualSize = actualFile.getTotalSpace();
        if (expectSize != actualSize) {
            return false;
        }

        // 期待値ファイルコンテンツ
        List<String> expectLineList = null;
        try {
            expectLineList = FileUtils.readLines(expectFile, charset);
        } catch (Exception e) {
            throw new RuntimeException("期待値ファイル：" + expectFile.getName() + " の読み込みに失敗しました。", e);
        }

        // 実績値ファイルコンテンツ
        List<String> actualLineList = null;
        try {
            actualLineList = FileUtils.readLines(actualFile, charset);
        } catch (Exception e) {
            throw new RuntimeException("実績値ファイル：" + actualFile.getName() + " の読み込みに失敗しました。", e);
        }

        // 行数比較
        if (expectLineList.size() != actualLineList.size()) {
            System.err.println("・行数が異なります。");
            System.err.println("  ・expect:" + expectFile);
            System.err.println("    ・行数:" + expectLineList.size());
            System.err.println("  ・actual:" + actualFile);
            System.err.println("    ・行数:" + actualLineList.size());
            return false;
        }

        // 内容比較
        for (int idx = 0; idx < expectLineList.size(); idx++) {
            String expectLine = expectLineList.get(idx);
            String actualLine = actualLineList.get(idx);
            if (! expectLine.equals(actualLine)) {
                System.err.println("・ファイルの内容が異なります。行番号:" + (idx + 1));
                System.err.println("  ・expect:" + expectFile);
                System.err.println("    ・内容:" + expectLine);
                System.err.println("  ・actual:" + actualFile);
                System.err.println("    ・内容:" + actualLine);
                return false;
            }
        }

        return true;
    }

}
