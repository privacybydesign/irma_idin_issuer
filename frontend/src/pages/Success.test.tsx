import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import SuccessPage from './Success';
import '../i18n';

const config = {
  idin_server_url: 'http://localhost',
  irma_server_url: 'http://localhost',
  language: 'en',
  idin_credential_id: 'irma-demo.idin.idin',
};

beforeEach(() => {
  vi.stubGlobal(
    'fetch',
    vi.fn(() => Promise.resolve({ json: () => Promise.resolve(config) } as Response))
  );
});

describe('SuccessPage', () => {
  it('shows the confirmation message and a close button once config is loaded', async () => {
    render(
      <MemoryRouter initialEntries={['/success']}>
        <SuccessPage />
      </MemoryRouter>
    );

    expect(await screen.findByText(/added to your Yivi wallet/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /close/i })).toBeInTheDocument();
  });

  it('closes the window when the close button is clicked', async () => {
    const close = vi.fn();
    vi.stubGlobal('close', close);

    render(
      <MemoryRouter initialEntries={['/success']}>
        <SuccessPage />
      </MemoryRouter>
    );

    const button = await screen.findByRole('button', { name: /close/i });
    button.click();

    await waitFor(() => expect(close).toHaveBeenCalledTimes(1));
  });
});
