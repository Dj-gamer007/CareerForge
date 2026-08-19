import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { jobService, SearchJobParams } from '@/services/job.service';
import { queryKeys } from '@/lib/queryClient';
import { Link } from 'react-router-dom';
import { Select } from '@/components/ui/Select';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Card, CardContent } from '@/components/ui/Card';
import { PaginationControls } from '@/components/table/PaginationControls';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { formatCurrency, formatDate } from '@/lib/utils';
import { Search, MapPin, Building2, Calendar, Briefcase, DollarSign } from 'lucide-react';
import { WorkMode, JobType, ExperienceLevel } from '@/types/job.types';

export function JobDiscoveryPage() {
  const [params, setParams] = useState<SearchJobParams>({
    page: 0,
    size: 10,
    keyword: '',
    location: '',
  });

  const [keywordInput, setKeywordInput] = useState('');
  const [locationInput, setLocationInput] = useState('');

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: queryKeys.publicJobs.list(params as Record<string, unknown>),
    queryFn: () => jobService.searchJobs(params),
  });

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setParams((prev) => ({
      ...prev,
      page: 0,
      keyword: keywordInput || undefined,
      location: locationInput || undefined,
    }));
  };

  const handleFilterChange = (key: keyof SearchJobParams, value: any) => {
    setParams((prev) => ({
      ...prev,
      page: 0,
      [key]: value || undefined,
    }));
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Search Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Explore Career Opportunities</h1>
        <p className="text-sm text-slate-500 mt-1">Discover verified job openings and apply with confidence</p>

        <form onSubmit={handleSearchSubmit} className="mt-6 flex flex-col md:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="w-5 h-5 absolute left-3 top-2.5 text-slate-400" />
            <input
              type="text"
              placeholder="Job title, keywords, or skills..."
              value={keywordInput}
              onChange={(e) => setKeywordInput(e.target.value)}
              className="w-full h-10 pl-10 pr-3 rounded-lg border border-slate-300 bg-white text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div className="w-full md:w-64 relative">
            <MapPin className="w-5 h-5 absolute left-3 top-2.5 text-slate-400" />
            <input
              type="text"
              placeholder="City, state, or remote..."
              value={locationInput}
              onChange={(e) => setLocationInput(e.target.value)}
              className="w-full h-10 pl-10 pr-3 rounded-lg border border-slate-300 bg-white text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <Button type="submit" size="md">
            Search Jobs
          </Button>
        </form>

        {/* Facet Filters */}
        <div className="mt-4 grid grid-cols-2 sm:grid-cols-4 gap-3">
          <Select
            options={[
              { label: 'All Work Modes', value: '' },
              { label: 'Onsite', value: 'ONSITE' },
              { label: 'Remote', value: 'REMOTE' },
              { label: 'Hybrid', value: 'HYBRID' },
            ]}
            value={params.workMode || ''}
            onChange={(e) => handleFilterChange('workMode', e.target.value as WorkMode)}
          />

          <Select
            options={[
              { label: 'All Job Types', value: '' },
              { label: 'Full Time', value: 'FULL_TIME' },
              { label: 'Part Time', value: 'PART_TIME' },
              { label: 'Contract', value: 'CONTRACT' },
              { label: 'Internship', value: 'INTERNSHIP' },
            ]}
            value={params.jobType || ''}
            onChange={(e) => handleFilterChange('jobType', e.target.value as JobType)}
          />

          <Select
            options={[
              { label: 'All Experience Levels', value: '' },
              { label: 'Entry Level', value: 'ENTRY_LEVEL' },
              { label: 'Mid Level', value: 'MID_LEVEL' },
              { label: 'Senior Level', value: 'SENIOR_LEVEL' },
              { label: 'Executive', value: 'EXECUTIVE' },
            ]}
            value={params.experienceLevel || ''}
            onChange={(e) => handleFilterChange('experienceLevel', e.target.value as ExperienceLevel)}
          />

          <Select
            options={[
              { label: 'Sort by Newest', value: 'createdAt,desc' },
              { label: 'Sort by Deadline', value: 'deadline,asc' },
            ]}
            value={params.sort || 'createdAt,desc'}
            onChange={(e) => handleFilterChange('sort', e.target.value)}
          />
        </div>
      </div>

      {/* Main Results */}
      {isLoading ? (
        <LoadingSpinner text="Searching jobs..." />
      ) : isError ? (
        <ErrorState
          title="Could not load jobs"
          message={(error as any)?.response?.data?.message || 'Failed to fetch job listings'}
          onRetry={() => refetch()}
        />
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          icon={<Briefcase className="w-8 h-8 text-slate-400" />}
          title="No jobs found"
          description="Try adjusting your search keywords or filter criteria."
          actionText="Reset Filters"
          onAction={() => {
            setKeywordInput('');
            setLocationInput('');
            setParams({ page: 0, size: 10 });
          }}
        />
      ) : (
        <div className="space-y-4">
          <div className="text-xs text-slate-500 font-medium">
            Found {data.totalElements} job {data.totalElements === 1 ? 'opening' : 'openings'}
          </div>

          <div className="grid grid-cols-1 gap-4">
            {data.content.map((job) => (
              <Card key={job.id} className="hover:border-indigo-200 transition-all hover:shadow-md">
                <CardContent className="p-6">
                  <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                    <div className="space-y-2">
                      <div className="flex items-center gap-2 flex-wrap">
                        <Link
                          to={`/jobs/${job.slug || job.id}`}
                          className="text-lg font-bold text-slate-900 hover:text-indigo-600 transition-colors"
                        >
                          {job.title}
                        </Link>
                        <Badge variant="info">{job.workMode}</Badge>
                        <Badge variant="outline">{job.jobType.replace('_', ' ')}</Badge>
                      </div>

                      <div className="flex items-center gap-4 text-xs text-slate-500 flex-wrap">
                        <Link
                          to={`/companies/${job.companySlug || job.companyId}`}
                          className="flex items-center gap-1 font-semibold text-slate-700 hover:text-indigo-600"
                        >
                          <Building2 className="w-3.5 h-3.5" />
                          {job.companyName}
                        </Link>
                        {job.location && (
                          <span className="flex items-center gap-1">
                            <MapPin className="w-3.5 h-3.5" />
                            {job.location}
                          </span>
                        )}
                        {(job.salaryMin || job.salaryMax) && (
                          <span className="flex items-center gap-1 font-medium text-slate-700">
                            <DollarSign className="w-3.5 h-3.5" />
                            {formatCurrency(job.salaryMin, job.currency)} - {formatCurrency(job.salaryMax, job.currency)}
                          </span>
                        )}
                        {job.deadline && (
                          <span className="flex items-center gap-1 text-slate-400">
                            <Calendar className="w-3.5 h-3.5" />
                            Deadline: {formatDate(job.deadline)}
                          </span>
                        )}
                      </div>

                      {/* Required Skills Badges */}
                      {job.skills && job.skills.length > 0 && (
                        <div className="flex items-center gap-1.5 flex-wrap pt-2">
                          {job.skills.slice(0, 5).map((s) => (
                            <span
                              key={s.id}
                              className={`text-[11px] px-2 py-0.5 rounded-md font-medium ${
                                s.isRequired ? 'bg-indigo-50 text-indigo-700 border border-indigo-100' : 'bg-slate-100 text-slate-600'
                              }`}
                            >
                              {s.skillName} {s.isRequired ? '*' : ''}
                            </span>
                          ))}
                          {job.skills.length > 5 && (
                            <span className="text-[10px] text-slate-400 font-medium">
                              +{job.skills.length - 5} more
                            </span>
                          )}
                        </div>
                      )}
                    </div>

                    <div className="shrink-0">
                      <Link to={`/jobs/${job.slug || job.id}`}>
                        <Button size="sm">View Details</Button>
                      </Link>
                    </div>
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
            onPageChange={(newPage) => setParams((prev) => ({ ...prev, page: newPage }))}
          />
        </div>
      )}
    </div>
  );
}
