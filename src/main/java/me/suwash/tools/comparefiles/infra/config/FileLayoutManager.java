package me.suwash.tools.comparefiles.infra.config;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.FileFormat;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.util.CompareUtils.CompareCriteria;
import me.suwash.util.FileUtils;
import me.suwash.util.JsonUtils;

import org.apache.commons.lang3.StringUtils;

/**
 * ファイルレイアウト定義マネージャ。
 */
@lombok.extern.slf4j.Slf4j
public final class FileLayoutManager {

    /** インスタンス。 */
    private static final FileLayoutManager instance = new FileLayoutManager();

    /** ファイルレイアウト定義Map。 */
    private final Map<String, FileLayout> layoutMap = new TreeMap<String, FileLayout>();

    /**
     * Singletonパターンでインスタンスを返します。
     *
     * @return インスタンス
     */
    public static FileLayoutManager getInstance() {
        return instance;
    }

    /**
     * コンストラクタ。
     */
    private FileLayoutManager() {
        addLayoutClasspath();
    }

    /**
     * クラスパスから、デフォルトのファイルレイアウト定義ファイルを登録します。
     */
    private void addLayoutClasspath() {
        log.debug("  ・addLayoutClasspath");

        // 実行中のclasspath定義
        final String classpathes = System.getProperty("java.class.path");
        // 環境ごとのclasspath区切り文字
        final String pathSeparator = System.getProperty("path.separator");

        // classpathを全件ループ
        for (final String curClasspath : classpathes.split(pathSeparator)) {
            // 当該classpathがディレクトリの場合、含まれるレイアウト定義ファイルを全て登録
            addLayoutClasspath(curClasspath);
        }
    }

    /**
     * 指定クラスパス配下のレイアウト配置ディレクトリから、全てのレイアウトファイルを登録します。
     *
     * @param classpathStr クラスパス
     */
    private void addLayoutClasspath(final String classpathStr) {
        // サブディレクトリにFileLayout定義配置ディレクトリが存在するか確認
        final File classpath = new File(classpathStr);
        if (classpath.isDirectory()) {
            final File layoutDir = new File(classpath.getPath() + "/" + Const.CLASSPATH_LAYOUT_DIR_NAME);
            if (layoutDir.exists()) {
                // 存在する場合、配下の全てのファイルを定義追加
                addAllLayoutFile(layoutDir);
            }
        }
    }

