package me.suwash.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;

import lombok.Getter;
import lombok.Setter;
import me.suwash.util.constant.UtilMessageConst;
import me.suwash.util.exception.UtilException;

/**
 * 画像比較ユーティリティ。
 */
public final class ImageCompareUtils {

    private static List<String> allowedExtList;
    private static Font font;
    private static BufferedImage arrowImage;

    /**
     * コンストラクタ。
     */
    private ImageCompareUtils() {}

    /*
     * staticイニシャライザ。
     */
    static {
        // 利用可能なImageReaderのSPIから、対応しているファイル拡張子を集める
        allowedExtList = new ArrayList<String>();
        IIORegistry registry = IIORegistry.getDefaultInstance();
        Iterator<ImageReaderSpi> spiItr = registry.getServiceProviders(ImageReaderSpi.class, false);
        while (spiItr.hasNext()) {
            ImageReaderSpi spi = spiItr.next();
            allowedExtList.addAll(Arrays.asList(spi.getFileSuffixes()));
        }
    }

    /**
     * 比較に対応している拡張子リストを返します。
     *
     * @return 比較に対応している拡張子リスト
     */
    public static List<String> getAllowedExtList() {
        return Collections.unmodifiableList(allowedExtList);
    }

    /**
     * 比較に対応している拡張子か確認します。
     *
     * @param ext 確認対象の拡張子
     * @return 対応している場合、true
     */
    public static boolean isAllowedExt(final String ext) {
        return allowedExtList.contains(ext.toLowerCase(Locale.getDefault()));
    }

    /**
     * 2つの画像を比較します。
     *
     * @param leftImage 左画像
     * @param rightImage 右画像
     * @param ignoreAreaList 除外エリアリスト
     * @return 比較結果
     */
    public static DiffAreas compare(
        final BufferedImage leftImage,
        final BufferedImage rightImage,
        final List<Rectangle> ignoreAreaList) {

        // 除外エリアが指定されている場合、左右の画像にマスクをかける
        final BufferedImage maskedLeftImage;
        final BufferedImage maskedRightImage;
        if (ignoreAreaList != null && ! ignoreAreaList.isEmpty()) {
            final float alpha = 1.0f;
            maskedLeftImage = getMaskedImage(leftImage, ignoreAreaList, alpha);
            maskedRightImage = getMaskedImage(rightImage, ignoreAreaList, alpha);

        } else {
            maskedLeftImage = leftImage;
            maskedRightImage = rightImage;
        }

        // 画像全体を比較
        return compare(maskedLeftImage, null, maskedRightImage, null);
    }

    /**
     * 指定エリアをマスクした画像を生成します。
     *
     * @param srcImage 対象画像
     * @param maskAreas マスクするエリア
     * @param alpha マスクの透過度
     * @return 指定エリアをマスクした画像
     */
    private static BufferedImage getMaskedImage(final BufferedImage srcImage, final List<Rectangle> maskAreas, final float alpha) {
        if (srcImage == null) {
            return null;
        }

        BufferedImage maskedImage = deepCopy(srcImage);

        Graphics graphics = maskedImage.getGraphics();
        graphics.setColor(new Color(1.0f, 1.0f, 1.0f, alpha));

        for (final Rectangle rectangle : maskAreas) {
            // fillRectのパラメータはintなので、Rectangleの内部オブジェクトからプロパティにアクセス
            final Point location = rectangle.getLocation();
            final Dimension size = rectangle.getSize();
            graphics.fillRect(location.x, location.y, size.width, size.height);
        }
        graphics.dispose();

        return maskedImage;
    }

    /**
     * 画像をDeepCopyします。
     *
     * @param image コピー元の画像
     * @return コピーした画像
     */
    private static BufferedImage deepCopy(final BufferedImage image) {
        final ColorModel colorModel = image.getColorModel();
        final boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();
        final WritableRaster raster = image.copyData(null);
        final WritableRaster rasterChild = (WritableRaster) raster.createChild(
            0, 0, image.getWidth(), image.getHeight(), image.getMinX(), image.getMinY(), null);
        return new BufferedImage(colorModel, rasterChild, isAlphaPremultiplied, null);
    }

