'use client';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface LocalCartItem {
  productId: string;
  maSp: number;
  tenSp: string;
  hinh: string;
  giaHienTai: number;
  giaBanDau: number;
  giaGiamGia: number;
  thuongHieu?: string;
  loaiDa?: string;
  mauDa?: string;
  hamLuong?: string;
  trongLuong?: number;
  quantity: number;
}

interface CartState {
  items: LocalCartItem[];
  selectedIds: string[];
  hydrated: boolean;
  addItem: (item: Omit<LocalCartItem, 'quantity'>, qty?: number) => void;
  removeItem: (productId: string) => void;
  updateQuantity: (productId: string, qty: number) => void;
  clearAll: () => void;
  clearSelected: () => void;
  toggleSelect: (productId: string) => void;
  toggleSelectAll: (select: boolean) => void;
  setSelected: (ids: string[]) => void;
  totalItems: () => number;
  totalSelected: () => number;
  selectedItems: () => LocalCartItem[];
  setHydrated: () => void;
}

export const useCartStore = create<CartState>()(
  persist(
    (set, get) => ({
      items: [],
      selectedIds: [],
      hydrated: false,
      addItem: (item, qty = 1) => {
        const items = get().items;
        const existing = items.find((i) => i.productId === item.productId);
        if (existing) {
          set({
            items: items.map((i) =>
              i.productId === item.productId ? { ...i, quantity: i.quantity + qty } : i
            )
          });
        } else {
          set({ items: [...items, { ...item, quantity: qty }] });
        }
      },
      removeItem: (productId) =>
        set({
          items: get().items.filter((i) => i.productId !== productId),
          selectedIds: get().selectedIds.filter((id) => id !== productId)
        }),
      updateQuantity: (productId, qty) => {
        if (qty < 1) return;
        set({
          items: get().items.map((i) =>
            i.productId === productId ? { ...i, quantity: qty } : i
          )
        });
      },
      clearAll: () => set({ items: [], selectedIds: [] }),
      clearSelected: () => {
        const sel = get().selectedIds;
        set({
          items: get().items.filter((i) => !sel.includes(i.productId)),
          selectedIds: []
        });
      },
      toggleSelect: (productId) => {
        const sel = get().selectedIds;
        set({
          selectedIds: sel.includes(productId)
            ? sel.filter((id) => id !== productId)
            : [...sel, productId]
        });
      },
      toggleSelectAll: (select) => {
        set({ selectedIds: select ? get().items.map((i) => i.productId) : [] });
      },
      setSelected: (ids) => set({ selectedIds: ids }),
      totalItems: () => get().items.reduce((s, i) => s + i.quantity, 0),
      totalSelected: () => {
        const sel = get().selectedIds;
        return get().items
          .filter((i) => sel.includes(i.productId))
          .reduce((s, i) => s + i.giaHienTai * i.quantity, 0);
      },
      selectedItems: () => {
        const sel = get().selectedIds;
        return get().items.filter((i) => sel.includes(i.productId));
      },
      setHydrated: () => set({ hydrated: true })
    }),
    {
      name: 'thluxury-cart',
      partialize: (s) => ({ items: s.items, selectedIds: s.selectedIds }),
      onRehydrateStorage: () => (state) => state?.setHydrated()
    }
  )
);
