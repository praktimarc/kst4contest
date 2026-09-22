// Renders the background of the macOS installer DMG window.
//
// Run from the repository root after changing the design; only the TIFF is kept:
//   TMP="$(mktemp -d)"
//   swift packaging/macos/dmg/render-background.swift "$TMP"
//   tiffutil -cathidpicheck "$TMP/background.png" "$TMP/background@2x.png" \
//       -out packaging/macos/dmg/background.tiff
//
// The layout (window size, icon positions) must match build-signed-dmg.sh.
// Colors follow the green KST4Contest website theme and the application icon.

import AppKit

let windowWidth: CGFloat = 640
let windowHeight: CGFloat = 400
// Icon centers in Finder coordinates (origin top left), same values as in the build script.
let appIconCenter = CGPoint(x: 170, y: 180)
let applicationsIconCenter = CGPoint(x: 470, y: 180)
let iconSize: CGFloat = 128

func color(_ hex: UInt32, _ alpha: CGFloat = 1) -> CGColor {
    CGColor(srgbRed: CGFloat((hex >> 16) & 0xff) / 255,
            green: CGFloat((hex >> 8) & 0xff) / 255,
            blue: CGFloat(hex & 0xff) / 255,
            alpha: alpha)
}

func render(scale: CGFloat, to path: String) {
    let pixelWidth = Int(windowWidth * scale)
    let pixelHeight = Int(windowHeight * scale)
    guard let ctx = CGContext(data: nil, width: pixelWidth, height: pixelHeight,
                              bitsPerComponent: 8, bytesPerRow: 0,
                              space: CGColorSpace(name: CGColorSpace.sRGB)!,
                              bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else {
        fatalError("Could not create drawing context")
    }

    // Draw in Finder coordinates: origin top left, y pointing down.
    ctx.scaleBy(x: scale, y: scale)
    ctx.translateBy(x: 0, y: windowHeight)
    ctx.scaleBy(x: 1, y: -1)

    let space = CGColorSpace(name: CGColorSpace.sRGB)!

    // Background: dark green gradient of the website theme.
    let background = CGGradient(colorsSpace: space,
                                colors: [color(0x0b140f), color(0x050806)] as CFArray,
                                locations: [0, 1])!
    ctx.drawLinearGradient(background, start: CGPoint(x: 0, y: 0),
                           end: CGPoint(x: 0, y: windowHeight), options: [])

    // Soft accent glow behind the arrow.
    let glow = CGGradient(colorsSpace: space,
                          colors: [color(0x54d63d, 0.16), color(0x54d63d, 0)] as CFArray,
                          locations: [0, 1])!
    let middle = CGPoint(x: (appIconCenter.x + applicationsIconCenter.x) / 2, y: appIconCenter.y)
    ctx.drawRadialGradient(glow, startCenter: middle, startRadius: 0,
                           endCenter: middle, endRadius: 260, options: [])

    // Radio waves like in the icon, above the arrow.
    ctx.setLineCap(.round)
    ctx.setLineWidth(4)
    let waveCenter = CGPoint(x: middle.x, y: middle.y - 18)
    for (index, radius) in [CGFloat(14), 26].enumerated() {
        ctx.setStrokeColor(color(0x8aef64, index == 0 ? 0.55 : 0.3))
        ctx.addArc(center: waveCenter, radius: radius, startAngle: -.pi / 4 - .pi / 2,
                   endAngle: .pi / 4 - .pi / 2, clockwise: false)
        ctx.strokePath()
    }

    // Arrow from the app to the Applications folder.
    let arrowStart = appIconCenter.x + iconSize / 2 + 22
    let arrowEnd = applicationsIconCenter.x - iconSize / 2 - 22
    let arrowY = appIconCenter.y
    let headLength: CGFloat = 22
    let headHalfWidth: CGFloat = 16

    ctx.saveGState()
    ctx.setShadow(offset: .zero, blur: 12, color: color(0x54d63d, 0.6))
    let arrowPath = CGMutablePath()
    arrowPath.move(to: CGPoint(x: arrowStart, y: arrowY))
    arrowPath.addLine(to: CGPoint(x: arrowEnd - headLength + 2, y: arrowY))
    ctx.addPath(arrowPath)
    ctx.setLineWidth(10)
    ctx.setStrokeColor(color(0x54d63d))
    ctx.strokePath()

    let head = CGMutablePath()
    head.move(to: CGPoint(x: arrowEnd, y: arrowY))
    head.addLine(to: CGPoint(x: arrowEnd - headLength, y: arrowY - headHalfWidth))
    head.addLine(to: CGPoint(x: arrowEnd - headLength, y: arrowY + headHalfWidth))
    head.closeSubpath()
    ctx.addPath(head)
    ctx.setFillColor(color(0x54d63d))
    ctx.setLineJoin(.round)
    ctx.setLineWidth(4)
    ctx.setStrokeColor(color(0x54d63d))
    ctx.drawPath(using: .fillStroke)
    ctx.restoreGState()

    // Label plates: Finder draws icon labels black in light mode and white in dark
    // mode, and the color cannot be configured. This mid tone keeps both readable
    // (contrast about 4.6:1 for white and for black text).
    let labelTop = appIconCenter.y + iconSize / 2 + 4
    for center in [appIconCenter, applicationsIconCenter] {
        let plate = CGRect(x: center.x - 80, y: labelTop, width: 160, height: 26)
        ctx.addPath(CGPath(roundedRect: plate, cornerWidth: 13, cornerHeight: 13, transform: nil))
        ctx.setFillColor(color(0x5c7d63))
        ctx.fillPath()
    }

    // Thin accent line at the bottom, as used on the website.
    ctx.setFillColor(color(0x31b54a))
    ctx.fill(CGRect(x: 0, y: windowHeight - 4, width: windowWidth, height: 4))

    guard let image = ctx.makeImage() else {
        fatalError("Could not render image")
    }
    let rep = NSBitmapImageRep(cgImage: image)
    // 72 dpi for 1x and 144 dpi for 2x, so tiffutil can combine them into one HiDPI TIFF.
    rep.size = NSSize(width: windowWidth, height: windowHeight)
    guard let data = rep.representation(using: .png, properties: [:]) else {
        fatalError("Could not encode PNG")
    }
    try! data.write(to: URL(fileURLWithPath: path))
}

let outputDirectory = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "."
render(scale: 1, to: outputDirectory + "/background.png")
render(scale: 2, to: outputDirectory + "/background@2x.png")
print("Rendered background.png and background@2x.png into \(outputDirectory)")