    /**
     * 2つの画像の指定範囲を比較します。
     *
     * @param leftSrcImage 左画像
     * @param leftTargetArea 左画像の比較範囲
     * @param rightSrcImage 右画像
     * @param rightTargetArea 右画像の比較範囲
     * @return 比較結果
     */
    public static DiffAreas compare(
        final BufferedImage leftSrcImage,
        final Rectangle leftTargetArea,
        final BufferedImage rightSrcImage,
        final Rectangle rightTargetArea) {

        if (leftSrcImage == null || rightSrcImage == null) {
            throw new UtilException(UtilMessageConst.CHECK_NOTNULL, new Object[] {
                "leftImage and rightImage"
            });
        }

        int offsetX = 0;
        int offsetY = 0;
        BufferedImage leftImage = leftSrcImage;
        if (leftTargetArea != null) {
            leftImage = getSubImage(leftSrcImage, leftTargetArea);
            offsetX = leftTargetArea.getLocation().x;
            offsetY = leftTargetArea.getLocation().y;
        }

        BufferedImage rightImage = rightSrcImage;
        if (rightTargetArea != null) {
            rightImage = getSubImage(rightSrcImage, rightTargetArea);
        }

        // 比較
        final List<Point> sizeDiffPoints = compareSize(leftImage, rightImage, offsetX, offsetY);
        final List<Point> diffPoints = compareContent(leftImage, rightImage, offsetX, offsetY);
        return new DiffAreas(diffPoints, sizeDiffPoints);
    }

    /**
     * 画像サイズを比較します。
     *
     * @param leftImage 左画像
     * @param rightImage 右画像
     * @param offsetX X方向オフセット値
     * @param offsetY Y方向オフセット値
     * @return 差分座標リスト
     */
    private static List<Point> compareSize(
        final BufferedImage leftImage,
        final BufferedImage rightImage,
        final int offsetX,
        final int offsetY) {

        final int leftWidth = leftImage.getWidth();
        final int leftHeight = leftImage.getHeight();
        final int rightWidth = rightImage.getWidth();
        final int rightHeight = rightImage.getHeight();

        if (leftWidth == rightWidth && leftHeight == rightHeight) {
            return new ArrayList<Point>();
        }

        final int diffStartX;
        final int diffEndX;
        if (leftWidth > rightWidth) {
            diffStartX = offsetX + rightWidth + 1;
            diffEndX = offsetX + leftWidth;
        } else if (leftWidth < rightWidth) {
            diffStartX = offsetX + leftWidth + 1;
            diffEndX = offsetX + rightWidth;
        } else {
            diffStartX = offsetX + leftWidth;
            diffEndX = offsetX + leftWidth;
        }

        final int diffStartY;
        final int diffEndY;
        if (leftHeight > rightHeight) {
            diffStartY = offsetY + rightHeight + 1;
            diffEndY = offsetY + leftHeight;
        } else if (leftHeight < rightHeight) {
            diffStartY = offsetY + leftHeight + 1;
            diffEndY = offsetY + rightHeight;
        } else {
            diffStartY = offsetY + leftHeight;
            diffEndY = offsetY + leftHeight;
        }

        final List<Point> diffPointList = new ArrayList<Point>();
        // X方向に、重ならない座標を追加
        if (leftWidth != rightWidth) {
            for (int curX = diffStartX; curX <= diffEndX; curX++) {
                for (int curY = 0; curY < diffEndY; curY++) {
                    diffPointList.add(new Point(curX, curY));
                }
            }
        }
        // Y方向に重ならない座標を追加
        if (leftHeight != rightHeight) {
            for (int curY = diffStartY; curY <= diffEndY; curY++) {
                for (int curX = 0; curX < diffStartX; curX++) {
                    diffPointList.add(new Point(curX, curY));
                }
            }
        }
        return diffPointList;
    }

