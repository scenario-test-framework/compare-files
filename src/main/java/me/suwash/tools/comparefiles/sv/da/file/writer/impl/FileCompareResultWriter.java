package me.suwash.tools.comparefiles.sv.da.file.writer.impl;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.domain.compare.file.FileCompareResult;
import me.suwash.util.CompareUtils.CompareStatus;
import me.suwash.util.CsvUtils;

import com.orangesignal.csv.CsvWriter;
import com.orangesignal.csv.annotation.CsvColumn;
import com.orangesignal.csv.annotation.CsvEntity;
import com.orangesignal.csv.io.CsvEntityWriter;

/**
 * ファイル比較結果Writer。
 */
public class FileCompareResultWriter extends BaseResultWriter<FileCompareResult> {

    /**
     * コンストラクタ。
     *
     * @param filePath 出力ファイルパス
     * @param charset 出力文字コード
     * @throws IOException ファイルにアクセスできない場合
     */
    public FileCompareResultWriter(final String filePath, final String charset) throws IOException {
        super(filePath, charset);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.writer.impl.BaseResultWriter#getWriter(java.io.File, java.lang.String)
     */
    @Override
    protected Closeable getWriter(final File file, final String charset) {
        CsvEntityWriter<FileCompareResultOutput> writer = null;
        try {
            writer = new CsvEntityWriter<FileCompareResultOutput>(
                new CsvWriter(
                    new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(file), charset)
                    ),
                    CsvUtils.getCsvConfig()
                ),
                FileCompareResultOutput.class
                );
        } catch (Exception e) {
            throw new CompareFilesException(Const.STREAM_CANTOPEN_OUTPUT, new Object[] {filePath}, e);
        }
        return writer;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.writer.impl.BaseResultWriter#writeRow(java.lang.Object)
     */
    @Override
    protected void writeRow(final FileCompareResult result) {
        if (result == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        final CsvEntityWriter<FileCompareResultOutput> csvWriter = (CsvEntityWriter<FileCompareResultOutput>) writer;
        try {
            csvWriter.write(
                new FileCompareResultOutput(
                    result.getStatus(),
                    result.getLeftFilePath(),
                    result.getRightFilePath(),
                    result.getFileLayout(),
                    result.getRowCount(),
                    result.getOkRowCount(),
                    result.getNgRowCount(),
                    result.getIgnoreRowCount(),
                    result.getLeftOnlyRowCount(),
                    result.getRightOnlyRowCount(),
                    result.getStartTime(),
                    result.getEndTime(),
                    result.getLength()
                ));
        } catch (IOException e) {
            throw new CompareFilesException(
                Const.FILE_CANTWRITE,
                new Object[] {filePath + "#" + outRowNum + "、result：" + result},
                e);
        }
    }

    /**
     * 出力ファイルレイアウト。
     */
    @CsvEntity(header = true)
    private static class FileCompareResultOutput {

        /** 比較ステータス。 */
        @CsvColumn(position = 0, name = "Status")
        private final String status;

        /** 比較対象.左。 */
        @CsvColumn(position = 1, name = "Left")
        private final String left;

        /** 比較対象.右。 */
        @CsvColumn(position = 2, name = "Right")
        private final String right;

        /** レイアウト名。 */
        @CsvColumn(position = 3, name = "Layout")
        private final String fileLayout;

        /** 比較行数。 */
        @CsvColumn(position = 4, name = "Row", format = Const.FORMAT_COUNT)
        private final long rowCount;

        /** 比較結果が「OK」の行数。 */
        @CsvColumn(position = 5, name = "OK Row", format = Const.FORMAT_COUNT)
        private final long okRowCount;

        /** 比較結果が「NG」の行数。 */
        @CsvColumn(position = 6, name = "NG Row", format = Const.FORMAT_COUNT)
        private final long ngRowCount;

        /** 比較結果が「除外」の行数。 */
        @CsvColumn(position = 7, name = "Ignore Row", format = Const.FORMAT_COUNT)
        private final long ignoreRowCount;

        /** 比較結果が「左のみ」の行数。 */
        @CsvColumn(position = 8, name = "LeftOnly Row", format = Const.FORMAT_COUNT)
        private final long leftOnlyRowCount;

        /** 比較結果が「右のみ」の行数。 */
        @CsvColumn(position = 9, name = "RightOnly Row", format = Const.FORMAT_COUNT)
        private final long rightOnlyRowCount;

        /** 処理開始時刻。 */
        @CsvColumn(position = 10, name = "StartTime", format = Const.FORMAT_TIMESTAMP)
        private Date startTime;

        /** 処理終了時刻。 */
        @CsvColumn(position = 11, name = "EndTime", format = Const.FORMAT_TIMESTAMP)
        private Date endTime;

        /** 処理時間。 */
        @CsvColumn(position = 12, name = "Length")
        private final String length;

        /**
         * コンストラクタ。
         *
         * @param status 比較ステータス
         * @param left 比較対象.左
         * @param right 比較対象.右
         * @param fileLayout ファイルレイアウト
         * @param rowCount 比較行数
         * @param okRowCount 比較結果が「OK」の行数
         * @param ngRowCount 比較結果が「NG」の行数
         * @param ignoreRowCount 比較結果が「除外」の行数
         * @param leftOnlyRowCount 比較結果が「左のみ」の行数
         * @param rightOnlyRowCount 比較結果が「右のみ」の行数
         * @param startTime 処理開始時刻
         * @param endTime 処理終了時刻
         * @param length 処理時間
         */
        public FileCompareResultOutput(
            final CompareStatus status,
            final String left,
            final String right,
            final FileLayout fileLayout,
            final long rowCount,
            final long okRowCount,
            final long ngRowCount,
            final long ignoreRowCount,
            final long leftOnlyRowCount,
            final long rightOnlyRowCount,
            final Date startTime,
            final Date endTime,
            final long length) {

            this.status = convStatus(status);
            this.left = left;
            this.right = right;
            this.fileLayout = convFileLayout(fileLayout);
            this.rowCount = rowCount;
            this.okRowCount = okRowCount;
            this.ngRowCount = ngRowCount;
            this.ignoreRowCount = ignoreRowCount;
            this.leftOnlyRowCount = leftOnlyRowCount;
            this.rightOnlyRowCount = rightOnlyRowCount;
            if (startTime != null) {
                this.startTime = (Date) startTime.clone();
            }
            if (endTime != null) {
                this.endTime = (Date) endTime.clone();
            }
            this.length = convLength(length);

        }

        /**
         * 比較ステータスを出力フォーマットに変換します。
         *
         * @param status 比較ステータス
         * @return 比較ステータス出力文言
         */
        private String convStatus(final CompareStatus status) {
            return status.toString();
        }

        /**
         * ファイルレイアウトを出力フォーマットに変換します。
         *
         * @param fileLayout ファイルレイアウト
         * @return ファイルレイアウト出力文言
         */
        private String convFileLayout(final FileLayout fileLayout) {
            if (fileLayout == null) {
                return Const.DUMMY_VALUE;
            } else {
                return fileLayout.getLogicalFileName();
            }
        }

        /**
         * 処理時間を出力フォーマットに変換します。
         *
         * @param lengthMs 処理時間（ミリ秒）
         * @return 処理時間出力文言
         */
        private String convLength(final long lengthMs) {
            final long totalSeconds = lengthMs / 1000;
            final long milliseconds = lengthMs % 1000;
            final long seconds = totalSeconds % 60;
            final long minutes = (totalSeconds / 60) % 60;
            final long hours = totalSeconds / 3600;

            return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
        }

    }
}
