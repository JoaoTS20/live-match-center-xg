import { Component, input, output } from '@angular/core';
import { PitchCoordinates } from '../../models/match-event.model';

@Component({
  selector: 'app-pitch',          // HTML tag name used in parent templates: <app-pitch />
  templateUrl: './pitch.html',
  styleUrl: './pitch.css',
})
export class PitchComponent {
  // Data flowing IN from the parent component
  coordinates = input.required<PitchCoordinates>();

  // Event flowing OUT to the parent whenever the user clicks the pitch
  coordinatesChange = output<PitchCoordinates>();

  onPitchClick(event: MouseEvent): void {
    const svg = event.currentTarget as SVGSVGElement;
    const rect = svg.getBoundingClientRect();

    // Convert pixel offset into 0..100 percentages
    const rawX = ((event.clientX - rect.left) / rect.width) * 100;
    const rawY = ((event.clientY - rect.top) / rect.height) * 100;

    // Clamp to [0, 100] and round to 1 decimal place
    const x = Math.round(Math.max(0, Math.min(100, rawX)) * 10) / 10;
    const y = Math.round(Math.max(0, Math.min(100, rawY)) * 10) / 10;

    this.coordinatesChange.emit({ x, y });
  }
}
