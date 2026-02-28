import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: number;
  message: string;
  type: ToastType;
  durationMs: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  private readonly _toasts = signal<Toast[]>([]);

  readonly toasts = this._toasts.asReadonly();

  show(message: string, type: ToastType = 'info', durationMs = 10_000): number {
    const id = this.nextId++;
    const toast: Toast = {
      id,
      message,
      type,
      durationMs
    };

    this._toasts.update((current) => [toast, ...current]);

    window.setTimeout(() => {
      this.dismiss(id);
    }, durationMs);

    return id;
  }

  success(message: string, durationMs = 10_000) {
    return this.show(message, 'success', durationMs);
  }

  error(message: string, durationMs = 10_000) {
    return this.show(message, 'error', durationMs);
  }

  warning(message: string, durationMs = 10_000) {
    return this.show(message, 'warning', durationMs);
  }

  info(message: string, durationMs = 10_000) {
    return this.show(message, 'info', durationMs);
  }

  dismiss(id: number) {
    this._toasts.update((current) => current.filter((t) => t.id !== id));
  }

  clear() {
    this._toasts.set([]);
  }
}
