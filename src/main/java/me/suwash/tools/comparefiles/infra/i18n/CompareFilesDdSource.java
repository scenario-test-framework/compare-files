package me.suwash.tools.comparefiles.infra.i18n;

import me.suwash.ddd.i18n.DddDdSource;
import me.suwash.util.i18n.DdSource;

/**
 * データディクショナリ用プロパティファイルの定義保持クラス。
 */
public class CompareFilesDdSource extends DdSource {
    private static CompareFilesDdSource instance = new CompareFilesDdSource();

    /**
     * Singletonパターンでインスタンスを返します。
     *
     * @return インスタンス
     */
    public static CompareFilesDdSource getInstance() {
        return instance;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.util.i18n.DdSource#getParent()
     */
    @Override
    protected DdSource getParent() {
        return DddDdSource.getInstance();
    }
}
