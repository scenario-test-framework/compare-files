package me.suwash.tools.comparefiles.infra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import me.suwash.tools.comparefiles.infra.exception.Errors;
import me.suwash.tools.comparefiles.infra.i18n.CompareFilesMessageSource;
import me.suwash.tools.comparefiles.infra.policy.Repository;

/**
 * システムコンテキスト。
 */
@lombok.extern.slf4j.Slf4j
public final class Context {

    /** Singletonパターン。 */
    private static final Context instance = new Context();

    /** チェックエラー。 */
    @Getter
    private Errors errors;

    @Getter
    private Map<String, Repository> repositoryMap = new HashMap<String, Repository>();

    /**
     * Singletonパターンでインスタンスを返します。
     *
     * @return インスタンス
     */
    public static Context getInstance() {
        return instance;
    }

    /**
     * コンストラクタ。
     */
    private Context() {}

    /**
     * チェックエラーを設定します。
     *
     * @param errors チェックエラー
     */
    public void setErrors(final Errors errors) {
        // チェックエラーを含む場合
        if (errors != null && errors.size() != 0) {
            // エラー内容を保持
            this.errors = errors;
        }
    }

    /**
     * UT用。チェックエラーをクリアします。
     */
    @Deprecated
    public void clearErrors() {
        if (this.errors != null) {
            errors = null;
        }
    }

    /**
     * コンテキストにリポジトリを追加します。
     *
     * @param id インスタンスごとに一意なリポジトリID
     * @param repository リポジトリ
     */
    public void addRepository(final String id, final Repository repository) {
        if (repositoryMap.containsKey(id)) {
            // キーが重複する場合、登録済みリポジトリをロールバックして、上書き
            log.warn(CompareFilesMessageSource.getInstance().getMessage(
                Const.DATA_DUPLICATE,
                new Object[] {"repositoryMap", "key", id}));
            Repository beforeRepo = repositoryMap.get(id);
            beforeRepo.rollback();
            repositoryMap.remove(id);
        }
        repositoryMap.put(id, repository);
    }

    /**
     * コンテキストに登録されているリポジトリリストを返します。
     *
     * @return コンテキストに登録されているリポジトリリスト
     */
    public List<Repository> getRepositoryList() {
        List<Repository> repositoryList = new ArrayList<Repository>();
        for (final Repository curRepo : repositoryMap.values()) {
            repositoryList.add(curRepo);
        }
        return repositoryList;
    }

    /**
     * コンテキストから、リポジトリを削除します。
     *
     * @param id インスタンスごとに一意なリポジトリID
     */
    public void removeRepository(final String id) {
        if (repositoryMap.containsKey(id)) {
            repositoryMap.remove(id);
        }
    }
}