    /**
     * 画像の内容を比較します。
     *
     * @param leftImage 左画像
     * @param rightImage 右画像
     * @param offsetX X方向オフセット値
     * @param offsetY Y方向オフセット値
     * @return 差分座標リスト
     */
    private static List<Point> compareContent(
        final BufferedImage leftImage,
        final BufferedImage rightImage,
        final int offsetX,
        final int offsetY) {

        final int minWidth = Math.min(leftImage.getWidth(), rightImage.getWidth());
        final int minHeight = Math.min(leftImage.getHeight(), rightImage.getHeight());

        final int[] leftRgb = getRGB(leftImage, minWidth, minHeight);
        final int[] rightRgb = getRGB(rightImage, minWidth, minHeight);

        final List<Point> diffPointList = new ArrayList<Point>();
        for (int curIndex = 0; curIndex < leftRgb.length; curIndex++) {
            if (leftRgb[curIndex] != rightRgb[curIndex]) {
                final int diffX = (curIndex % minWidth) + offsetX;
                final int diffY = (curIndex / minWidth) + offsetY;
                diffPointList.add(new Point(diffX, diffY));
            }
        }
        return diffPointList;
    }

    /**
     * 画像のRGBピクセル配列を返します。
     *
     * @param image 画像
     * @param width 読み込む幅
     * @param height 読み込む高さ
     * @return ピクセル配列
     */
    private static int[] getRGB(BufferedImage image, int width, int height) {
        return image.getRGB(0, 0, width, height, null, 0, width);
    }

    /**
     * 二つの画像と差分情報から一致確認用画像を取得します。
     *
     * @param leftImage 左側の画像
     * @param rightImage 右側の画像
     * @param ignoreAreaList 除外エリアリスト
     * @param leftLabel 左画像ラベル
     * @param rightLabel 右画像ラベル
     * @param style 差分確認用画面スタイル
     * @return 一致確認用画像
     */
    public static BufferedImage getOkImage(
        final BufferedImage leftImage,
        final BufferedImage rightImage,
        final List<Rectangle> ignoreAreaList,
        final String leftLabel,
        final String rightLabel,
        final ConfirmImageStyle style) {

        // マスク
        final float alpha = 0.4f;
        final BufferedImage leftMarkedImage = getMaskedImage(leftImage, ignoreAreaList, alpha);
        final BufferedImage rightMarkedImage = getMaskedImage(rightImage, ignoreAreaList, alpha);

        // スタイル
        final ConfirmImageStyle effectiveStyle;
        if (style == null) {
            // デフォルトスタイル
            final int border = 4;

            final int labelFontSize = 24;
            final int labelHeight = 36;
            final int labelPaddingLeft = 12;
            final int labelPaddingTop = 28;

            final RgbaColor labelColor = new RgbaColor(255, 255, 255, 256 / 10 * 8);
            final RgbaColor leftBgColor = new RgbaColor(52, 152, 219, 255);
            final RgbaColor rightBgColor = new RgbaColor(26, 188, 156, 255);

            effectiveStyle = new ConfirmImageStyle(border, labelFontSize, labelHeight, labelPaddingLeft, labelPaddingTop, labelColor, leftBgColor, rightBgColor);

        } else {
            effectiveStyle = style;
        }

        // 確認画像の生成
        return getConfirmImage(leftMarkedImage, rightMarkedImage, leftLabel, rightLabel, effectiveStyle);
    }

    /**
     * 二つの画像と差分情報から差分確認用画像を取得します。
     *
     * @param leftImage 左側の画像
     * @param rightImage 右側の画像
     * @param diffAreas 差分データ
     * @param leftLabel 左画像ラベル
     * @param rightLabel 右画像ラベル
     * @param style 差分確認用画面スタイル
     * @return 差分確認用画像
     */
    public static BufferedImage getNgImage(
        final BufferedImage leftImage,
        final BufferedImage rightImage,
        final DiffAreas diffAreas,
        final String leftLabel,
        final String rightLabel,
        final ConfirmImageStyle style) {

        // マークを描画
        final BufferedImage leftMarkedImage = getMarkedImage(leftImage, diffAreas);
        final BufferedImage rightMarkedImage = getMarkedImage(rightImage, diffAreas);

        // スタイル
        final ConfirmImageStyle effectiveStyle;
        if (style == null) {
            // デフォルトスタイル
            final int border = 4;

            final int labelFontSize = 24;
            final int labelHeight = 36;
            final int labelPaddingLeft = 12;
            final int labelPaddingTop = 28;

            final RgbaColor labelColor = new RgbaColor(255, 255, 255, 200);
            final RgbaColor leftBgColor = new RgbaColor(52, 152, 219, 255);
            final RgbaColor rightBgColor = new RgbaColor(231, 76, 60, 255);

            effectiveStyle = new ConfirmImageStyle(border, labelFontSize, labelHeight, labelPaddingLeft, labelPaddingTop, labelColor, leftBgColor, rightBgColor);

        } else {
            effectiveStyle = style;
        }

        // 確認画像の生成
        return getConfirmImage(leftMarkedImage, rightMarkedImage, leftLabel, rightLabel, effectiveStyle);
    }

