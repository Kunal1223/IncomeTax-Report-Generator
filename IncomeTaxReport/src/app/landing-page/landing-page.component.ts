import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormsModule } from '@angular/forms';
import { CaptchaComponent } from '../captcha/captcha.component';
import { LandingService } from '../services/landing.service';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, throwError } from 'rxjs';
import { catchError, finalize, switchMap } from 'rxjs/operators';
import { ToastService } from '../toast/toast.service';

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
  @ViewChild(CaptchaComponent) captchaComp?: CaptchaComponent;

  constructor(
    private fb: FormBuilder,
    private landingService: LandingService,
    private router: Router,
    private route: ActivatedRoute,
    private toast: ToastService
  ) {
    this.financialYears = this.getFinancialYears(3);

    this.form = this.fb.group({
      id: [null],
      name: ['', Validators.required],
      post: ['', Validators.required],
      department: ['', Validators.required],
      pan: ['', [Validators.required, Validators.pattern(/^[A-Z]{5}[0-9]{4}[A-Z]$/i)]],
      employerTan: ['', Validators.required],
      treasuryName: ['', Validators.required],

      mobileNumber: ['', [Validators.pattern(/^\d{10}$/)]],

      basicPay: [null, [Validators.required, Validators.min(0)]],
      da: [null, [Validators.required, Validators.min(0)]],
      ta: [null, [Validators.required, Validators.min(0)]],
      daOnTransportAllowance: [null, [Validators.min(0)]],
      hra: [null, [Validators.required, Validators.min(0)]],
      medicalAllowances: [null, [Validators.required, Validators.min(0)]],

      specialPay: [null, [Validators.min(0)]],
      arrearDearnessAllowance: [null, [Validators.min(0)]],
      arrearPayAndAllowances: [null, [Validators.min(0)]],

      incomeFromHouseRent: [null, [Validators.min(0)]],
      interestOnHousingLoan: [null, [Validators.min(0)]],

      interestOnSaving: [null, [Validators.min(0)]],
      interestOnFixedDeposit: [null, [Validators.min(0)]],
      anyOtherIncome: [null, [Validators.min(0)]],
      financialYear: ['', Validators.required],

      place: [''],
      reportDate: ['', [Validators.pattern(/^\d{8}$/)]],
      incomeTaxPaid: [null, [Validators.min(0)]]
    });
    // no fallback captcha in use

    const panFromUrl = (this.route.snapshot.queryParamMap.get('pan') || '').trim();
    if (panFromUrl) {
      this.prefillByPan(panFromUrl);
    }
  }

  get f() {
    return this.form.controls;
  }

  goHome() {
    this.router.navigate(['/']);
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
      this.toast.warning(this.captchaWarning);
      return;
    }

    if (this.form.valid) {
      this.saving = true;

      const payload = { ...this.form.value };
      payload.pan = (payload?.pan || '').trim().toUpperCase();
      payload.mobileNumber = (payload?.mobileNumber || '').trim();
      payload.place = (payload?.place || '').trim();
      payload.reportDate = (payload?.reportDate || '').trim();
      const hasId = !!payload?.id;

      const save$ = hasId
        ? this.landingService.updateEmployee(payload.id, payload)
        : this.landingService.getEmployeeByPan(payload.pan).pipe(
            // If PAN exists, block save for New Form.
            switchMap((emp: any) => {
              if (emp && emp.id) {
                this.toast.warning(
                  'Hey, this PAN number is already existing in our database. Please use Edit/Update to modify it.'
                );
                return EMPTY;
              }
              return this.landingService.saveEmployee(payload);
            }),
            // If PAN not found (404), allow create; otherwise surface error.
            catchError((err: any) => {
              if (err?.status === 404) {
                return this.landingService.saveEmployee(payload);
              }
              return throwError(() => err);
            })
          );

      const req$ = save$.pipe(
        switchMap((res: any) => {
          if (!res || !res.id) return EMPTY;
          return this.landingService.generateReport(res.id).pipe(
            switchMap((r: any) => {
              const downloadUrl = r && r.downloadUrl ? r.downloadUrl : null;
              if (!downloadUrl) {
                return throwError(() => new Error('Report generation failed'));
              }
              const full = this.landingService.serverRoot() + downloadUrl;
              const a = document.createElement('a');
              a.href = full;
              a.target = '_blank';
              a.rel = 'noopener';
              document.body.appendChild(a);
              a.click();
              a.remove();

              this.toast.success('Details saved and report generated successfully.');
              return EMPTY;
            }),
            catchError((err2: any) => {
              // Data may be saved, but report generation can fail.
              const msg = err2?.error?.message || err2?.message || 'Report generation failed.';
              this.toast.error('Saved, but ' + msg);
              return EMPTY;
            })
          );
        })
      );

      req$
        .pipe(
          finalize(() => {
            this.saving = false;
          })
        )
        .subscribe({
          next: () => {},
          error: (err) => {
            console.error('Save failed', err);
            const msg = err?.error?.message || err?.message || 'Failed to save the details.';
            this.toast.error(msg);
          }
        });
    } else {
      this.form.markAllAsTouched();
      const empty = this.getEmptyFields();
      const msg = empty.length ? 'Please fill: ' + empty.join(', ') : 'Please complete the form.';
      this.toast.warning(msg);
    }
  }

  private resetFormToNew() {
    this.form.reset({
      id: null,
      name: '',
      post: '',
      department: '',
      pan: '',
      employerTan: '',
      treasuryName: '',

      mobileNumber: '',
      basicPay: null,
      da: null,
      ta: null,
      daOnTransportAllowance: null,
      hra: null,
      medicalAllowances: null,
      specialPay: null,
      arrearDearnessAllowance: null,
      arrearPayAndAllowances: null,
      incomeFromHouseRent: null,
      interestOnHousingLoan: null,
      interestOnSaving: null,
      interestOnFixedDeposit: null,
      anyOtherIncome: null,
      financialYear: '',

      place: '',
      reportDate: '',
      incomeTaxPaid: null
    });
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.captchaVerified = false;
    this.captchaComp?.generate();
  }

  private prefillByPan(pan: string) {
    const value = (pan || '').trim();
    if (!value) return;

    this.landingService.getEmployeeByPan(value).subscribe({
      next: (emp: any) => {
        if (!emp || !emp.id) {
          this.toast.error('PAN is incorrect or not registered');
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

          mobileNumber: emp.mobileNumber ?? '',
          basicPay: emp.basicPay ?? 0,
          da: emp.da ?? 0,
          ta: emp.ta ?? 0,
          daOnTransportAllowance: emp.daOnTransportAllowance ?? 0,
          hra: emp.hra ?? 0,
          medicalAllowances: emp.medicalAllowances ?? 0,
          specialPay: emp.specialPay ?? 0,
          arrearDearnessAllowance: emp.arrearDearnessAllowance ?? 0,
          arrearPayAndAllowances: emp.arrearPayAndAllowances ?? 0,

          incomeFromHouseRent: emp.incomeFromHouseRent ?? 0,
          interestOnHousingLoan: emp.interestOnHousingLoan ?? 0,

          interestOnSaving: emp.interestOnSaving ?? 0,
          interestOnFixedDeposit: emp.interestOnFixedDeposit ?? 0,
          anyOtherIncome: emp.anyOtherIncome ?? 0,
          financialYear: emp.financialYear ?? '',

          place: emp.place ?? '',
          reportDate: emp.reportDate ?? '',
          incomeTaxPaid: emp.incomeTaxPaid ?? 0
        });

        this.captchaVerified = false;
        this.captchaComp?.generate();
      },
      error: (err) => {
        console.error('Search by PAN failed', err);
        const msg = err?.error?.message || 'PAN is incorrect or not registered';
        this.toast.error(msg);
      }
    });
  }

  onCaptchaVerified(v: boolean) {
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
      daOnTransportAllowance: 'DA on Transport Allowance',
      hra: 'HRA',
      medicalAllowances: 'Medical Allowances',
      specialPay: 'Special Pay / Bonus / Other Allowances',
      arrearDearnessAllowance: 'Arrear of Dearness Allowance',
      arrearPayAndAllowances: 'Arrear of Pay and Allowances',

      incomeFromHouseRent: 'Income from House Rent',
      interestOnHousingLoan: 'Interest on Housing Loan',

      interestOnSaving: 'Interest on Saving A/c',
      interestOnFixedDeposit: 'Interest on Fixed Deposit / Recurring Deposit',
      anyOtherIncome: 'Any other Income',
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
