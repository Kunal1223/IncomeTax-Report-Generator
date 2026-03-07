import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly siteOrigin = 'https://mycleartax.com';

  constructor(
    private router: Router,
    @Inject(DOCUMENT) private document: Document
  ) {
    this.updateCanonicalForCurrentUrl();

    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => this.updateCanonicalForCurrentUrl());
  }

  private updateCanonicalForCurrentUrl() {
    const urlPath = this.router.url.split('?')[0].split('#')[0] || '/';
    const canonicalUrl = new URL(urlPath, this.siteOrigin).toString();

    let linkEl = this.document.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!linkEl) {
      linkEl = this.document.createElement('link');
      linkEl.setAttribute('rel', 'canonical');
      this.document.head.appendChild(linkEl);
    }

    linkEl.setAttribute('href', canonicalUrl);
  }
}