    /**
     * 左右に指定画像を並べた一枚の画像を返します。
     *
     * @param leftImage 左画像
     * @param rightImage 右画像
     * @param leftLabel 左ラベル
     * @param rightLabel 右ラベル
     * @param style 確認用画像スタイル
     * @return 確認用画像
     */
    private static BufferedImage getConfirmImage(
        final BufferedImage leftImage,
        final BufferedImage rightImage,
        final String leftLabel,
        final String rightLabel,
        final ConfirmImageStyle style) {

        final int border = style.getBorder();
        final int labelFontSize = style.getLabelFontSize();
        final int labelHeight = style.getLabelHeight();
        final int labelPaddingLeft = style.getLabelPaddingLeft();
        final int labelPaddingTop = style.getLabelPaddingTop();
        final Color labelColor = style.getLabelColor().getColor();
        final Color leftBgColor = style.getLeftBgColor().getColor();
        final Color rightBgColor = style.getRightBgColor().getColor();

        int borderedLeftWidth = leftImage.getWidth() + border * 2;
        int borderedRightWidth = rightImage.getWidth() + border * 2;
        int outputWidth = borderedLeftWidth + borderedRightWidth;

        final int borderedHeight;
        if (leftImage.getHeight() >= rightImage.getHeight()) {
            borderedHeight = leftImage.getHeight() + border * 2;
        } else {
            borderedHeight = rightImage.getHeight() + border * 2;
        }
        final int outputHeight = borderedHeight + labelHeight;

        final BufferedImage outputImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D graphics = (Graphics2D) outputImage.getGraphics();

        // 左領域の描画
        graphics.setColor(leftBgColor);
        graphics.fillRect(0, 0, borderedLeftWidth, outputHeight);
        graphics.drawImage(leftImage, border, labelHeight + border, null);

        // 右領域の描画
        graphics.setColor(rightBgColor);
        graphics.fillRect(borderedLeftWidth, 0, borderedRightWidth, outputHeight);
        graphics.drawImage(rightImage, borderedLeftWidth + border, labelHeight + border, null);

        // ラベル文字の描画
        graphics.setColor(labelColor);
        graphics.setFont(getFont(labelFontSize));
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawString(leftLabel, border + labelPaddingLeft, labelPaddingTop);
        graphics.drawString(rightLabel, borderedLeftWidth + border + labelPaddingLeft, labelPaddingTop);

        return outputImage;
    }

    /**
     * 確認画像用スタイル。
     */
    @Getter
    @Setter
    public static class ConfirmImageStyle {
        int border;
        int labelFontSize;
        int labelHeight;
        int labelPaddingLeft;
        int labelPaddingTop;
        RgbaColor labelColor;
        RgbaColor leftBgColor;
        RgbaColor rightBgColor;

        /**
         * コンストラクタ。
         */
        public ConfirmImageStyle() {
            super();
        }

