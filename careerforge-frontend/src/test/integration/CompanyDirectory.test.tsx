import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { CompanyDirectoryPage } from '@/features/recruiter/CompanyDirectoryPage';

const server = setupServer(
  http.get('/api/v1/companies', ({ request }) => {
    const url = new URL(request.url);
    const search = url.searchParams.get('search') || url.searchParams.get('name');

    if (search === 'NonExistentCompany') {
      return HttpResponse.json({
        success: true,
        message: 'Companies retrieved successfully',
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 9,
          number: 0,
        },
      });
    }

    return HttpResponse.json({
      success: true,
      message: 'Companies retrieved successfully',
      data: {
        content: [
          {
            id: 1,
            name: 'Acme Technologies',
            slug: 'acme-technologies',
            industry: 'Cloud Computing',
            location: 'San Francisco, CA',
            companySize: '500-1000',
            verificationStatus: 'VERIFIED',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 9,
        number: 0,
      },
    });
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Company Directory Page Integration', () => {
  it('renders verified companies and searches with search button', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CompanyDirectoryPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    expect(screen.getByText('Loading company directory...')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Acme Technologies')).toBeInTheDocument();
      expect(screen.getByText('Cloud Computing')).toBeInTheDocument();
    });

    // Test Search Button
    const searchInput = screen.getByPlaceholderText('Search companies by name...');
    const searchButton = screen.getByRole('button', { name: /search/i });

    fireEvent.change(searchInput, { target: { value: 'NonExistentCompany' } });
    fireEvent.click(searchButton);

    await waitFor(() => {
      expect(screen.getByText('No companies found')).toBeInTheDocument();
      expect(screen.getByText('No company named "NonExistentCompany" found.')).toBeInTheDocument();
    });
  });
});
