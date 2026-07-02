// Package imagecmp は画像比較と結果画像の生成を提供します。
// Java 版 me.suwash.util.ImageCompareUtils 相当です。
// 差分エリアの数・座標は Java 版と同一のアルゴリズムで算出します。
// 結果画像はレイアウト・配色を再現しますが、フォントラスタライズ等の
// 描画結果はバイト一致しません (見た目同等)。
package imagecmp

import (
	"fmt"
	"image"
	"image/color"
	"image/draw"
	"image/gif"
	"image/jpeg"
	"image/png"
	"math"
	"os"
	"path/filepath"
	"strings"

	"golang.org/x/image/bmp"

	"github.com/scenario-test-framework/compare-files/internal/config"
)

// allowedExts は比較対応拡張子です (Java 8 ImageIO 標準 SPI 相当。wbmp は非対応)。
var allowedExts = map[string]bool{
	"png": true, "jpg": true, "jpeg": true, "gif": true, "bmp": true,
}

// IsAllowedExt は比較に対応している拡張子か確認します (大文字小文字非区別)。
func IsAllowedExt(ext string) bool {
	return allowedExts[strings.ToLower(ext)]
}

// Rect は Java の java.awt.Rectangle 相当の矩形です (負座標を許容)。
type Rect struct {
	X, Y, W, H int
}

func (r Rect) maxX() int { return r.X + r.W }
func (r Rect) maxY() int { return r.Y + r.H }

// intersects は Java Rectangle.intersects と同じ判定です (辺の接触は交差ではない)。
func (r Rect) intersects(o Rect) bool {
	return o.X < r.X+r.W && o.X+o.W > r.X && o.Y < r.Y+r.H && o.Y+o.H > r.Y
}

// contains は Java Rectangle.contains(Rectangle) と同じ判定です。
func (r Rect) contains(o Rect) bool {
	return o.X >= r.X && o.Y >= r.Y && o.X+o.W <= r.X+r.W && o.Y+o.H <= r.Y+r.H
}

// union は 2 矩形を包含する矩形を返します。
func (r Rect) union(o Rect) Rect {
	x1 := min(r.X, o.X)
	y1 := min(r.Y, o.Y)
	x2 := max(r.X+r.W, o.X+o.W)
	y2 := max(r.Y+r.H, o.Y+o.H)
	return Rect{X: x1, Y: y1, W: x2 - x1, H: y2 - y1}
}

// LoadImage は画像ファイルを読み込みます (png/jpg/jpeg/gif/bmp)。
func LoadImage(path string) (*image.NRGBA, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, fmt.Errorf("ファイルを読込みできません。対象:%s: %w", path, err)
	}
	defer f.Close()

	var img image.Image
	switch strings.ToLower(strings.TrimPrefix(filepath.Ext(path), ".")) {
	case "png":
		img, err = png.Decode(f)
	case "jpg", "jpeg":
		img, err = jpeg.Decode(f)
	case "gif":
		img, err = gif.Decode(f)
	case "bmp":
		img, err = bmp.Decode(f)
	default:
		img, _, err = image.Decode(f)
	}
	if err != nil {
		return nil, fmt.Errorf("ファイルを読込みできません。対象:%s: %w", path, err)
	}
	return toNRGBA(img), nil
}

// SavePNG は画像を PNG として書き出します。
func SavePNG(img image.Image, path string) error {
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		return err
	}
	f, err := os.Create(path)
	if err != nil {
		return fmt.Errorf("ファイルを書出しできません。対象:%s: %w", path, err)
	}
	if err := png.Encode(f, img); err != nil {
		f.Close()
		return fmt.Errorf("ファイルを書出しできません。対象:%s: %w", path, err)
	}
	return f.Close()
}

func toNRGBA(img image.Image) *image.NRGBA {
	if n, ok := img.(*image.NRGBA); ok && n.Bounds().Min == (image.Point{}) {
		return n
	}
	b := img.Bounds()
	out := image.NewNRGBA(image.Rect(0, 0, b.Dx(), b.Dy()))
	draw.Draw(out, out.Bounds(), img, b.Min, draw.Src)
	return out
}