        /**
         * コンストラクタ。
         *
         * @param border 対象画像を囲むボーダー幅（ピクセル指定）。
         * @param labelFontSize ラベルフォントサイズ（ポイント指定）。
         * @param labelHeight ラベル領域の高さ（ピクセル指定）。
         * @param labelPaddingLeft ラベルの左Padding（ピクセル指定）。
         * @param labelPaddingTop　ラベルの上Padding（ピクセル指定）。
         * @param labelColor ラベルのフォントカラー（RGBA指定）。
         * @param leftBgColor 左画像の背景色（RGBA指定）。r
         * @param rightBgColor 右画像の背景色（RGBA指定）。
         */
        public ConfirmImageStyle(
            final int border,
            final int labelFontSize,
            final int labelHeight,
            final int labelPaddingLeft,
            final int labelPaddingTop,
            final RgbaColor labelColor,
            final RgbaColor leftBgColor,
            final RgbaColor rightBgColor) {

            this();
            this.border = border;
            this.labelFontSize = labelFontSize;
            this.labelHeight = labelHeight;
            this.labelPaddingLeft = labelPaddingLeft;
            this.labelPaddingTop = labelPaddingTop;
            this.labelColor = labelColor;
            this.leftBgColor = leftBgColor;
            this.rightBgColor = rightBgColor;
        }
    }

    /**
     * RGBAスケールの色指定。
     */
    @Getter
    @Setter
    public static class RgbaColor {
        private int r = 0;
        private int g = 0;
        private int b = 0;
        private int a = 255;

        /**
         * コンストラクタ。
         */
        public RgbaColor() {
            super();
        }

        /**
         * コンストラクタ。
         *
         * @param r red
         * @param g green
         * @param b blue
         * @param a alpha
         */
        public RgbaColor(final int r, final int g, final int b, final int a) {

            this();
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        /**
         * このインスタンスが示すColorオブジェクトを返します。
         *
         * @return Color
         */
        protected Color getColor() {
            return new Color(r, g, b, a);
        }
    }

    /**
     * 画像描画で利用するフォントを指定サイズで返します。
     *
     * @param size フォントサイズ（ポイント指定）
     * @return 画像描画用フォント
     */
    private static Font getFont(final int size){
        final String fontName = "SourceHanCodeJP-Normal";
        if (font != null) {
            return font;
        }

        final String fileClasspath = "/fonts/" + fontName + ".otf";
        try {
            final InputStream is = ImageCompareUtils.class.getResourceAsStream(fileClasspath);
            final Font readedFont = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(readedFont);
            is.close();
        } catch (Exception e) {
            throw new UtilException(UtilMessageConst.ERRORHANDLE, new Object[] {"regist " + fileClasspath}, e);
        }

        font = new Font(fontName, Font.PLAIN, size);
        return font;
    }

    /**
     * 差分エリアをマークした画像を作成します。
     *
     * @param image 対象画像
     * @param diffAreas 差分エリア
     * @return マークした画像
     */
    private static BufferedImage getMarkedImage(BufferedImage image, DiffAreas diffAreas) {
        List<Rectangle> diffAreaList = diffAreas.getDiffAreaList();
        BufferedImage arrowImage = getArrowImage();

        // マーカーを描画すると、元画像をはみ出るかを確認
        int markedMaxX = image.getWidth();
        int markedMaxY = image.getHeight();
        int markedMinX = 0;
        int markedMinY = 0;

        boolean extend = false;
        final int strokeWidth = 4;
        final int arrowWidth = arrowImage.getWidth();
        final int arrowHeight = arrowImage.getHeight();
        for (final Rectangle diffArea : diffAreaList) {
            if (markedMaxX < (int) diffArea.getMaxX() + strokeWidth) {
                markedMaxX = (int) diffArea.getMaxX() + strokeWidth;
                extend = true;
            }
            if (markedMaxY < (int) diffArea.getMaxY() + strokeWidth) {
                markedMaxY = (int) diffArea.getMaxY() + strokeWidth;
                extend = true;
            }
            if (markedMinX > diffArea.getMinX() - arrowWidth) {
                markedMinX = (int) diffArea.getMinX() - arrowWidth;
                extend = true;
            }
            if (markedMinY > diffArea.getMinY() - arrowHeight) {
                markedMinY = (int) diffArea.getMinY() - arrowHeight;
                extend = true;
            }
        }

        BufferedImage markedImg;
        Graphics2D marker;
        if (extend) {
            // はみ出る場合、はみ出た分を包含する画像で背景を作成
            markedImg = new BufferedImage(markedMaxX - markedMinX, markedMaxY - markedMinY, BufferedImage.TYPE_INT_ARGB);
            marker = (Graphics2D) markedImg.getGraphics();
            marker.setBackground(Color.GRAY);
            marker.clearRect(0, 0, markedMaxX - markedMinX, markedMaxY - markedMinY);
            marker.drawImage(image, -markedMinX, -markedMinY, image.getWidth(), image.getHeight(), null);
        } else {
            // はみ出ない場合、コピーした画像で背景を作成
            markedImg = deepCopy(image);
            marker = (Graphics2D) markedImg.getGraphics();
        }

        for (final Rectangle diffArea : diffAreaList) {
            // マーカーで囲む
            marker.setColor(new RgbaColor(255, 0, 0, 100).getColor());
            marker.setStroke(new BasicStroke(strokeWidth));
            final int markerPadding = 2;
            marker.drawRect(
                diffArea.x - markerPadding - markedMinX,
                diffArea.y - markerPadding - markedMinY,
                diffArea.width + markerPadding * 2,
                diffArea.height + markerPadding * 2);
            // 矢印を置く
            marker.drawImage(arrowImage, diffArea.x - arrowWidth - markedMinX, diffArea.y - arrowHeight - markedMinY, null);
        }

        return markedImg;
    }

