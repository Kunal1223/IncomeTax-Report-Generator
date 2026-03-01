import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ToastService } from '../toast/toast.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent {
  constructor(private router: Router, private toast: ToastService) {}

  goHome() {
    this.router.navigate(['/']);
  }

  goToNewForm() {
    this.router.navigate(['/landing']);
  }

  aboutUs() {
    this.toast.info('P&A — Income Tax Report Generator');
  }
}
