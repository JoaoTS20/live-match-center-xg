import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class VoiceService {
  readonly enabled = signal(true);

  speak(text: string): void {
    if (!this.enabled() || !text.trim()) {
      return;
    }
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
      return;
    }

    const utterance = new SpeechSynthesisUtterance(text);
    // Match the language your Gemini prompt generates (e.g. 'en-US' or 'pt-PT')
    utterance.lang = 'en-US';
    utterance.rate = 1.05; // Slightly brisker commentator cadence

    window.speechSynthesis.speak(utterance);
  }

  toggle(): void {
    const next = !this.enabled();
    this.enabled.set(next);

    if (!next && typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
  }
}