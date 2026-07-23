import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import SuccessPage from './Success';
import '../i18n';

describe('SuccessPage', () => {
  it('shows the confirmation message and a close button', async () => {
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
