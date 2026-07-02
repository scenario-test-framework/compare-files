package imagecmp

import (
	"bytes"
	"image"
	"image/color"
	"image/draw"
	"image/png"
	"sync"

	"golang.org/x/image/font"
	"golang.org/x/image/font/opentype"
	"golang.org/x/image/math/fixed"

	"github.com/scenario-test-framework/compare-files/internal/assets"
	"github.com/scenario-test-framework/compare-files/internal/config"
)

// デフォルトスタイル (Java 版 getOkImage / getNgImage のフォールバック値)。
func defaultStyle(ok bool) *config.ConfirmImageStyle {
	labelAlpha := config.JInt(200)
	rightBg := &config.RgbaColor{R: 231, G: 76, B: 60, A: 255}
	if ok {
		labelAlpha = config.JInt(256 / 10 * 8) // 204
		rightBg = &config.RgbaColor{R: 26, G: 188, B: 156, A: 255}
	}
	return &config.ConfirmImageStyle{
		Border:           4,
		LabelFontSize:    24,
		LabelHeight:      36,
		LabelPaddingLeft: 12,
		LabelPaddingTop:  28,
		LabelColor:       &config.RgbaColor{R: 255, G: 255, B: 255, A: labelAlpha},
		LeftBgColor:      &config.RgbaColor{R: 52, G: 152, B: 219, A: 255},
		RightBgColor:     rightBg,
	}
}

// OkImage は一致確認用画像 (左右並置、除外エリアを半透明マスク) を返します。
func OkImage(leftImage, rightImage *image.NRGBA, ignoreAreaList []config.Rectangle, leftLabel, rightLabel string, style *config.ConfirmImageStyle) *image.NRGBA {
	// マスク (alpha 0.4 = 102/255)
	leftMasked := maskedImage(leftImage, ignoreAreaList, 102)
	rightMasked := maskedImage(rightImage, ignoreAreaList, 102)
	if style == nil {
		style = defaultStyle(true)
	}
	return confirmImage(leftMasked, rightMasked, leftLabel, rightLabel, style)
}

// NgImage は差分確認用画像 (左右並置、差分エリアを赤枠+矢印でマーク) を返します。
func NgImage(leftImage, rightImage *image.NRGBA, diffAreas *DiffAreas, leftLabel, rightLabel string, style *config.ConfirmImageStyle) *image.NRGBA {
	leftMarked := markedImage(leftImage, diffAreas)
	rightMarked := markedImage(rightImage, diffAreas)
	if style == nil {
		style = defaultStyle(false)
	}
	return confirmImage(leftMarked, rightMarked, leftLabel, rightLabel, style)
}

func rgba(c *config.RgbaColor) color.NRGBA {
	if c == nil {
		return color.NRGBA{A: 255}
	}
	return color.NRGBA{R: uint8(c.R), G: uint8(c.G), B: uint8(c.B), A: uint8(c.A)}
}

// confirmImage は左右に画像を並べた 1 枚の確認用画像を返します。
// Java 版 getConfirmImage 相当 (背景色 → 画像 → ラベル文字の順に描画)。
func confirmImage(leftImage, rightImage *image.NRGBA, leftLabel, rightLabel string, style *config.ConfirmImageStyle) *image.NRGBA {
	border := int(style.Border)
	labelHeight := int(style.LabelHeight)
	labelPaddingLeft := int(style.LabelPaddingLeft)
	labelPaddingTop := int(style.LabelPaddingTop)

	borderedLeftWidth := leftImage.Bounds().Dx() + border*2
	borderedRightWidth := rightImage.Bounds().Dx() + border*2
	outputWidth := borderedLeftWidth + borderedRightWidth

	borderedHeight := max(leftImage.Bounds().Dy(), rightImage.Bounds().Dy()) + border*2
	outputHeight := borderedHeight + labelHeight

	out := image.NewNRGBA(image.Rect(0, 0, outputWidth, outputHeight))
	// TYPE_INT_RGB 相当: 黒で初期化 (不透明)
	draw.Draw(out, out.Bounds(), &image.Uniform{color.NRGBA{A: 255}}, image.Point{}, draw.Src)

	// 左領域
	draw.Draw(out, image.Rect(0, 0, borderedLeftWidth, outputHeight), &image.Uniform{rgba(style.LeftBgColor)}, image.Point{}, draw.Over)
	draw.Draw(out, leftImage.Bounds().Add(image.Pt(border, labelHeight+border)), leftImage, image.Point{}, draw.Over)

	// 右領域
	draw.Draw(out, image.Rect(borderedLeftWidth, 0, outputWidth, outputHeight), &image.Uniform{rgba(style.RightBgColor)}, image.Point{}, draw.Over)
	draw.Draw(out, rightImage.Bounds().Add(image.Pt(borderedLeftWidth+border, labelHeight+border)), rightImage, image.Point{}, draw.Over)

	// ラベル文字
	face := labelFace(int(style.LabelFontSize))
	if face != nil {
		labelColor := rgba(style.LabelColor)
		drawString(out, leftLabel, border+labelPaddingLeft, labelPaddingTop, labelColor, face)
		drawString(out, rightLabel, borderedLeftWidth+border+labelPaddingLeft, labelPaddingTop, labelColor, face)
	}
	return out
}

var (
	fontOnce   sync.Once
	parsedFont *opentype.Font
	faceCache  = map[int]font.Face{}
	faceMu     sync.Mutex
)

