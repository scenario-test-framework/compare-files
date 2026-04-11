package me.suwash.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;

import me.suwash.util.constant.UtilMessageConst;
import me.suwash.util.exception.UtilException;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * JSON関連ユーティリティ。
 */
public final class JsonUtils {

    /** メッセージ出力用データ文字列長。 */
    private static final int PART_CONTENT_LENGTH = 200;
    /** JSONオブジェクトマッパー。 */
    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * ユーティリティ向けに、コンストラクタはprivate宣言。
     */
    private JsonUtils() {}

    static {
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));// ISO8601
    }

    /**
     * JSONファイルのオブジェクト変換。
     *
     * @param <T> 対象クラス
     * @param filePath ファイルパス
     * @param charset 文字コード
     * @param type 対象クラス
     * @return 変換後のオブジェクト
     */
    public static <T> T parseFile(final String filePath, final String charset, final Class<T> type) {
        //--------------------------------------------------
        // 事前処理
        //--------------------------------------------------
        // ファイル共通チェック
        FileUtils.readCheck(filePath, charset);

        // 変換先クラス
        if (type == null) {
            throw new UtilException(UtilMessageConst.CHECK_NOTNULL, new Object[] {"type"});
        }

        //--------------------------------------------------
        // 本処理
        //--------------------------------------------------
        try {
            final File file = new File(filePath);
            final Reader reader = new InputStreamReader(new FileInputStream(file), charset);
            return mapper.readValue(reader, type);

        } catch (Exception e) {
            throw new UtilException(UtilMessageConst.ERRORHANDLE, new Object[] {
                JsonUtils.class.getName() + ".parseFile",
                "file=" + filePath + ", charset=" + charset + ", type=" + type.getSimpleName()
            }, e);
        }
    }

    /**
     * JSONファイルのオブジェクト変換（クラスパス指定）。
     *
     * @param <T> 対象クラス
     * @param filePath ファイルパス（クラスパス）
     * @param charset 文字コード
     * @param type 対象クラス
     * @return 変換後のオブジェクト
     */
    public static <T> T parseFileByClasspath(final String filePath, final String charset, final Class<T> type) {
        //--------------------------------------------------
        // 事前処理
        //--------------------------------------------------
        // ファイル共通チェック（クラスパス）
        FileUtils.readCheckByClasspath(filePath, charset, type);

        // 変換先クラス
        if (type == null) {
            throw new UtilException(UtilMessageConst.CHECK_NOTNULL, new Object[] {"type"});
        }

        //--------------------------------------------------
        // 本処理
        //--------------------------------------------------
        try {
            final Reader reader = new InputStreamReader(type.getResourceAsStream(filePath), charset);
            return mapper.readValue(reader, type);

        } catch (Exception e) {
            throw new UtilException(UtilMessageConst.ERRORHANDLE, new Object[] {
                JsonUtils.class.getName() + ".parseFileByClasspath",
                "file=" + filePath + ", charset=" + charset + ", type=" + type.getSimpleName()
            }, e);
        }
    }

    /**
     * JSON文字列のオブジェクト変換。
     *
     * @param <T> 対象クラス
     * @param target JSON文字列
     * @param type 対象クラス
     * @return 変換後のオブジェクト
     */
    public static <T> T parseString(final String target, final Class<T> type) {
        //--------------------------------------------------
        // 事前処理
        //--------------------------------------------------
        // 変換先クラス
        if (type == null) {
            throw new UtilException(UtilMessageConst.CHECK_NOTNULL, new Object[] {"type"});
        }

        //--------------------------------------------------
        // 本処理
        //--------------------------------------------------
        if (StringUtils.isEmpty(target)) {
            return null;
        }

        try {
            return mapper.readValue(target, type);
        } catch (Exception e) {
            String partTarget = target;
            if (target.length() > PART_CONTENT_LENGTH) {
                partTarget = target.substring(0, PART_CONTENT_LENGTH);
            }
            throw new UtilException(UtilMessageConst.ERRORHANDLE, new Object[] {
                JsonUtils.class.getName() + ".parseString",
                "target(part)=" + partTarget + ", type=" + type.getSimpleName()
            }, e);
        }
    }

    /**
     * オブジェクトのJSON文字列変換。
     *
     * @param target 対象オブジェクト
     * @return JSON形式の文字列
     */
    public static String writeString(final Object target) {
        if (target == null) {
            return null;
        }

        try {
            return mapper.writeValueAsString(target);
        } catch (Exception e) {
            throw new UtilException(UtilMessageConst.ERRORHANDLE, new Object[] {
                JsonUtils.class.getName() + ".writeString",
                "target=" + target
            }, e);
        }
    }

    /**
     * オブジェクトの整形済みJSON文字列変換。
     *
     * @param target 対象オブジェクト
     * @return JSON形式の文字列
     */
    public static String writePrettyString(final Object target) {
        if (target == null) {
            return null;
        }

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(target);
        } catch (Exception e) {
            throw new UtilException(UtilMessageConst.ERRORHANDLE, new Object[] {
                JsonUtils.class.getName() + ".writePrettyString",
                "target=" + target
            }, e);
        }
    }

}
