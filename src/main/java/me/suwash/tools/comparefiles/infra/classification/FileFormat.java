package me.suwash.tools.comparefiles.infra.classification;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import me.suwash.util.classification.Classification;

/**
 * ファイルフォーマット。
 */
public enum FileFormat implements Classification {
    /** プレーンテキスト。 */
    Text("TXT"),
    /** 項目名付きCSV。 */
    CSV_withHeader("CSVH"),
    /** CSV。 */
    CSV_noHeader("CSV"),
    /** 項目名付きTSV。 */
    TSV_withHeader("TSVH"),
    /** TSV。 */
    TSV_noHeader("TSV"),
    /** JSON。 */
    Json("JSN"),
    /** 改行区切りのJSONリスト。 */
    JsonList("JSNL"),
    /** YAML。 */
    Yaml("YML"),
    /** XML。 */
    XML("XML"),
    /** 固定長テキスト。 */
    Fixed("FIX"),
    /** 画像。 */
    Image("IMG");

    /** グループ名配列。 */
    private static final String[] groups;
    /** 区分値グループMap。 */
    private static final Map<String, FileFormat[]> groupValuesMap;
    /** グループ内デフォルト区分値Map。 */
    private static final Map<String, FileFormat> groupDefaultMap;

    /** グループ：デフォルト。 */
    public static final String GROUP_DEFAULT = "default";
    /** グループ：テキスト。 */
    public static final String GROUP_TEXT = "text";
    /** グループ：画像。 */
    public static final String GROUP_IMAGE = "image";

    /** データディクショナリID。 */
    private String ddId;
    /** 永続化値。 */
    private String storeValue;

    static {
        // グループMap
        groupValuesMap = new HashMap<String, FileFormat[]>();
        groupValuesMap.put(GROUP_DEFAULT, new FileFormat[]{
            Text,
            CSV_withHeader,
            CSV_noHeader,
            TSV_withHeader,
            TSV_noHeader,
            Json,
            JsonList,
            Yaml,
            XML,
            Fixed,
            Image
            });
        groupValuesMap.put(GROUP_TEXT, new FileFormat[]{
            Text,
            CSV_withHeader,
            CSV_noHeader,
            TSV_withHeader,
            TSV_noHeader,
            Json,
            JsonList,
            Yaml,
            XML,
            Fixed
            });
        groupValuesMap.put(GROUP_IMAGE, new FileFormat[]{
            Image
            });

        // グループ内デフォルト値Map
        groupDefaultMap = new HashMap<String, FileFormat>();
        groupDefaultMap.put(GROUP_DEFAULT, Text);
        groupDefaultMap.put(GROUP_TEXT, Text);
        groupDefaultMap.put(GROUP_IMAGE, Image);

        // グループ名配列
        groups = groupValuesMap.keySet().toArray(new String[0]);
    }

    /**
     * デフォルト区分値を返します。
     *
     * @return デフォルト区分値
     */
    public static FileFormat defaultValue() {
        return groupDefaultMap.get(GROUP_DEFAULT);
    }

    /**
     * グループ内のデフォルト区分値を返します。
     *
     * @param group グループ名
     * @return デフォルトの区分値
     */
    public static FileFormat defaultValue(final String group) {
        return groupDefaultMap.get(group);
    }

    /**
     * 区分が持つグループ群を返します。
     *
     * @return グループ名配列
     */
    public static String[] groups() {
        return Arrays.copyOf(groups, groups.length);
    }

    /**
     * 指定したグループ名に属する区分値を返します。
     *
     * @param group グループ名
     * @return 区分値配列
     */
    public static FileFormat[] values(final String group) {
        return groupValuesMap.get(group);
    }

    /**
     * データディクショナリIDから区分値を返します。
     * 見つからない場合はnullを返します。
     *
     * @param ddId データディクショナリID
     * @return 区分値
     */
    public static FileFormat valueOfByDdId(final String ddId) {
        for (final FileFormat curEnum : FileFormat.values()) {
            if (curEnum.ddId().equals(ddId)) {
                return curEnum;
            }
        }
        return null;
    }

    /**
     * 永続化値から区分値を返します。
     * 見つからない場合はnullを返します。
     *
     * @param storeValue 永続化値
     * @return 区分値
     */
    public static FileFormat valueOfByStoreValue(final String storeValue) {
        for (final FileFormat curEnum : FileFormat.values()) {
            if (curEnum.storeValue().equals(storeValue)) {
                return curEnum;
            }
        }
        return null;
    }

    /**
     * 区分内に、指定した区分値名が存在するか確認します。
     *
     * @param name 区分値名
     * @return 存在する場合 true
     */
    public static boolean containsName(final String name) {
        for (final FileFormat curEnum : FileFormat.values()) {
            if (curEnum.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 区分内の、指定したグループに、指定した区分値名が存在するか確認します。
     *
     * @param group グループ名
     * @param name 区分値名
     * @return 存在する場合 true
     */
    public static boolean containsName(final String group, final String name) {
        for (final FileFormat curEnum : FileFormat.values(group)) {
            if (curEnum.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 区分内に、指定したデータディクショナリIDが存在するか確認します。
     *
     * @param ddId データディクショナリID
     * @return 存在する場合 true
     */
    public static boolean containsDdId(final String ddId) {
        for (final FileFormat curEnum : FileFormat.values()) {
            if (curEnum.ddId().equals(ddId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 区分内の、指定したグループに、指定したデータディクショナリIDが存在するか確認します。
     *
     * @param group グループ名
     * @param ddId データディクショナリID
     * @return 存在する場合 true
     */
    public static boolean containsDdId(final String group, final String ddId) {
        for (final FileFormat curEnum : FileFormat.values(group)) {
            if (curEnum.ddId().equals(ddId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 区分内に、指定した永続化値が存在するか確認します。
     *
     * @param storeValue 永続化値
     * @return 存在する場合 true
     */
    public static boolean containsStoreValue(final String storeValue) {
        for (final FileFormat curEnum : FileFormat.values()) {
            if (curEnum.storeValue().equals(storeValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 区分内の、指定したグループに、指定した永続化値が存在するか確認します。
     *
     * @param group グループ名
     * @param storeValue 永続化値
     * @return 存在する場合 true
     */
    public static boolean containsStoreValue(final String group, final String storeValue) {
        for (final FileFormat curEnum : FileFormat.values(group)) {
            if (curEnum.storeValue().equals(storeValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * コンストラクタ。
     *
     * @param storeValue 永続化値
     */
    private FileFormat(final String storeValue) {
        this.ddId = this.getClass().getSimpleName() + "." + name();
        this.storeValue = storeValue;
    }

    /* (非 Javadoc)
     * @see me.suwash.util.classification.Classification#ddId()
     */
    @Override
    public String ddId() {
        return ddId;
    }

    /* (非 Javadoc)
     * @see me.suwash.util.classification.Classification#storeValue()
     */
    @Override
    public String storeValue() {
        return storeValue;
    }

    /* (非 Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return ddId() + "(" + storeValue() + ")";
    }
}