    /**
     * 指定ディレクトリ配下にある全てのファイルを、ファイルレイアウト定義として上書き登録します。
     *
     * @param dirPath 対象のディレクトリパス
     */
    public void addLayoutDir(final String dirPath) {
        log.debug("  ・addLayoutDir(" + dirPath + ")");

        // 入力チェック
        if (StringUtils.isEmpty(dirPath)) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"dirPath"});
        }

        // ディレクトリ読み込みチェック
        FileUtils.readDirCheck(dirPath);

        // 指定ディレクトリ配下の全てのファイルを定義追加
        final File dir = new File(dirPath);
        addAllLayoutFile(dir);
    }

    /**
     * 指定ディレクトリ直下の全てのファイルをレイアウト登録します。
     *
     * @param dir 対象ディレクトリ
     */
    private void addAllLayoutFile(final File dir) {
        final File[] files = dir.listFiles();
        if (files != null) {
            for (final File curFile : files) {
                addLayoutFile(curFile.getPath());
            }
        }
    }

    /**
     * ファイルレイアウト定義ファイルを、上書き登録します。
     *
     * @param inputLayoutFilePath 対象のレイアウト定義ファイル
     */
    public void addLayoutFile(final String inputLayoutFilePath) {
        log.debug("  ・addLayoutFile(" + inputLayoutFilePath + ")");

        // 入力チェック
        if (StringUtils.isEmpty(inputLayoutFilePath)) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"inputLayoutFilePath"});
        }
        // ファイル読み込みチェック
        FileUtils.readCheck(inputLayoutFilePath, Const.CHARSET_DEFAULT_CONFIG);

        // パース
        final File inputLayoutFile = new File(inputLayoutFilePath);
        final FileLayoutList inputLayoutList = JsonUtils.parseFile(inputLayoutFile.getAbsolutePath(), Const.CHARSET_DEFAULT_CONFIG, FileLayoutList.class);

        // 指定ファイルの定義内容をループ
        for (final FileLayout inputLayout : inputLayoutList.getLayoutList()) {
            // 登録済み定義と一致する正規表現の場合、削除
            final String regex = inputLayout.getFileRegexPattern();
            if (layoutMap.containsKey(regex)) {
                layoutMap.remove(regex);
            }
            // 定義リストに追加
            log.trace("    ・[ADD]" + inputLayout.getLogicalFileName());
            layoutMap.put(regex, inputLayout);
        }
    }

    /**
     * 物理ファイル名にマッチするファイルレイアウトを返します。
     * マッチするレイアウトが定義されていない場合、nullを返します。
     *
     * @param physicalFileName 物理ファイル名
     * @param systemConfig システム設定
     * @return マッチするファイルレイアウト
     */
    public FileLayout getLayout(final String physicalFileName, final CompareFilesConfig systemConfig) {
        // 返却用レイアウト定義
        FileLayout returnLayout = null;

        // 定義リストを全件ループ
        for (final Map.Entry<String, FileLayout> curLayoutEntry : layoutMap.entrySet()) {
            // 定義の正規表現にマッチするか確認
            try {
                final Pattern pattern = Pattern.compile(curLayoutEntry.getKey());
                final Matcher matcher = pattern.matcher(physicalFileName);
                log.trace("        ・check regex:" + curLayoutEntry.getKey());
                if (matcher.matches()) {
                    // マッチした場合、返却用レイアウト定義に設定
                    log.info("      ・[MATCH  ]レイアウト:" + curLayoutEntry.getValue().getLogicalFileName() + ", 正規表現:" + curLayoutEntry.getKey());
                    returnLayout = curLayoutEntry.getValue();
                    break;
                }
            } catch (Exception e) {
                log.error("正規表現の評価でエラーが発生しました。レイアウト:" + curLayoutEntry.getValue().getLogicalFileName() + ", 正規表現:" + curLayoutEntry.getKey(), e);
            }
        }

        // マッチしなかった場合、nullを返却
        if (returnLayout == null) {
            log.warn("      ・[UNMATCH]ファイル名:" + physicalFileName);
            return null;
        }

        // systemConfigの存在確認
        if (systemConfig != null) {
            // 指定されている場合、返却用レイアウト定義に除外項目を適用
            FileLayoutManager.updateIgnore(systemConfig, returnLayout);
        }

        // 除外項目適用後の返却用レイアウト定義を返却
        return returnLayout;
    }

    /**
     * システム設定の除外項目リストを、ファイルレイアウトオブジェクトに反映します。
     *
     * @param systemConfig システム設定
     * @param fileLayout ファイルレイアウト
     */
    private static void updateIgnore(final CompareFilesConfig systemConfig, final FileLayout fileLayout) {
        final FileFormat fileFormat = fileLayout.getFileFormat();
        if (FileFormat.Image.equals(fileFormat)) {
            // 画像比較の場合、システム設定の除外エリアをマージ
            final List<Rectangle> mergedIgnoreAreaList = new ArrayList<Rectangle>();
            if (fileLayout.getIgnoreAreaList() != null) {
                mergedIgnoreAreaList.addAll(fileLayout.getIgnoreAreaList());
            }
            if (systemConfig.getIgnoreAreaList() != null) {
                mergedIgnoreAreaList.addAll(systemConfig.getIgnoreAreaList());
            }
            fileLayout.setIgnoreAreaList(mergedIgnoreAreaList);

        } else {
            // 除外項目リストを取得
            final List<String> ignoreItemList = systemConfig.getIgnoreItemList();
            if (ignoreItemList == null) {
                // 設定されていない場合、更新せずに終了
                return;
            }

            // 返却用レイアウト定義の項目を全件ループ
            for (final RecordLayout curRecordLayout : fileLayout.getRecordList()) {
                for (final ItemLayout curItemLayout : curRecordLayout.getItemList()) {
                    // 一致する場合、criteriaを除外に設定
                    if (FileLayoutManager.isIgnoreItem(ignoreItemList, curItemLayout)) {
                        curItemLayout.setCriteria(CompareCriteria.Ignore);
                    }
                }
            }
        }
    }

    /**
     * 指定項目が、除外対象か否かを返します。
     *
     * @param ignoreItemList 除外項目リスト
     * @param itemLayout 対象項目レイアウト
     * @return 除外対象の場合、true
     */
    private static boolean isIgnoreItem(final List<String> ignoreItemList, final ItemLayout itemLayout) {
        boolean isIgnore = false;
        // 除外項目リストをループ
        for (final String ignoreItemName : ignoreItemList) {
            // 項目名の一致を確認
            if (ignoreItemName.equals(itemLayout.getId())) {
                isIgnore = true;
                break;
            }
        }
        return isIgnore;
    }

    /**
     * テキストファイルのデフォルトファイルレイアウトを返します。
     * <pre>
     * 論理名　　　：-
     * ファイル形式：テキスト形式
     * 文字コード　：UTF8
     * 改行コード　：システム
     * </pre>
     *
     * @return デフォルトのファイルレイアウト。
     */
    public static FileLayout getDefaultTextLayout() {
        final FileLayout layout = new FileLayout();
        layout.setLogicalFileName(Const.DUMMY_VALUE);
        layout.setFileFormat(FileFormat.Text);
        layout.setCharset(Const.CHARSET_DEFAULT_CONFIG);
        layout.setLineSp(null);
        return layout;
    }

    /**
     * 画像ファイルのデフォルトファイルレイアウトを返します。
     * <pre>
     * 論理名　　　：Image
     * ファイル形式：画像形式
     * 除外エリア　：システム設定を引き継ぐ
     * </pre>
     *
     * @param systemConfig システム設定
     * @return デフォルトのファイルレイアウト。
     */
    public static FileLayout getDefaultImageLayout(final CompareFilesConfig systemConfig) {
        final FileLayout layout = new FileLayout();
        layout.setLogicalFileName(Const.DUMMY_VALUE);
        layout.setFileFormat(FileFormat.Image);
        FileLayoutManager.updateIgnore(systemConfig, layout);
        return layout;
    }

}