// labelFace は同梱フォント SourceHanCodeJP-Normal の指定サイズの Face を返します。
func labelFace(size int) font.Face {
	fontOnce.Do(func() {
		f, err := opentype.Parse(assets.Font())
		if err == nil {
			parsedFont = f
		}
	})
	if parsedFont == nil {
		return nil
	}
	faceMu.Lock()
	defer faceMu.Unlock()
	if face, ok := faceCache[size]; ok {
		return face
	}
	face, err := opentype.NewFace(parsedFont, &opentype.FaceOptions{
		Size:    float64(size),
		DPI:     72, // Java の Font サイズ (pt) と等倍
		Hinting: font.HintingNone,
	})
	if err != nil {
		return nil
	}
	faceCache[size] = face
	return face
}

// renderMu は文字描画を直列化します (opentype の Face は並行利用に対応していないため)。
var renderMu sync.Mutex

// drawString は (x, y) をベースラインとして文字列を描画します。
func drawString(dst draw.Image, s string, x, y int, c color.NRGBA, face font.Face) {
	renderMu.Lock()
	defer renderMu.Unlock()
	d := &font.Drawer{
		Dst:  dst,
		Src:  &image.Uniform{c},
		Face: face,
		Dot:  fixed.P(x, y),
	}
	d.DrawString(s)
}

var (
	arrowOnce sync.Once
	arrowImg  *image.NRGBA
)

// arrowImage は同梱の差分マーカー画像 (arrow.png) を返します。
func arrowImage() *image.NRGBA {
	arrowOnce.Do(func() {
		img, err := png.Decode(bytes.NewReader(assets.ArrowImage()))
		if err == nil {
			arrowImg = toNRGBA(img)
		}
	})
	return arrowImg
}

// markedImage は差分エリアを赤枠と矢印でマークした画像を返します。
// Java 版 getMarkedImage 相当。マークが元画像をはみ出す場合はグレー背景で拡張します。
func markedImage(img *image.NRGBA, diffAreas *DiffAreas) *image.NRGBA {
	arrow := arrowImage()
	arrowWidth, arrowHeight := 0, 0
	if arrow != nil {
		arrowWidth = arrow.Bounds().Dx()
		arrowHeight = arrow.Bounds().Dy()
	}

	const strokeWidth = 4
	markedMaxX := img.Bounds().Dx()
	markedMaxY := img.Bounds().Dy()
	markedMinX := 0
	markedMinY := 0
	extend := false
	for _, area := range diffAreas.AreaList {
		if markedMaxX < area.maxX()+strokeWidth {
			markedMaxX = area.maxX() + strokeWidth
			extend = true
		}
		if markedMaxY < area.maxY()+strokeWidth {
			markedMaxY = area.maxY() + strokeWidth
			extend = true
		}
		if markedMinX > area.X-arrowWidth {
			markedMinX = area.X - arrowWidth
			extend = true
		}
		if markedMinY > area.Y-arrowHeight {
			markedMinY = area.Y - arrowHeight
			extend = true
		}
	}

	var marked *image.NRGBA
	if extend {
		marked = image.NewNRGBA(image.Rect(0, 0, markedMaxX-markedMinX, markedMaxY-markedMinY))
		// Color.GRAY 背景
		draw.Draw(marked, marked.Bounds(), &image.Uniform{color.NRGBA{R: 128, G: 128, B: 128, A: 255}}, image.Point{}, draw.Src)
		draw.Draw(marked, img.Bounds().Add(image.Pt(-markedMinX, -markedMinY)), img, image.Point{}, draw.Over)
	} else {
		marked = image.NewNRGBA(img.Bounds())
		copy(marked.Pix, img.Pix)
	}

	markerColor := color.NRGBA{R: 255, G: 0, B: 0, A: 100}
	const markerPadding = 2
	for _, area := range diffAreas.AreaList {
		drawRectStroke(marked,
			area.X-markerPadding-markedMinX,
			area.Y-markerPadding-markedMinY,
			area.W+markerPadding*2,
			area.H+markerPadding*2,
			strokeWidth, markerColor)
		if arrow != nil {
			draw.Draw(marked, arrow.Bounds().Add(image.Pt(area.X-arrowWidth-markedMinX, area.Y-arrowHeight-markedMinY)), arrow, image.Point{}, draw.Over)
		}
	}
	return marked
}

// drawRectStroke は矩形パスを中心に指定太さの枠を描画します
// (Java の BasicStroke(4) + drawRect 相当: パスの内外に 2px ずつ)。
func drawRectStroke(dst *image.NRGBA, x, y, w, h, stroke int, c color.NRGBA) {
	half := stroke / 2
	uniform := &image.Uniform{c}
	// 上辺
	draw.Draw(dst, image.Rect(x-half, y-half, x+w+half, y+half+stroke%2), uniform, image.Point{}, draw.Over)
	// 下辺
	draw.Draw(dst, image.Rect(x-half, y+h-half, x+w+half, y+h+half+stroke%2), uniform, image.Point{}, draw.Over)
	// 左辺
	draw.Draw(dst, image.Rect(x-half, y+half, x+half+stroke%2, y+h-half), uniform, image.Point{}, draw.Over)
	// 右辺
	draw.Draw(dst, image.Rect(x+w-half, y+half, x+w+half+stroke%2, y+h-half), uniform, image.Point{}, draw.Over)
}