// DiffAreas は画像比較結果です。
type DiffAreas struct {
	AreaList []Rect
}

// Size は差分エリア数を返します。
func (d *DiffAreas) Size() int { return len(d.AreaList) }

// HasDiff は差分の有無を返します。
func (d *DiffAreas) HasDiff() bool { return len(d.AreaList) > 0 }

// Compare は 2 つの画像を比較します。Java 版 ImageCompareUtils.compare 相当。
// 除外エリアは不透明の白で塗りつぶしてから比較します。
func Compare(leftImage, rightImage *image.NRGBA, ignoreAreaList []config.Rectangle) *DiffAreas {
	maskedLeft := leftImage
	maskedRight := rightImage
	if len(ignoreAreaList) > 0 {
		maskedLeft = maskedImage(leftImage, ignoreAreaList, 255)
		maskedRight = maskedImage(rightImage, ignoreAreaList, 255)
	}
	sizeDiffPoints := compareSize(maskedLeft, maskedRight)
	diffPoints := compareContent(maskedLeft, maskedRight)
	return newDiffAreas(diffPoints, sizeDiffPoints)
}

// maskedImage は指定エリアを白 (指定アルファ) でマスクした画像を返します。
func maskedImage(src *image.NRGBA, maskAreas []config.Rectangle, alpha uint8) *image.NRGBA {
	out := image.NewNRGBA(src.Bounds())
	copy(out.Pix, src.Pix)
	maskColor := color.NRGBA{R: 255, G: 255, B: 255, A: alpha}
	for _, area := range maskAreas {
		rect := image.Rect(int(area.X), int(area.Y), int(area.X)+int(area.Width), int(area.Y)+int(area.Height))
		draw.Draw(out, rect.Intersect(out.Bounds()), &image.Uniform{maskColor}, image.Point{}, draw.Over)
	}
	return out
}

type point struct{ x, y int }

// compareSize は画像サイズを比較し、重ならない領域の座標を返します。
// Java 版 compareSize 相当 (オフセットは常に 0)。
func compareSize(left, right *image.NRGBA) []point {
	leftWidth, leftHeight := left.Bounds().Dx(), left.Bounds().Dy()
	rightWidth, rightHeight := right.Bounds().Dx(), right.Bounds().Dy()

	if leftWidth == rightWidth && leftHeight == rightHeight {
		return nil
	}

	var diffStartX, diffEndX int
	switch {
	case leftWidth > rightWidth:
		diffStartX = rightWidth + 1
		diffEndX = leftWidth
	case leftWidth < rightWidth:
		diffStartX = leftWidth + 1
		diffEndX = rightWidth
	default:
		diffStartX = leftWidth
		diffEndX = leftWidth
	}

	var diffStartY, diffEndY int
	switch {
	case leftHeight > rightHeight:
		diffStartY = rightHeight + 1
		diffEndY = leftHeight
	case leftHeight < rightHeight:
		diffStartY = leftHeight + 1
		diffEndY = rightHeight
	default:
		diffStartY = leftHeight
		diffEndY = leftHeight
	}

	var diffPoints []point
	if leftWidth != rightWidth {
		for curX := diffStartX; curX <= diffEndX; curX++ {
			for curY := 0; curY < diffEndY; curY++ {
				diffPoints = append(diffPoints, point{curX, curY})
			}
		}
	}
	if leftHeight != rightHeight {
		for curY := diffStartY; curY <= diffEndY; curY++ {
			for curX := 0; curX < diffStartX; curX++ {
				diffPoints = append(diffPoints, point{curX, curY})
			}
		}
	}
	return diffPoints
}

