import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from './toast/toast.component';
import { NavbarComponent } from './navbar/navbar.component';
import { SeoService } from './services/seo.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastComponent, NavbarComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('IncomeTaxReport');

  constructor(_seo: SeoService) {
    // Ensure the SEO service is instantiated.
  }
}