    /**
     * マーカー画像を返します。
     *
     * @return マーカー画像
     */
    private static BufferedImage getArrowImage() {
        if (arrowImage != null) {
            return arrowImage;
        }

        final String filename = "arrow.png";
        URL resource = ImageCompareUtils.class.getClassLoader().getResource(filename);
        if (resource == null) {
            throw new UtilException(UtilMessageConst.CHECK_NOTEXIST, new Object[] {filename});
        }

        try {
            arrowImage = ImageIO.read(resource);
        } catch (IOException e) {
            throw new UtilException(UtilMessageConst.FILE_CANTREAD, new Object[] {filename}, e);
        }

        return arrowImage;
    }

    /**
     * 指定エリアで切り出した画像を取得します。
     *
     * @param image 元画像
     * @param area 切り出すエリア
     * @return 切り出した画像
     */
    private static BufferedImage getSubImage(BufferedImage image, Rectangle area) {
        return image.getSubimage((int) area.getX(), (int) area.getY(), (int) area.getWidth(), (int) area.getHeight());
    }

    /**
     * 画像比較結果。
     */
    public static class DiffAreas {
        @Getter
        private List<Rectangle> diffAreaList;

        /**
         * コンストラクタ。
         *
         * @param diffPointList 内容の差分座標リスト
         * @param sizeDiffPointList サイズの差分座標リスト
         */
        protected DiffAreas(final List<Point> diffPointList, final List<Point> sizeDiffPointList) {
            diffAreaList = diffPointList2AreaList(diffPointList);
            diffAreaList.addAll(sizeDiffPoints2AreaList(sizeDiffPointList));
        }

        /**
         * 差分エリア数を返します。
         *
         * @return 差分エリア数
         */
        public int size() {
            return diffAreaList.size();
        }

        /**
         * 差分の有無を返します。
         *
         * @return 差分がある場合、true
         */
        public boolean hasDiff() {
            return !diffAreaList.isEmpty();
        }

