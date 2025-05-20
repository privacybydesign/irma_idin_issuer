import { render, screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import IndexPage from './Index';

vi.stubGlobal('fetch', vi.fn());

describe('IndexPage', () => {
  it('renders bank options', async () => {
    (fetch as any).mockResolvedValueOnce({
      json: () =>
        Promise.resolve({
          NL: [{ issuerID: 'ING', issuerName: 'ING Bank' }],
        }),
    });
    render(<IndexPage />);
    await waitFor(() => expect(screen.getByRole('option', { name: 'ING Bank' })).toBeInTheDocument());
  });
});
