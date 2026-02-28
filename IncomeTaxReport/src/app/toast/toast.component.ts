import { Component, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.css']
})
export class ToastComponent {
  constructor(public toastService: ToastService) {}

  // Render newest first.
  readonly toasts = computed(() => this.toastService.toasts());

  trackById(_: number, t: { id: number }) {
    return t.id;
  }

  dismiss(id: number) {
    this.toastService.dismiss(id);
  }
}
