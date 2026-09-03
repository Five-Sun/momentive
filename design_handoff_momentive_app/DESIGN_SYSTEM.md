# Momentive Design System

Handoff spec for Momentive (모멘티브), a premium/minimal dog-apparel shopping web app. Pink-forward palette, warm neutrals, fully-rounded UI. Built as a **web app** (responsive, not native-only).

## Brand
- Wordmark: lowercase "momentive". Tagline: "소중한 순간에 러블리함을 가득담아 보내드립니다🤍"
- Logo: `assets/logo/momentive-logo.jpeg` (pastel-yellow circle, pink rounded wordmark) — reuse as-is, do not redraw.
- Tone: d다정하고 담백한 존댓말. No hard-sell copy. One 🤍 emoji max, nowhere else.
- Sizing: dog apparel sized by weight/back-length (XS–XL), never human clothing sizes.

## Color tokens (`tokens/colors.css`)
```css
--brand-pink:#f57ea0;        --brand-pink-active:#e8547f;   --brand-pink-deep:#d6396a;
--brand-pink-soft:#fce0ea;   --brand-pink-tint:#fff2f6;
--brand-yellow:#fef8c4;      --brand-yellow-soft:#fffdf0;
--ink:#33242b;  --body:#5c454f;  --muted:#93818a;  --muted-soft:#c4aeb7;
--hairline:#f3e2e9;  --hairline-soft:#f9edf1;  --border-strong:#e3c3d0;
--canvas:#fffbfc;  --surface-soft:#fdf1f4;  --surface-card:#ffffff;  --surface-strong:#fbe4ea;
--success:#5a8a6b;  --error:#c1543b;  --sale:#e8547f;
```
Pink is now the dominant tint across surfaces (canvas/soft/strong all pink-leaning), not just an accent — apply `--brand-pink` fills more freely (chips selected-state, active nav, section backgrounds) while keeping one bold primary-pink CTA per screen.

## Type (`tokens/typography.css`)
- Display + Body/UI: `UhBeeSehyun`(어비 세현체, 상업적 사용 무료) 전체 적용, fallback `Noto Sans KR`.
- Scale: `--text-display-lg` 30px, `--text-display-md` 24px, `--text-title` 18px/700, `--text-title-sm` 16px/600, `--text-body` 15px, `--text-body-sm` 13px, `--text-caption` 12px/500, `--text-price` 17px/700, `--text-button` 15px/600, `--text-tag` 11px/600.

## Spacing / radius / shadow
- Spacing scale (`tokens/spacing.css`): 4/8/12/16/24/32/48/64, 8px-grid.
- Radius (`tokens/radius.css`): xs 6, sm 10, md 16, lg 22, full 9999 — buttons always full pill, cards 16px, size-cells 10px. No square corners.
- Shadow (`tokens/shadow.css`): `--shadow-card` (default elevation, product cards/floating icons), `--shadow-float` (toast/bottom sheet only). Two levels total.
- Motion (`tokens/motion.css`): `--ease-spring` (cubic-bezier(.34,1.56,.64,1)), keyframes `paw-pop`(heart/chip/badge 토글 시 팝 바운스), `bump-up`(하단 탭 활성화 시 아이콘 살짝 튐). 미세 인터랙션에만 사용, 화면 전환 등 큰 모션에는 쓰지 않음.

## Components (`components/`)
- **core**: `Button` (primary pink pill / secondary outline / ghost underline), `IconButton` (circular, outline|filled), `Badge` (new/sale/soldout/neutral pills), `Chip` (category/filter toggle).
- **forms**: `SearchInput` (pill search bar).
- **navigation**: `BottomNav` (5-tab: 홈/카테고리/검색/위시/마이).
- **feedback**: `Toast` (bottom floating confirmation), `ShippingProgress` (free-shipping threshold nudge bar).
- **commerce**: `ProductCard` (square photo + floating heart + title/price/rating), `Rating` (ink star, never gold), `SizeSelector` (square size cells + 사이즈 가이드 link → breed/weight chart), `FilterSheet` (sort/filter bottom sheet), `ReviewCard` (review row w/ optional photo grid).

Each component has a `.jsx` (source), `.d.ts` (prop contract), `.prompt.md` (usage note + example) — read those before consuming. Live demos: `*.card.html` files per folder.

## Reference apps
`ui_kits/web-app/index.html` — **primary reference**, responsive web (mobile web ≤1024px: bottom tab bar; desktop >1024px: top horizontal nav + wider grid). Build from this one.
`ui_kits/mobile-app/index.html` — earlier native-app-shaped exploration, kept for reference only; superseded by web-app for layout decisions. Both share the same components, benchmarked against Korean commerce UX conventions (Ably/Musinsa-style structure, kept within Momentive's restrained pink-premium tone):
- **Home**: promo banner, "지금 인기 있는" ranked horizontal row, category chips, 2-col grid, "최근 본 상품" recently-viewed row.
- **Category**: category list screen (browse entry point).
- **Search**: type-ahead suggestions, ranked popular searches, sort/filter bottom sheet on results.
- **Product Detail**: image dots, size guide accordion, delivery/exchange accordion, reviews with photo grid.
- **Cart**: free-shipping progress bar, coupon toggle, qty steppers, order summary.
- **Wishlist**, **My** (profile summary, order/coupon/points shortcuts).
Rebuild these flows responsively for web (same components, fluid grid instead of fixed phone frame).

## Known gaps / flags for follow-up
- Jua is a substitute for the logo's actual display face — no source font file was provided.
- No real icon set — unicode glyphs (⌂ ⌕ ♥ 🛍) stand in for icons.
- No real product photography — `<image-slot>` placeholders throughout.
- Account/orders screen not yet designed.
