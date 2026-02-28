import { Routes } from '@angular/router';
import { LandingPageComponent } from './landing-page/landing-page.component';
import { HomePageComponent } from './home-page/home-page.component';

export const routes: Routes = [
	{ path: '', component: HomePageComponent },
	{ path: 'landing', component: LandingPageComponent }
];