// compareContent は重なる範囲のピクセルを厳密比較します。
func compareContent(left, right *image.NRGBA) []point {
	minWidth := min(left.Bounds().Dx(), right.Bounds().Dx())
	minHeight := min(left.Bounds().Dy(), right.Bounds().Dy())

	var diffPoints []point
	for y := 0; y < minHeight; y++ {
		leftRow := left.Pix[y*left.Stride : y*left.Stride+minWidth*4]
		rightRow := right.Pix[y*right.Stride : y*right.Stride+minWidth*4]
		for x := 0; x < minWidth; x++ {
			o := x * 4
			if leftRow[o] != rightRow[o] || leftRow[o+1] != rightRow[o+1] ||
				leftRow[o+2] != rightRow[o+2] || leftRow[o+3] != rightRow[o+3] {
				diffPoints = append(diffPoints, point{x, y})
			}
		}
	}
	return diffPoints
}

// newDiffAreas は差分座標をグルーピングして矩形リストを作成します。
func newDiffAreas(diffPoints, sizeDiffPoints []point) *DiffAreas {
	areas := diffPoints2AreaList(diffPoints)
	areas = append(areas, sizeDiffPoints2AreaList(sizeDiffPoints)...)
	return &DiffAreas{AreaList: areas}
}

const groupDistance = 10

// newPointGroup は中心点から 10x10 の矩形グループを作成します。
func newPointGroup(p point) Rect {
	return Rect{X: p.x - groupDistance/2, Y: p.y - groupDistance/2, W: groupDistance, H: groupDistance}
}

func canUnion(a, b Rect) bool {
	return a.contains(b) || b.contains(a) || a.intersects(b)
}

// diffPoints2AreaList は差分座標をグルーピングした矩形リストを返します。
// Java 版 DiffAreas.diffPointList2AreaList 相当。
func diffPoints2AreaList(diffPoints []point) []Rect {
	if len(diffPoints) == 0 {
		return nil
	}

	var groups []Rect
	for _, p := range diffPoints {
		group := newPointGroup(p)
		unioned := false
		for i := range groups {
			if canUnion(groups[i], group) {
				groups[i] = groups[i].union(group)
				unioned = true
				break
			}
		}
		if !unioned {
			groups = append(groups, group)
		}
	}

	// マージ対象がなくなるまでループ (Java 版と同じ収束処理)
	for {
		unionCount := 0
		for i := 0; i < len(groups); i++ {
			var removeIdx []int
			for j := 0; j < len(groups); j++ {
				if i != j && canUnion(groups[i], groups[j]) {
					groups[i] = groups[i].union(groups[j])
					unionCount++
					removeIdx = append(removeIdx, j)
				}
			}
			if unionCount > 0 {
				kept := groups[:0]
				removeSet := map[int]bool{}
				for _, idx := range removeIdx {
					removeSet[idx] = true
				}
				for idx := range groups {
					if !removeSet[idx] {
						kept = append(kept, groups[idx])
					}
				}
				groups = kept
				break
			}
		}
		if unionCount == 0 {
			break
		}
	}
	return groups
}

// sizeDiffPoints2AreaList はサイズ差分座標をバウンディング矩形化します。
// Java 版 DiffAreas.sizeDiffPoints2AreaList 相当。
func sizeDiffPoints2AreaList(sizeDiffPoints []point) []Rect {
	if len(sizeDiffPoints) == 0 {
		return nil
	}

	start := point{x: math.MaxInt, y: math.MaxInt}
	end := point{x: math.MinInt, y: math.MinInt}

	for _, p := range sizeDiffPoints {
		if p.y == 0 && start.x > p.x {
			start.x = p.x
		}
		if p.x == 0 && start.y > p.y {
			start.y = p.y
		}
		if end.x < p.x {
			end.x = p.x
		}
		if end.y < p.y {
			end.y = p.y
		}
	}

	var areas []Rect
	if start.x <= end.x {
		areas = append(areas, Rect{X: start.x, Y: 0, W: end.x - start.x, H: end.y})
	}
	if start.y <= end.y {
		const rectMargin = 3
		areas = append(areas, Rect{X: -rectMargin, Y: start.y - rectMargin, W: end.x + rectMargin*2, H: end.y - start.y + rectMargin*2})
	}
	return areas
}
