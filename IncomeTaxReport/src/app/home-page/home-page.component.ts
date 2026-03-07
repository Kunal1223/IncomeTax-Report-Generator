import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Meta, Title } from '@angular/platform-browser';
import { LandingService } from '../services/landing.service';
import { ToastService } from '../toast/toast.service';

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

  readonly seoTitle =
    'Income Tax Calculator for Salaried Persons | Download PDF Schedule | mycleartax.com';
  readonly seoDescription =
    'Use mycleartax.com (MyClearText) to calculate your income tax for AY 2025-26. Compare Old vs New Tax Regimes and download your professional tax calculation sheet in PDF format instantly.';

  readonly faqJsonLd = JSON.stringify(
    {
      '@context': 'https://schema.org',
      '@type': 'FAQPage',
      mainEntity: [
        {
          '@type': 'Question',
          name: 'Is this calculator free?',
          acceptedAnswer: {
            '@type': 'Answer',
            text: 'Yes, the tool and PDF download are 100% free for all users.'
          }
        },
        {
          '@type': 'Question',
          name: 'Which tax regime is better for me?',
          acceptedAnswer: {
            '@type': 'Answer',
            text: 'Our tool provides a direct comparison based on your specific deductions to help you decide.'
          }
        },
        {
          '@type': 'Question',
          name: 'Do I need to sign up?',
          acceptedAnswer: {
            '@type': 'Answer',
            text: 'No, you can use the calculator and get your PDF without creating an account.'
          }
        },
        {
          '@type': 'Question',
          name: 'How much is the Standard Deduction?',
          acceptedAnswer: {
            '@type': 'Answer',
            text: 'For salaried individuals, a standard deduction of ₹75,000 is applicable under the New Regime for FY 2025-26.'
          }
        }
      ]
    },
    null,
    0
  );

  readonly appJsonLd = JSON.stringify(
    {
      '@context': 'https://schema.org',
      '@type': 'SoftwareApplication',
      name: 'mycleartax.com (MyClearText) Income Tax Calculator',
      applicationCategory: 'FinanceApplication',
      operatingSystem: 'Web',
      offers: { '@type': 'Offer', price: '0', priceCurrency: 'INR' },
      description:
        'Free income tax calculator for salaried persons. Compare old vs new regimes and download a PDF tax schedule.'
    },
    null,
    0
  );

  constructor(
    private router: Router,
    private landingService: LandingService,
    private toast: ToastService,
    private title: Title,
    private meta: Meta
  ) {
    this.applySeoTags();
  }

  goHome() {
    this.router.navigate(['/']);
  }

  goToNewForm() {
    this.router.navigate(['/landing']);
  }

  aboutUs() {
    // Kept for backward compatibility (navbar now scrolls).
    this.toast.info('mycleartax.com — Income Tax Calculator for Salaried Persons');
  }

  private applySeoTags() {
    this.title.setTitle(this.seoTitle);

    this.meta.updateTag({ name: 'description', content: this.seoDescription });
    this.meta.updateTag({ name: 'keywords', content: 'MyClearText, mycleartax, income tax calculator, salaried persons, old vs new regime, AY 2025-26, PDF tax schedule' });
    this.meta.updateTag({ name: 'robots', content: 'index,follow' });

    // Social previews
    this.meta.updateTag({ property: 'og:title', content: this.seoTitle });
    this.meta.updateTag({ property: 'og:description', content: this.seoDescription });
    this.meta.updateTag({ property: 'og:type', content: 'website' });
    this.meta.updateTag({ property: 'og:url', content: 'https://mycleartax.com/' });

    this.meta.updateTag({ name: 'twitter:card', content: 'summary' });
    this.meta.updateTag({ name: 'twitter:title', content: this.seoTitle });
    this.meta.updateTag({ name: 'twitter:description', content: this.seoDescription });
    this.meta.updateTag({ name: 'twitter:url', content: 'https://mycleartax.com/' });
  }

  toggleEdit() {
    this.editOpen = true;
  }

  searchAndEdit() {
    const pan = (this.searchPan || '').trim().toUpperCase();
    if (!pan) {
      this.toast.warning('Please enter the PAN number');
      return;
    }

    this.searching = true;
    this.landingService.getEmployeeByPan(pan).subscribe({
      next: (emp: any) => {
        this.searching = false;
        if (!emp || !emp.id) {
          this.toast.error('PAN is incorrect or not registered');
          return;
        }
        this.router.navigate(['/landing'], { queryParams: { pan } });
      },
      error: (err) => {
        this.searching = false;
        const msg = err?.error?.message || 'PAN is incorrect or not registered';
        this.toast.error(msg);
      }
    });
  }
}
