import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent {
  constructor(private router: Router) {}

  goHome() {
    this.router.navigate(['/']);
  }

  goToNewForm() {
    this.router.navigate(['/landing']);
  }

  aboutUs() {
    // Scroll to the About section on the single landing page.
    this.router.navigate(['/'], { fragment: 'about' });
  }
}
