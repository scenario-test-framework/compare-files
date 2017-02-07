package me.suwash.tools.comparefiles.infra.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.util.CsvUtils;
import me.suwash.util.FileUtils;

import org.apache.commons.lang3.StringUtils;

/**
 * ファイル名正規表現指定の比較対象設定リスト。
 */
@lombok.extern.slf4j.Slf4j
public class CompareRegexTargetList implements Iterable<CompareRegexTarget> {

    private static final int REGEX_TARGET_COLNUM_COUNT = 3;

    private final List<CompareRegexTarget> list = new ArrayList<CompareRegexTarget>();

    /**
     * コンストラクタ。
     *
     * @param filePath 比較対象設定ファイルパス
     * @param charset 文字コード
     */
    public CompareRegexTargetList(final String filePath, final String charset) {
        log.debug("CompareRegexTargetList(" + filePath + ", " + charset + ")");

        // 引数チェック
        FileUtils.readCheck(filePath, charset);

        // CSVを全行ループ
        final List<String[]> parsedStringsList = CsvUtils.parseFile(filePath, charset, CsvUtils.getCsvConfig());
        for (int i = 0; i < parsedStringsList.size(); i++) {
            final String[] parsedStrings = parsedStringsList.get(i);
            // 空行をスキップ
            if (parsedStrings == null || parsedStrings.length == 1 && StringUtils.isEmpty(parsedStrings[0])) {
                continue;
            }

            // #で始まる行をスキップ
            if (parsedStrings.length > 0 && parsedStrings[0].charAt(0) == '#') {
                continue;
            }

            // レイアウト確認
            if (parsedStrings.length != REGEX_TARGET_COLNUM_COUNT) {
                throw new CompareFilesException(
                    Const.MSGCD_ERROR_FILE_LAYOUT,
                    new Object[] {filePath, charset, i + 1, getArrayString(parsedStrings)});
            }

            // オブジェクト変換
            try {
                list.add(getCompareTarget(parsedStrings));
            } catch (Exception e) {
                throw new CompareFilesException(
                    Const.MSGCD_ERROR_FILE_PARSE,
                    new Object[] {filePath, charset, i + 1, getArrayString(parsedStrings)},
                    e);
            }
        }

    }

    /**
     * ファイル1行分の文字列リストから比較対象オブジェクトに変換します。
     *
     * @param parsedStrings ファイル1行分の文字列リスト
     * @return 比較対象オブジェクト
     */
    private CompareRegexTarget getCompareTarget(final String... parsedStrings) {
        return new CompareRegexTarget(parsedStrings[0], parsedStrings[1], parsedStrings[2]);
    }

    /**
     * 文字列配列の文字列表現を返します。
     *
     * @param array 文字列配列
     * @return 文字列表現
     */
    private String getArrayString(final String... array) {
        final StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (final String elem : array) {
            sb.append(elem).append(',');
        }
        if (array.length != 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(']');

        return sb.toString();
    }

    /*
     * (非 Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<CompareRegexTarget> iterator() {
        return list.iterator();
    }
}