        /**
         * 差分座標をグルーピングした矩形領域のリストを返します。
         *
         * @param diffPointList 差分座標リスト
         * @return 矩形領域リスト
         */
        private static List<Rectangle> diffPointList2AreaList(final List<Point> diffPointList) {
            if (diffPointList == null || diffPointList.isEmpty()) {
                return new ArrayList<Rectangle>();
            }

            final List<PointGroup> diffGroupList = new ArrayList<PointGroup>();
            for (final Point curPoint : diffPointList) {
                boolean isUnioned = false;
                final PointGroup group = new PointGroup(new Point(curPoint.x, curPoint.y));
                // 現在のポイントが、直前までにマージした領域のどれかにマージできるか確認
                for (final PointGroup diffGroup : diffGroupList) {
                    if (diffGroup.canUnion(group)) {
                        diffGroup.union(group);
                        isUnioned = true;
                        break;
                    }
                }
                // マージできなかった場合、新たな差分グループとして追加
                if (!isUnioned) {
                    diffGroupList.add(group);
                }
            }

            // マージ対象がなくなるまでループ
            int unionCount = -1;
            while (unionCount != 0) {
                unionCount = 0;
                for (final PointGroup curDiffGroup : diffGroupList) {
                    final List<PointGroup> removeGroupList = new ArrayList<PointGroup>();
                    for (final PointGroup curCheckGroup : diffGroupList) {
                        if (!curDiffGroup.equals(curCheckGroup) && curDiffGroup.canUnion(curCheckGroup)) {
                            // 確認対象をマージして、削除リストに追加
                            curDiffGroup.union(curCheckGroup);
                            unionCount++;
                            removeGroupList.add(curCheckGroup);
                        }
                    }
                    if (unionCount > 0) {
                        // 削除リストがある場合は、リストから取り除く
                        for (final PointGroup removeGroup : removeGroupList) {
                            diffGroupList.remove(removeGroup);
                        }
                        break;
                    }
                }
            }

            // グループリストから、矩形リストを作成
            final List<Rectangle> diffAreaList = new ArrayList<Rectangle>();
            for (final PointGroup curDiffGroup : diffGroupList) {
                diffAreaList.add(curDiffGroup.getRectangle());
            }
            return diffAreaList;
        }

        /**
         * サイズの差分座標をグルーピングした矩形領域のリストを返します。
         *
         * @param sizeDiffPointList サイズの差分座標リスト
         * @return 矩形領域リスト
         */
        private static List<Rectangle> sizeDiffPoints2AreaList(final List<Point> sizeDiffPointList) {
            List<Rectangle> areas = new ArrayList<Rectangle>();
            if (sizeDiffPointList == null || sizeDiffPointList.isEmpty()) {
                return areas;
            }

            Point start = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
            Point end = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);

            for (Point point : sizeDiffPointList) {
                if (point.y == 0 && start.x > point.x) {
                    start.x = point.x;
                }
                if (point.x == 0 && start.y > point.y) {
                    start.y = point.y;
                }
                if (end.x < point.x) {
                    end.x = point.x;
                }
                if (end.y < point.y) {
                    end.y = point.y;
                }
            }

            if (start.x <= end.x) {
                areas.add(new Rectangle(start.x, 0, end.x - start.x, end.y));
            }

            if (start.y <= end.y) {
                final int rectMargin = 3;
                areas.add(new Rectangle(-rectMargin, start.y - rectMargin, end.x + rectMargin * 2, end.y - start.y
                    + rectMargin * 2));
            }

            return areas;
        }
    }

    /**
     * 座標をグルーピングしたエリア。
     */
    private static class PointGroup {

        private static final int GROUP_DISTANCE = 10;
        private Rectangle rectangle;

        /**
         * 中心点を指定してマーカーを生成します。
         *
         * @param p 中心点
         */
        private PointGroup(Point p) {
            rectangle = new Rectangle(
                p.x - GROUP_DISTANCE / 2,
                p.y - GROUP_DISTANCE / 2,
                GROUP_DISTANCE,
                GROUP_DISTANCE);
        }

        /**
         * 結合の条件を満たすか否かを返します。
         * ※一方がもう一方を内包している、または交差している場合に結合可能です。
         *
         * @param pointGroup 対象マーカー
         * @return 結合できるか否か。結合可能な場合はtrue
         */
        public boolean canUnion(PointGroup pointGroup) {
            // お互いを内包している場合はOK
            if (pointGroup.getRectangle().contains(this.getRectangle()) ||
                this.getRectangle().contains(pointGroup.getRectangle())) {
                return true;
            }

            // 交差している場合はOK、その他はNG
            if (pointGroup.getRectangle().intersects(this.getRectangle())) {
                return true;
            }

            return false;
        }

        /**
         * 指定されたグループと結合します。
         *
         * @param pointGroup マージ対象座標グループ
         */
        public void union(PointGroup pointGroup) {
            rectangle = rectangle.union(pointGroup.getRectangle());
        }

        /**
         * グループの矩形領域を取得します。
         *
         * @return 矩形領域
         */
        public Rectangle getRectangle() {
            return rectangle;
        }

    }

}
