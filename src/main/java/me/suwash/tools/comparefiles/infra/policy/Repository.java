package me.suwash.tools.comparefiles.infra.policy;

/**
 * データアクセスリポジトリ。
 */
public interface Repository {

    /**
     * トランザクションを開始します。
     */
    void begin();

    /**
     * トランザクションを確定します。
     */
    void commit();

    /**
     * トランザクションをロールバックします。
     */
    void rollback();
}
