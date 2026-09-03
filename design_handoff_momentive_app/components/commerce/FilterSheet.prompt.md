Bottom sheet sort/filter picker over a scrim. Opens from a "정렬/필터" toolbar button above a product grid.

```jsx
<FilterSheet open={open} sortOptions={['인기순','신상순','낮은 가격순','높은 가격순']} selected={sort} onSelect={setSort} onApply={() => setOpen(false)} onClose={() => setOpen(false)} />
```
