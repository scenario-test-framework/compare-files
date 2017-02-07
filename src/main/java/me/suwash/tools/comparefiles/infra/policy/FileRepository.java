package me.suwash.tools.comparefiles.infra.policy;

/**
 * ファイルリポジトリ。
 *
 * @param <E> データモデル
 */
public interface FileRepository<E> extends Repository {

    /**
     * 次の行を読み込みます。
     *
     * @return 行データ
     */
    E next();

    /**
     * 行データを書き出します。
     *
     * @param row 行データ
     */
    void write(E row);
}
