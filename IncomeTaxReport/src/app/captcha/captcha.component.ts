import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'captcha-widget',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './captcha.component.html',
  styleUrls: ['./captcha.component.css']
})
export class CaptchaComponent {
  @Output() verified = new EventEmitter<boolean>();

  question = '';
  expected = 0;
  answerInput = '';
  success = false;
  errorMessage = '';

  constructor() {
    this.generate();
  }

  generate() {
    const a = Math.floor(Math.random() * 9) + 1;
    const b = Math.floor(Math.random() * 9) + 1;
    this.question = `${a} + ${b} = ?`;
    this.expected = a + b;
    this.answerInput = '';
    this.success = false;
    this.verified.emit(false);
  }

  check() {
    const val = Number(this.answerInput);
    if (!isNaN(val) && val === this.expected) {
      this.success = true;
      this.errorMessage = '';
      this.verified.emit(true);
    } else {
      this.success = false;
      this.errorMessage = 'Please enter the correct number.';
      this.verified.emit(false);
    }
  }
}
