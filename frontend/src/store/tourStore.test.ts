import { beforeEach, describe, expect, it } from 'vitest';

import { useTourStore } from './tourStore';

function reset() {
  localStorage.clear();
  useTourStore.setState({ completed: false, running: false, stepIndex: 0 });
}

describe('tourStore', () => {
  beforeEach(reset);

  it('empieza sin completar y sin correr', () => {
    expect(useTourStore.getState().completed).toBe(false);
    expect(useTourStore.getState().running).toBe(false);
  });

  it('start() corre desde el primer paso', () => {
    useTourStore.getState().start();

    expect(useTourStore.getState().running).toBe(true);
    expect(useTourStore.getState().stepIndex).toBe(0);
  });

  it('next()/prev() mueven el paso sin bajar de cero', () => {
    useTourStore.getState().start();
    useTourStore.getState().next();
    useTourStore.getState().next();
    expect(useTourStore.getState().stepIndex).toBe(2);

    useTourStore.getState().prev();
    expect(useTourStore.getState().stepIndex).toBe(1);

    useTourStore.getState().prev();
    useTourStore.getState().prev();
    expect(useTourStore.getState().stepIndex).toBe(0);
  });

  it('skip() marca completado y detiene el tour', () => {
    useTourStore.getState().start();
    useTourStore.getState().skip();

    expect(useTourStore.getState().completed).toBe(true);
    expect(useTourStore.getState().running).toBe(false);
  });

  it('finish() tambien marca completado y detiene el tour', () => {
    useTourStore.getState().start();
    useTourStore.getState().finish();

    expect(useTourStore.getState().completed).toBe(true);
    expect(useTourStore.getState().running).toBe(false);
  });

  it('solo persiste "completed": running y stepIndex no sobreviven una recarga', () => {
    useTourStore.getState().start();
    useTourStore.getState().next();

    const guardado = JSON.parse(localStorage.getItem('luma-tour') ?? '{}');
    expect(guardado.state).toEqual({ completed: false });
  });
});
