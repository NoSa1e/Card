#!/usr/bin/env node
import { mkdirSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const outDir = join(__dirname, '../public/cards');
if (!existsSync(outDir)) {
  mkdirSync(outDir, { recursive: true });
}

const suits = [
  { key: 'SPADES', short: 'S', symbol: '\u2660', main: '#0f1a2a', accent: '#122a46', text: '#0d1a2b' },
  { key: 'HEARTS', short: 'H', symbol: '\u2665', main: '#fcf2f4', accent: '#ffe5ea', text: '#d22f46' },
  { key: 'DIAMONDS', short: 'D', symbol: '\u2666', main: '#fcf8f2', accent: '#ffe6d0', text: '#d25e2f' },
  { key: 'CLUBS', short: 'C', symbol: '\u2663', main: '#f3f6fb', accent: '#dfe6f2', text: '#183025' }
];

const ranks = [
  { code: 'A', label: 'A' },
  { code: '2', label: '2' },
  { code: '3', label: '3' },
  { code: '4', label: '4' },
  { code: '5', label: '5' },
  { code: '6', label: '6' },
  { code: '7', label: '7' },
  { code: '8', label: '8' },
  { code: '9', label: '9' },
  { code: '10', label: '10' },
  { code: 'J', label: 'J' },
  { code: 'Q', label: 'Q' },
  { code: 'K', label: 'K' }
];

const fontFamily = "'Pretendard','Inter','Noto Sans','Noto Sans KR',system-ui,sans-serif";

function svgFor(rank, suit) {
  const upperSymbol = suit.symbol;
  const lowerSymbol = suit.symbol;
  const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg width="228" height="312" viewBox="0 0 228 312" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect width="228" height="312" rx="20" fill="#ffffff"/>
  <rect x="10" y="10" width="208" height="292" rx="16" fill="${suit.main}" stroke="${suit.accent}" stroke-width="4"/>
  <g fill="${suit.text}" font-family="${fontFamily}">
    <text x="28" y="58" font-size="48" font-weight="700">${rank.label}</text>
    <text x="28" y="104" font-size="40" font-weight="600">${upperSymbol}</text>
  </g>
  <g fill="${suit.text}" font-family="${fontFamily}" transform="rotate(180 114 156)">
    <text x="28" y="58" font-size="48" font-weight="700">${rank.label}</text>
    <text x="28" y="104" font-size="40" font-weight="600">${lowerSymbol}</text>
  </g>
  <text x="114" y="176" text-anchor="middle" font-size="132" font-weight="600" fill="${suit.text}" font-family="${fontFamily}">${upperSymbol}</text>
</svg>`;
  return svg;
}

for (const suit of suits) {
  for (const rank of ranks) {
    const svg = svgFor(rank, suit);
    const baseName = `${rank.code}_${suit.short}`;
    const longName = `${rank.code}_${suit.key}`;
    writeFileSync(join(outDir, `${baseName}.svg`), svg, 'utf8');
    writeFileSync(join(outDir, `${longName}.svg`), svg, 'utf8');
  }
}

const backSvg = `<?xml version="1.0" encoding="UTF-8"?>
<svg width="228" height="312" viewBox="0 0 228 312" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect width="228" height="312" rx="20" fill="#0c1a3a"/>
  <rect x="14" y="14" width="200" height="284" rx="18" fill="#103269" stroke="#1a4c99" stroke-width="4"/>
  <g stroke="#6db0ff" stroke-width="3" opacity="0.65">
    <rect x="42" y="42" width="144" height="228" rx="16"/>
    <path d="M57 78h114"/><path d="M57 120h114"/><path d="M57 162h114"/><path d="M57 204h114"/><path d="M57 246h114"/>
  </g>
  <g fill="#89c5ff" opacity="0.9">
    <circle cx="114" cy="156" r="54" fill="#163f7a" stroke="#89c5ff" stroke-width="4"/>
    <path d="M114 112l18 36h-36l18-36z" fill="#89c5ff"/>
    <circle cx="114" cy="156" r="12" fill="#0c1a3a"/>
  </g>
</svg>`;
writeFileSync(join(outDir, 'BACK.svg'), backSvg, 'utf8');

console.log('Generated playing card SVGs in', outDir);
