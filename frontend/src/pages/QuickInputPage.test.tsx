import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import type { ManualInboundEventListDto } from '../api';
import { fakeRootStore, renderApp } from '../test';

const LIST: ManualInboundEventListDto = {
  totalPages: 2,
  totalCount: 12,
  items: [
    {
      id: 'appointment',
      text: 'Dentist on 1 December at 10, then get the bonus booklet stamped',
      status: 'ANALYZED',
      createdDate: '2026-11-25T10:00:00+01:00',
      elements: [
        { id: 'e1', type: 'APPOINTMENT' },
        { id: 'e2', type: 'TASK' },
      ],
    },
    {
      id: 'pending',
      text: 'Milk is empty',
      status: 'RECEIVED',
      createdDate: '2026-11-25T10:05:00+01:00',
      elements: [],
    },
    {
      id: 'failed',
      text: 'Plant the balcony',
      status: 'FAILED',
      createdDate: '2026-11-24T18:42:00+01:00',
      elements: [],
    },
  ],
};

async function renderPage(list?: ManualInboundEventListDto) {
  const fakes = fakeRootStore(list);
  const result = await renderApp('/', { rootStore: fakes.rootStore });
  return { ...fakes, ...result, store: fakes.rootStore.inboundEventStore };
}

function entry(entries: HTMLElement[], index: number): HTMLElement {
  const element = entries[index];
  if (element === undefined) {
    throw new Error(`entry ${String(index)} missing`);
  }
  return element;
}

function textField() {
  return screen.getByRole('textbox', { name: 'Eingabe' });
}

describe('QuickInputPage', () => {
  it('that opening the page should result into the recent inputs with status and elements', async () => {
    // given
    const list = LIST;

    // when
    const { push } = await renderPage(list);

    // then
    const listSection = await screen.findByRole('region', { name: 'Letzte Eingaben' });
    const entries = within(listSection).getAllByRole('listitem');
    expect(entries).toHaveLength(3);
    expect(within(entry(entries, 0)).getByText('Analysiert')).toBeInTheDocument();
    expect(within(entry(entries, 0)).getByText('Termin')).toBeInTheDocument();
    expect(within(entry(entries, 0)).getByText('Aufgabe')).toBeInTheDocument();
    expect(within(entry(entries, 1)).getByText('Wird analysiert')).toBeInTheDocument();
    expect(within(entry(entries, 2)).getByText('Fehlgeschlagen')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mehr laden' })).toBeInTheDocument();
    expect(push.connect).toHaveBeenCalledTimes(1);
  });

  it('that no inputs should result into a hint for the first input', async () => {
    // given
    const list = undefined;

    // when
    await renderPage(list);

    // then
    expect(await screen.findByText('Noch keine Eingaben. Deine erste Eingabe erscheint hier.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Mehr laden' })).not.toBeInTheDocument();
  });

  it('that submitting an input should result into a sent input and a reset form', async () => {
    // given
    const { lunaCore } = await renderPage();
    await userEvent.type(textField(), '  Pay the invoice  ');
    await userEvent.click(screen.getByRole('button', { name: 'Rechnung' }));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Absenden' }));

    // then
    expect(lunaCore.POST).toHaveBeenCalledWith('/inbound-events/manual', {
      body: { text: 'Pay the invoice', typeHint: 'INVOICE' },
    });
    await waitFor(() => {
      expect(textField()).toHaveValue('');
    });
    expect(screen.getByRole('button', { name: 'Automatisch' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('that pressing ctrl and enter should result into a sent input without type hint', async () => {
    // given
    const { lunaCore } = await renderPage();
    await userEvent.type(textField(), 'Milk is empty');

    // when
    await userEvent.keyboard('{Control>}{Enter}{/Control}');

    // then
    await waitFor(() => {
      expect(lunaCore.POST).toHaveBeenCalledWith('/inbound-events/manual', { body: { text: 'Milk is empty' } });
    });
  });

  it('that pressing enter alone should result into a new line instead of sending', async () => {
    // given
    const { lunaCore } = await renderPage();
    await userEvent.type(textField(), 'First line');

    // when
    await userEvent.keyboard('{Enter}');

    // then
    expect(textField()).toHaveValue('First line\n');
    expect(lunaCore.POST).not.toHaveBeenCalled();
  });

  it('that submitting an empty text should result into a hint at the field without request', async () => {
    // given
    const { lunaCore } = await renderPage();
    await userEvent.type(textField(), '   ');

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Absenden' }));

    // then
    expect(screen.getByText('Bitte gib einen Text ein.')).toBeInTheDocument();
    expect(textField()).toHaveAttribute('aria-invalid', 'true');
    expect(lunaCore.POST).not.toHaveBeenCalled();
  });

  it('that typing after an empty submission should result into the regular help text', async () => {
    // given
    await renderPage();
    await userEvent.click(screen.getByRole('button', { name: 'Absenden' }));

    // when
    await userEvent.type(textField(), 'A');

    // then
    expect(screen.getByText('Freitext, höchstens 10.000 Zeichen.')).toBeInTheDocument();
    expect(screen.getByText('1 / 10.000')).toBeInTheDocument();
  });

  it('that a rejected input should result into a hint at the field and a kept text', async () => {
    // given
    const { lunaCore } = await renderPage();
    lunaCore.POST.mockResolvedValueOnce({
      data: { id: '' },
      error: undefined,
      response: new Response(null, { status: 400 }),
    });
    await userEvent.type(textField(), 'Something');

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Absenden' }));

    // then
    expect(
      await screen.findByText('Luna konnte die Eingabe nicht annehmen. Prüfe den Text und versuche es erneut.'),
    ).toBeInTheDocument();
    expect(textField()).toHaveValue('Something');
  });

  it('that a failed submission should result into an error message and a kept text', async () => {
    // given
    const { lunaCore } = await renderPage();
    lunaCore.POST.mockRejectedValueOnce(new TypeError('Failed to fetch'));
    await userEvent.type(textField(), 'Something');

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Absenden' }));

    // then
    expect(await screen.findByRole('alert')).toHaveTextContent('Senden fehlgeschlagen.');
    expect(textField()).toHaveValue('Something');
  });

  it('that loading more should result into another request for older inputs', async () => {
    // given
    const { lunaCore } = await renderPage(LIST);

    // when
    await userEvent.click(await screen.findByRole('button', { name: 'Mehr laden' }));

    // then
    expect(lunaCore.GET).toHaveBeenCalledTimes(2);
  });

  it('that a failed list should result into an error with retry', async () => {
    // given
    const fakes = fakeRootStore();
    fakes.lunaCore.GET.mockRejectedValueOnce(new TypeError('Failed to fetch'));
    await renderApp('/', { rootStore: fakes.rootStore });

    // when
    await userEvent.click(await screen.findByRole('button', { name: 'Erneut versuchen' }));

    // then
    expect(fakes.lunaCore.GET).toHaveBeenCalledTimes(2);
    await waitFor(() => {
      expect(screen.queryByText('Die Liste konnte nicht geladen werden.')).not.toBeInTheDocument();
    });
  });

  it('that an interrupted live connection should result into a hint', async () => {
    // given
    const { push } = await renderPage();

    // when
    act(() => {
      push.handlers[0]?.onDisconnected();
    });

    // then
    expect(screen.getByRole('status')).toHaveTextContent('Live-Aktualisierung unterbrochen');
  });

  it('that leaving the page should result into a closed live connection', async () => {
    // given
    const { push, router } = await renderPage();

    // when
    await act(() => router.navigate('/tasks'));

    // then
    expect(push.close).toHaveBeenCalledTimes(1);
  });
});
