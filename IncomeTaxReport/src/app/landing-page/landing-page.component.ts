import { Component } from '@angular/core';
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

  constructor(private fb: FormBuilder, private landingService: LandingService) {
    this.financialYears = this.getFinancialYears(3);

    this.form = this.fb.group({
      name: ['', Validators.required],
      post: ['', Validators.required],
      department: ['', Validators.required],
      pan: ['', Validators.required],
      employeeId: ['', Validators.required],

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
      this.landingService.saveEmployee(this.form.value).subscribe({
        next: () => {
          this.saving = false;
          console.log('Landing form submitted', this.form.value);
          alert('Details saved successfully.');
        },
        error: (err) => {
          this.saving = false;
          console.error('Save failed', err);
          alert('Failed to save the details.');
        }
      });
    } else {
      this.form.markAllAsTouched();
      const empty = this.getEmptyFields();
      const msg = empty.length ? 'Please fill: ' + empty.join(', ') : 'Please complete the form.';
      alert(msg);
    }
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
      employeeId: 'Employee ID',
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
