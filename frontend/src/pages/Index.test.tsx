import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import IndexPage from './Index';
import '../i18n';

// Render the page before the runtime config has loaded.
vi.mock('../hooks', () => ({ useConfig: () => null }));

describe('IndexPage', () => {
  it('shows a loading state until the config is available', () => {
    render(<IndexPage />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });
});
