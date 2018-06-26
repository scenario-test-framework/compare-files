package me.suwash.tools.comparefiles.infra.config;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.FileFormat;
import me.suwash.tools.comparefiles.infra.classification.LineSp;
import me.suwash.tools.comparefiles.infra.classification.RecordPattern;
import me.suwash.tools.comparefiles.infra.classification.RecordType;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.util.validation.constraints.Charset;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * ファイルレイアウト設定。
 */
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FileLayout {

    private static final int LIST_SIZE_DATA_ONLY = 1;

    private static final int LIST_SIZE_HEADER_DATA = 2;

    /** ファイル物理名正規表現。 */
    private String fileRegexPattern;

    /** ファイル論理名。 */
    @NotEmpty
    private String logicalFileName;

    /** ファイルフォーマット。 */
    @NotNull
    private FileFormat fileFormat;

    /** 文字コード。 */
    @NotEmpty
    @Charset
    private String charset;

    /** 改行コード。 */
    private LineSp lineSp;

    /** 行設定リスト。 */
    private List<RecordLayout> recordList;

    /** 画像比較用 比較除外エリアリスト。 */
    private List<Rectangle> ignoreAreaList;

    /**
     * デフォルトコンストラクタ。
     */
    public FileLayout() {}

    /**
     * インスタンスのコピーを返します。
     *
     * @param fileLayout コピー元
     * @return コピーしたFileLayout
     */
    public static FileLayout copy(final FileLayout fileLayout) {
        List recordList = fileLayout.recordList;
        if (recordList != null) recordList = Collections.unmodifiableList(recordList);

        List ignoreAreaList = fileLayout.ignoreAreaList;
        if (ignoreAreaList != null) recordList = Collections.unmodifiableList(ignoreAreaList);

        return new FileLayout(
                fileLayout.fileRegexPattern,
                fileLayout.logicalFileName,
                fileLayout.fileFormat,
                fileLayout.charset,
                fileLayout.lineSp,
                recordList,
                ignoreAreaList
        );
    }

    /**
     * レコードレイアウト1件目のバイト長を返します。
     *
     * @return レコードレイアウト1件目のバイト長
     */
    public int getFirstRecordByteLength() {
        if (recordList == null || recordList.isEmpty()) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"recordList"});
        }
        return recordList.get(0).getByteLength();
    }

    /**
     * レコードパターンを返します。
     *
     * @return レコードパターン
     */
    public RecordPattern getRecordPattern() {
        if (recordList == null || recordList.isEmpty()) {
            // ファイルレイアウトが設定されていない場合
            return RecordPattern.None;

        } else if (recordList.size() == LIST_SIZE_DATA_ONLY && RecordType.Data.equals(recordList.get(0).getType())) {
            // データレコードのみ定義されている場合
            return RecordPattern.DataOnly;

        } else if (recordList.size() == LIST_SIZE_HEADER_DATA) {
            boolean hasHeader = false;
            boolean hasData = false;
            for (final RecordLayout curRecordLayout : recordList) {
                if (RecordType.Header.equals(curRecordLayout.getType())) {
                    hasHeader = true;
                } else if (RecordType.Data.equals(curRecordLayout.getType())) {
                    hasData = true;
                }
            }
            if (hasHeader && hasData) {
                // ヘッダー/データレコードが定義されている場合
                return RecordPattern.HeaderData;
            }
        }

        // その他の場合
        return RecordPattern.Multi;
    }

}
