import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormsModule } from '@angular/forms';
import { CaptchaComponent } from '../captcha/captcha.component';
import { LandingService } from '../services/landing.service';

@Component({
  selector: 'landing-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, CaptchaComponent],
  templateUrl: './landing-page.component.html',
  styleUrls: ['./landing-page.component.css']
})
export class LandingPageComponent {
  captchaVerified = false;
  captchaWarning = '';
  form: any;
  financialYears: string[] = [];
  saving = false;
  searching = false;
  mode: 'new' | 'edit' = 'new';
  searchPan = '';
  @ViewChild(CaptchaComponent) captchaComp?: CaptchaComponent;

  constructor(private fb: FormBuilder, private landingService: LandingService) {
    this.financialYears = this.getFinancialYears(3);

    this.form = this.fb.group({
      id: [null],
      name: ['', Validators.required],
      post: ['', Validators.required],
      department: ['', Validators.required],
      pan: ['', Validators.required],
      employerTan: ['', Validators.required],
      treasuryName: ['', Validators.required],

      basicPay: [null, [Validators.required, Validators.min(0)]],
      da: [null, [Validators.required, Validators.min(0)]],
      ta: [null, [Validators.required, Validators.min(0)]],
      hra: [null, [Validators.required, Validators.min(0)]],
      medicalAllowances: [null, [Validators.required, Validators.min(0)]],
      financialYear: ['', Validators.required]
    });
    // no fallback captcha in use
  }

  get f() {
    return this.form.controls;
  }

  private getFinancialYears(count: number): string[] {
    const now = new Date();
    const month = now.getMonth() + 1; // 1..12
    const year = now.getFullYear();
    // If month >= Apr (4), current FY starts this calendar year
    let currentStart = month >= 4 ? year : year - 1;
    const years: string[] = [];
    for (let i = 0; i < count; i++) {
      const y1 = currentStart - i;
      const y2 = y1 + 1;
      years.push(`${y1}-${y2}`);
    }
    return years;
  }

  onSubmit() {
    this.captchaWarning = '';
    if (!this.captchaVerified) {
      this.captchaWarning = 'Please complete the captcha verification before submitting.';
      alert(this.captchaWarning);
      return;
    }

    if (this.form.valid) {
      this.saving = true;
      const payload = this.form.value;
      const hasId = !!payload?.id;
      const req$ = hasId ? this.landingService.updateEmployee(payload.id, payload) : this.landingService.saveEmployee(payload);

      req$.subscribe({
        next: (res) => {
          this.saving = false;
          console.log('Landing form submitted', this.form.value, res);
          alert('Details saved successfully.');
          // Trigger report generation and download
          const id = res && res.id ? res.id : null;
          if (id) {
            this.landingService.generateReport(id).subscribe({
              next: (r: any) => {
                const downloadUrl = r && r.downloadUrl ? r.downloadUrl : null;
                if (downloadUrl) {
                  const full = this.landingService.serverRoot() + downloadUrl;
                  const a = document.createElement('a');
                  a.href = full;
                  a.target = '_blank';
                  a.rel = 'noopener';
                  document.body.appendChild(a);
                  a.click();
                  a.remove();
                }
              },
              error: (err2) => {
                console.error('Report generation failed', err2);
              }
            });
          }
          // Reset only for "new" mode; for edit mode keep the loaded data.
          if (this.mode === 'new') {
            this.form.reset();
            this.captchaVerified = false;
            this.captchaComp?.generate();
          }
        },
        error: (err) => {
          this.saving = false;
          console.error('Save failed', err);
          const msg = err?.error?.message || err?.message || 'Failed to save the details.';
          alert('Failed to save the details: ' + msg);
        }
      });
    } else {
      this.form.markAllAsTouched();
      const empty = this.getEmptyFields();
      const msg = empty.length ? 'Please fill: ' + empty.join(', ') : 'Please complete the form.';
      alert(msg);
    }
  }

  selectNewMode() {
    this.mode = 'new';
    this.searchPan = '';
    this.form.reset();
    this.captchaVerified = false;
    this.captchaComp?.generate();
    alert('Please fill the form');
  }

  selectEditMode() {
    this.mode = 'edit';
    this.searchPan = '';
    // Keep current form values until a search loads an employee.
  }

  onSearchByPan() {
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

        this.form.patchValue({
          id: emp.id ?? null,
          name: emp.name ?? '',
          post: emp.post ?? '',
          department: emp.department ?? '',
          pan: emp.pan ?? '',
          employerTan: emp.employerTan ?? '',
          treasuryName: emp.treasuryName ?? emp.tragary ?? '',
          basicPay: emp.basicPay ?? 0,
          da: emp.da ?? 0,
          ta: emp.ta ?? 0,
          hra: emp.hra ?? 0,
          medicalAllowances: emp.medicalAllowances ?? 0,
          financialYear: emp.financialYear ?? ''
        });

        // Require captcha again for updates too.
        this.captchaVerified = false;
        this.captchaComp?.generate();
      },
      error: (err) => {
        this.searching = false;
        console.error('Search by PAN failed', err);
        const msg = err?.error?.message || 'PAN is incorrect or not registered';
        alert(msg);
      }
    });
  }

  onCaptchaVerified(v: boolean) {
    console.log('onCaptchaVerified', v);
    this.captchaVerified = !!v;
  }

  

  private getEmptyFields(): string[] {
    const labels: { [key: string]: string } = {
      name: 'Full name',
      post: 'Post',
      department: 'Department',
      pan: 'PAN',
      employerTan: 'Employer TAN',
      treasuryName: 'Treasury Name',
      basicPay: 'Basic Pay',
      da: 'DA',
      ta: 'TA',
      hra: 'HRA',
      medicalAllowances: 'Medical Allowances',
      financialYear: 'Financial Year'
    };

    const empty: string[] = [];
    Object.keys(labels).forEach((key) => {
      const control = this.form.get(key);
      if (!control) return;
      const val = control.value;
      if (val === null || val === '' || val === undefined) {
        empty.push(labels[key]);
      }
    });
    return empty;
  }
}
