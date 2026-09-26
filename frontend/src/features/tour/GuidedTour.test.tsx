import { act } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';

import { testIds } from '@/lib/testids';
import { useTourStore } from '@/store/tourStore';

import { GuidedTour } from './GuidedTour';
import { tourStepTitle, tourSteps } from './tourSteps';

function resetTour() {
  localStorage.clear();
  useTourStore.setState({ completed: false, running: false, stepIndex: 0 });
}

describe('GuidedTour', () => {
  beforeEach(resetTour);

  it('arranca solo la primera vez, en el primer paso', () => {
    render(<GuidedTour />);

    expect(screen.getByTestId(testIds.tour.root)).toBeInTheDocument();
    expect(screen.getByTestId(testIds.tour.title)).toHaveTextContent(tourStepTitle(tourSteps[0]!));
  });

  it('no arranca solo si ya se completo antes', () => {
    useTourStore.setState({ completed: true });

    render(<GuidedTour />);

    expect(screen.queryByTestId(testIds.tour.root)).not.toBeInTheDocument();
  });

  it('avanza con "Siguiente" y retrocede con "Anterior"', () => {
    render(<GuidedTour />);

    fireEvent.click(screen.getByTestId(testIds.tour.nextButton));
    expect(screen.getByTestId(testIds.tour.title)).toHaveTextContent(tourStepTitle(tourSteps[1]!));

    fireEvent.click(screen.getByTestId(testIds.tour.prevButton));
    expect(screen.getByTestId(testIds.tour.title)).toHaveTextContent(tourStepTitle(tourSteps[0]!));
  });

  it('omitir en cualquier paso cierra el tour, lo marca completado y no reaparece solo', () => {
    const { unmount } = render(<GuidedTour />);

    fireEvent.click(screen.getByTestId(testIds.tour.nextButton));
    fireEvent.click(screen.getByTestId(testIds.tour.skipButton));

    expect(screen.queryByTestId(testIds.tour.root)).not.toBeInTheDocument();
    expect(useTourStore.getState().completed).toBe(true);

    unmount();
    render(<GuidedTour />);
    expect(screen.queryByTestId(testIds.tour.root)).not.toBeInTheDocument();
  });

  it('terminar el ultimo paso tambien lo marca completado', () => {
    render(<GuidedTour />);

    for (let i = 0; i < tourSteps.length - 1; i += 1) {
      fireEvent.click(screen.getByTestId(testIds.tour.nextButton));
    }

    expect(screen.getByTestId(testIds.tour.title)).toHaveTextContent(
      tourStepTitle(tourSteps[tourSteps.length - 1]!),
    );

    fireEvent.click(screen.getByTestId(testIds.tour.finishButton));

    expect(screen.queryByTestId(testIds.tour.root)).not.toBeInTheDocument();
    expect(useTourStore.getState().completed).toBe(true);
  });

  it('se puede volver a arrancar manualmente aunque ya este completado, como hace Ajustes', () => {
    useTourStore.setState({ completed: true });
    render(<GuidedTour />);
    expect(screen.queryByTestId(testIds.tour.root)).not.toBeInTheDocument();

    act(() => {
      useTourStore.getState().start();
    });

    expect(screen.getByTestId(testIds.tour.root)).toBeInTheDocument();
    expect(screen.getByTestId(testIds.tour.title)).toHaveTextContent(tourStepTitle(tourSteps[0]!));
  });
});
