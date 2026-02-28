import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LandingService } from '../services/landing.service';

@Component({
  selector: 'home-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './home-page.component.html',
  styleUrls: ['./home-page.component.css']
})
export class HomePageComponent {
  editOpen = false;
  searchPan = '';
  searching = false;

  constructor(private router: Router, private landingService: LandingService) {}

  goToNewForm() {
    this.router.navigate(['/landing']);
  }

  toggleEdit() {
    this.editOpen = true;
  }

  searchAndEdit() {
    const pan = (this.searchPan || '').trim();
    if (!pan) {
      alert('Please enter the PAN number');
      return;
    }

    this.searching = true;
    this.landingService.getEmployeeByPan(pan).subscribe({
      next: (emp: any) => {
        this.searching = false;
        if (!emp || !emp.id) {
          alert('PAN is incorrect or not registered');
          return;
        }
        this.router.navigate(['/landing'], { queryParams: { pan } });
      },
      error: (err) => {
        this.searching = false;
        const msg = err?.error?.message || 'PAN is incorrect or not registered';
        alert(msg);
      }
    });
  }
}
