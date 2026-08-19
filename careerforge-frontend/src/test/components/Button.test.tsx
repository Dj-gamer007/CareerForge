import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Button } from '@/components/ui/Button';

describe('Button Component', () => {
  it('renders button text correctly', () => {
    render(<Button>Submit Application</Button>);
    expect(screen.getByText('Submit Application')).toBeInTheDocument();
  });

  it('handles loading state properly', () => {
    render(<Button isLoading>Submit Application</Button>);
    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
  });
});
