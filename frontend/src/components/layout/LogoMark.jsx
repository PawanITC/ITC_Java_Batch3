/**
 * Funkart wordmark logo.
 *
 * Icon mark: teal rounded-square with a geometric "F" in white.
 * A small terracotta spark sits at the crossbar end — the brand's only accent.
 * Wordmark: "Funkart" in Inter 800, rendered as SVG text so it scales cleanly.
 *
 * Props:
 *   size   — icon height in px (default 32). Wordmark scales proportionally.
 *   dark   — when true, wordmark text becomes white (pass your theme flag).
 */
export default function LogoMark({ size = 32, dark = false }) {
    const iconW = size;
    const iconH = size;
    const gap   = size * 0.35;
    const textH = size * 0.44;
    const totalW = iconW + gap + size * 3.8;

    return (
        <svg
            viewBox={`0 0 ${totalW} ${iconH}`}
            height={iconH}
            xmlns="http://www.w3.org/2000/svg"
            aria-label="Funkart"
            role="img"
        >
            {/* ── Icon mark ── */}
            {/* Teal rounded square */}
            <rect
                x="0" y="0"
                width={iconW} height={iconH}
                rx={iconW * 0.22}
                fill="#2a5f78"
            />

            {/* White F — vertical stroke */}
            <rect
                x={iconW * 0.25}
                y={iconH * 0.20}
                width={iconW * 0.13}
                height={iconH * 0.60}
                rx={iconW * 0.065}
                fill="white"
            />
            {/* F — top horizontal bar */}
            <rect
                x={iconW * 0.25}
                y={iconH * 0.20}
                width={iconW * 0.52}
                height={iconH * 0.13}
                rx={iconW * 0.065}
                fill="white"
            />
            {/* F — middle horizontal bar */}
            <rect
                x={iconW * 0.25}
                y={iconH * 0.435}
                width={iconW * 0.36}
                height={iconH * 0.13}
                rx={iconW * 0.065}
                fill="white"
            />

            {/* Terracotta spark dot at end of top bar */}
            <circle
                cx={iconW * 0.25 + iconW * 0.52 + iconW * 0.085}
                cy={iconH * 0.20 + iconH * 0.065}
                r={iconW * 0.08}
                fill="#c96a3a"
            />

            {/* ── Wordmark ── */}
            <text
                x={iconW + gap}
                y={iconH * 0.75}
                fontFamily="Inter, system-ui, -apple-system, sans-serif"
                fontWeight="800"
                fontSize={textH}
                letterSpacing="-0.03em"
                fill={dark ? "#f5f2ee" : "#1d1a17"}
            >
                Funkart
            </text>
        </svg>
    );
}
