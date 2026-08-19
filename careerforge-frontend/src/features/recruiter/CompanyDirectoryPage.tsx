import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { recruiterService } from '@/services/recruiter.service';
import { queryKeys } from '@/lib/queryClient';
import { Link } from 'react-router-dom';
import { Input } from '@/components/ui/Input';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { PaginationControls } from '@/components/table/PaginationControls';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { Building2, MapPin, Users, CheckCircle2 } from 'lucide-react';

export function CompanyDirectoryPage() {
  const [page, setPage] = useState(0);
  const [searchName, setSearchName] = useState('');

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: queryKeys.publicCompanies.list({ page, name: searchName || undefined }),
    queryFn: () => recruiterService.getCompanies({ page, size: 9, name: searchName || undefined }),
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Verified Hiring Companies</h1>
        <p className="text-sm text-slate-500 mt-1">Discover vetted employers actively hiring on CareerForge</p>

        <div className="mt-6 max-w-md">
          <Input
            placeholder="Search companies by name..."
            value={searchName}
            onChange={(e) => {
              setSearchName(e.target.value);
              setPage(0);
            }}
          />
        </div>
      </div>

      {isLoading ? (
        <LoadingSpinner text="Loading company directory..." />
      ) : isError ? (
        <ErrorState
          title="Could not load companies"
          message={(error as any)?.response?.data?.message || 'Failed to fetch companies'}
          onRetry={() => refetch()}
        />
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          icon={<Building2 className="w-8 h-8 text-slate-400" />}
          title="No companies found"
          description="Try modifying your search filter."
        />
      ) : (
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {data.content.map((company) => (
              <Card key={company.id} className="hover:border-indigo-200 transition-all hover:shadow-md">
                <CardContent className="p-6 space-y-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="w-12 h-12 rounded-xl bg-slate-100 flex items-center justify-center text-indigo-600 font-bold border border-slate-200">
                      <Building2 className="w-6 h-6" />
                    </div>
                    <Badge variant="success" className="flex items-center gap-1">
                      <CheckCircle2 className="w-3 h-3" />
                      Verified
                    </Badge>
                  </div>

                  <div>
                    <Link
                      to={`/companies/${company.slug || company.id}`}
                      className="text-lg font-bold text-slate-900 hover:text-indigo-600 transition-colors"
                    >
                      {company.name}
                    </Link>
                    <p className="text-xs text-slate-500 mt-0.5">{company.industry || 'Technology & Engineering'}</p>
                  </div>

                  <div className="flex items-center gap-4 text-xs text-slate-500 pt-2 border-t border-slate-100">
                    {company.location && (
                      <span className="flex items-center gap-1">
                        <MapPin className="w-3.5 h-3.5" />
                        {company.location}
                      </span>
                    )}
                    {company.companySize && (
                      <span className="flex items-center gap-1">
                        <Users className="w-3.5 h-3.5" />
                        {company.companySize}
                      </span>
                    )}
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>

          <PaginationControls
            currentPage={data.number}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            pageSize={data.size}
            onPageChange={(newPage) => setPage(newPage)}
          />
        </div>
      )}
    </div>
  );
}
