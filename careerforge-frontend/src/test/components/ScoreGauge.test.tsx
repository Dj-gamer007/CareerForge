import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ScoreGauge } from '@/components/ui/ScoreGauge';

describe('ScoreGauge Component', () => {
  it('renders percentage and Strong Match badge for high scores (>= 75%)', () => {
    render(<ScoreGauge score={88} />);
    expect(screen.getByText('88%')).toBeInTheDocument();
    expect(screen.getByText('Strong Match')).toBeInTheDocument();
  });

  it('renders Moderate Match for scores between 50% and 74%', () => {
    render(<ScoreGauge score={62} />);
    expect(screen.getByText('62%')).toBeInTheDocument();
    expect(screen.getByText('Moderate Match')).toBeInTheDocument();
  });

  it('renders Low Match for scores below 50%', () => {
    render(<ScoreGauge score={35} />);
    expect(screen.getByText('35%')).toBeInTheDocument();
    expect(screen.getByText('Low Match')).toBeInTheDocument();
  });
});
