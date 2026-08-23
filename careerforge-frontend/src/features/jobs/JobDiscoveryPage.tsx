import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { jobService, SearchJobParams } from '@/services/job.service';
import { applicationService } from '@/services/application.service';
import { queryKeys } from '@/lib/queryClient';
import { useAuthStore } from '@/features/auth/authStore';
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
import {
  AlertCircle,
  Search,
  MapPin,
  Building2,
  Calendar,
  Briefcase,
  DollarSign,
  Bookmark,
  BookmarkCheck,
  X,
} from 'lucide-react';
import { WorkMode, JobType, ExperienceLevel, JobSummaryResponse } from '@/types/job.types';

const availableSkills = [
  { id: 1, name: 'Java' },
  { id: 2, name: 'Spring Boot' },
  { id: 3, name: 'MySQL' },
  { id: 4, name: 'Git' },
  { id: 5, name: 'Docker' },
  { id: 6, name: 'REST API' },
  { id: 7, name: 'React' },
  { id: 8, name: 'TypeScript' },
  { id: 9, name: 'Python' },
  { id: 10, name: 'Microservices' },
];

export function JobDiscoveryPage() {
  const queryClient = useQueryClient();
  const { user, isAuthenticated } = useAuthStore();
  const isStudent = isAuthenticated && user?.role === 'ROLE_STUDENT';

  const [activeTab, setActiveTab] = useState<'search' | 'current'>('search');
  const [hasSearched, setHasSearched] = useState(false);
  const [validationMessage, setValidationMessage] = useState<string | null>(null);

  // Search Jobs Params
  const [searchParams, setSearchParams] = useState<SearchJobParams>({
    page: 0,
    size: 10,
    keyword: '',
    location: '',
    sort: 'createdAt,desc',
  });

  const [keywordInput, setKeywordInput] = useState('');
  const [locationInput, setLocationInput] = useState('');

  // Current Openings Page
  const [currentOpeningsPage, setCurrentOpeningsPage] = useState(0);

  // Query for Search Jobs Tab
  const {
    data: searchData,
    isLoading: isSearchLoading,
    isError: isSearchError,
    error: searchError,
    refetch: refetchSearch,
  } = useQuery({
    queryKey: queryKeys.publicJobs.list(searchParams as Record<string, unknown>),
    queryFn: () => jobService.searchJobs(searchParams),
    enabled: activeTab === 'search' && hasSearched,
  });

  // Query for Current Openings Tab (Loads all active jobs)
  const {
    data: currentOpeningsData,
    isLoading: isCurrentLoading,
    isError: isCurrentError,
    error: currentError,
    refetch: refetchCurrent,
  } = useQuery({
    queryKey: queryKeys.publicJobs.list({ page: currentOpeningsPage, size: 10, tab: 'current' }),
    queryFn: () => jobService.searchJobs({ page: currentOpeningsPage, size: 10 }),
    enabled: activeTab === 'current',
  });

  // Saved Jobs for Candidate Bookmarking
  const { data: savedJobsData } = useQuery({
    queryKey: ['student', 'saved-jobs'],
    queryFn: () => applicationService.getSavedJobs({ size: 100 }),
    enabled: isStudent,
  });

  const savedJobIds = new Set(savedJobsData?.content?.map((s) => s.jobId) || []);

  const toggleBookmarkMutation = useMutation({
    mutationFn: async (jobId: number) => {
      if (savedJobIds.has(jobId)) {
        await applicationService.removeSavedJob(jobId);
      } else {
        await applicationService.saveJob(jobId);
      }
    },
    onSuccess: (_, jobId) => {
      queryClient.invalidateQueries({ queryKey: ['student', 'saved-jobs'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.student.isSaved(jobId) });
    },
  });

  const isSortModified = Boolean(searchParams.sort && searchParams.sort !== 'createdAt,desc');

  const hasActiveFilters =
    Boolean(keywordInput.trim()) ||
    Boolean(locationInput.trim()) ||
    Boolean(searchParams.workMode) ||
    Boolean(searchParams.jobType) ||
    Boolean(searchParams.experienceLevel) ||
    Boolean(searchParams.minSalary) ||
    Boolean(searchParams.maxSalary) ||
    Boolean(searchParams.companyId) ||
    Boolean(searchParams.skillIds?.length) ||
    isSortModified;

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const trimmedKeyword = keywordInput.trim();
    const trimmedLocation = locationInput.trim();

    const hasCriteria =
      Boolean(trimmedKeyword) ||
      Boolean(trimmedLocation) ||
      Boolean(searchParams.workMode) ||
      Boolean(searchParams.jobType) ||
      Boolean(searchParams.experienceLevel) ||
      Boolean(searchParams.minSalary) ||
      Boolean(searchParams.maxSalary) ||
      Boolean(searchParams.companyId) ||
      Boolean(searchParams.skillIds?.length) ||
      isSortModified;

    if (!hasCriteria) {
      setValidationMessage('Please select at least one search criterion or filter.');
      setHasSearched(false);
      return;
    }

    setValidationMessage(null);
    setHasSearched(true);
    setSearchParams((prev) => ({
      ...prev,
      page: 0,
      keyword: trimmedKeyword || undefined,
      location: trimmedLocation || undefined,
    }));
  };

  const handleFilterChange = (key: keyof SearchJobParams, value: any) => {
    setValidationMessage(null);
    setHasSearched(true);
    setSearchParams((prev) => ({
      ...prev,
      page: 0,
      keyword: keywordInput.trim() || undefined,
      location: locationInput.trim() || undefined,
      [key]: value || undefined,
    }));
  };

  const toggleSkill = (skillId: number) => {
    setValidationMessage(null);
    setHasSearched(true);
    setSearchParams((prev) => {
      const currentSkillIds = prev.skillIds || [];
      const updatedSkillIds = currentSkillIds.includes(skillId)
        ? currentSkillIds.filter((id) => id !== skillId)
        : [...currentSkillIds, skillId];

      return {
        ...prev,
        page: 0,
        keyword: keywordInput.trim() || undefined,
        location: locationInput.trim() || undefined,
        skillIds: updatedSkillIds.length > 0 ? updatedSkillIds : undefined,
      };
    });
  };

  const resetFilters = () => {
    setKeywordInput('');
    setLocationInput('');
    setHasSearched(false);
    setValidationMessage(null);
    setSearchParams({
      page: 0,
      size: 10,
      keyword: '',
      location: '',
      workMode: undefined,
      jobType: undefined,
      experienceLevel: undefined,
      minSalary: undefined,
      maxSalary: undefined,
      companyId: undefined,
      skillIds: undefined,
      sort: 'createdAt,desc',
    });
  };

  const renderJobCard = (job: JobSummaryResponse) => (
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

            {/* Required Skills */}
            {job.skills && job.skills.length > 0 && (
              <div className="flex items-center gap-1.5 flex-wrap pt-2">
                {job.skills.slice(0, 5).map((s) => (
                  <span
                    key={s.id}
                    className={`text-[11px] px-2 py-0.5 rounded-md font-medium ${
                      s.isRequired
                        ? 'bg-indigo-50 text-indigo-700 border border-indigo-100'
                        : 'bg-slate-100 text-slate-600'
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

          <div className="shrink-0 flex items-center gap-2">
            {isStudent && (
              <Button
                variant="ghost"
                size="sm"
                className={
                  savedJobIds.has(job.id)
                    ? 'text-indigo-600 hover:bg-indigo-50'
                    : 'text-slate-400 hover:text-slate-600'
                }
                onClick={() => toggleBookmarkMutation.mutate(job.id)}
                isLoading={toggleBookmarkMutation.isPending && (toggleBookmarkMutation.variables as any) === job.id}
                title={savedJobIds.has(job.id) ? 'Remove from bookmarks' : 'Bookmark job'}
              >
                {savedJobIds.has(job.id) ? (
                  <BookmarkCheck className="w-4 h-4 text-indigo-600" />
                ) : (
                  <Bookmark className="w-4 h-4" />
                )}
              </Button>
            )}
            <Link to={`/jobs/${job.slug || job.id}`}>
              <Button size="sm">View Details</Button>
            </Link>
          </div>
        </div>
      </CardContent>
    </Card>
  );

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Explore Career Opportunities</h1>
        <p className="text-sm text-slate-500 mt-1">Discover verified job openings and apply with confidence</p>

        {/* Top-Level Tabs: Search Jobs | Current Openings */}
        <div className="mt-6 flex border-b border-slate-200 gap-8">
          <button
            type="button"
            onClick={() => setActiveTab('search')}
            className={`pb-3 text-sm font-semibold border-b-2 transition-colors flex items-center gap-2 ${
              activeTab === 'search'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
            }`}
          >
            <Search className="w-4 h-4" />
            Search Jobs
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('current')}
            className={`pb-3 text-sm font-semibold border-b-2 transition-colors flex items-center gap-2 ${
              activeTab === 'current'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
            }`}
          >
            <Briefcase className="w-4 h-4" />
            Current Openings
          </button>
        </div>
      </div>

      {/* =========================================================
          TAB 1: SEARCH JOBS
      ========================================================== */}
      {activeTab === 'search' && (
        <div className="space-y-6">
          <div className="bg-slate-50 p-6 rounded-2xl border border-slate-200 space-y-4">
            {validationMessage && (
              <div
                role="alert"
                className="p-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-xl text-xs flex items-center gap-2"
              >
                <AlertCircle className="w-4 h-4 text-amber-600 shrink-0" />
                <span>{validationMessage}</span>
              </div>
            )}

            <form onSubmit={handleSearchSubmit} className="flex flex-col md:flex-row gap-3">
              <div className="flex-1 relative">
                <Search className="w-5 h-5 absolute left-3 top-2.5 text-slate-400" />
                <input
                  type="text"
                  placeholder="Job title, keywords, or skills..."
                  value={keywordInput}
                  onChange={(e) => {
                    setKeywordInput(e.target.value);
                    if (validationMessage) setValidationMessage(null);
                  }}
                  className="w-full h-10 pl-10 pr-3 rounded-lg border border-slate-300 bg-white text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div className="w-full md:w-64 relative">
                <MapPin className="w-5 h-5 absolute left-3 top-2.5 text-slate-400" />
                <input
                  type="text"
                  placeholder="City, state, or remote..."
                  value={locationInput}
                  onChange={(e) => {
                    setLocationInput(e.target.value);
                    if (validationMessage) setValidationMessage(null);
                  }}
                  className="w-full h-10 pl-10 pr-3 rounded-lg border border-slate-300 bg-white text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <Button type="submit" size="md">
                Search Jobs
              </Button>
            </form>

            {/* Facet Filters */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <Select
                options={[
                  { label: 'All Work Modes', value: '' },
                  { label: 'Onsite', value: 'ONSITE' },
                  { label: 'Remote', value: 'REMOTE' },
                  { label: 'Hybrid', value: 'HYBRID' },
                ]}
                value={searchParams.workMode || ''}
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
                value={searchParams.jobType || ''}
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
                value={searchParams.experienceLevel || ''}
                onChange={(e) => handleFilterChange('experienceLevel', e.target.value as ExperienceLevel)}
              />

              <Select
                options={[
                  { label: 'Sort by Newest', value: 'createdAt,desc' },
                  { label: 'Sort by Deadline', value: 'deadline,asc' },
                ]}
                value={searchParams.sort || 'createdAt,desc'}
                onChange={(e) => handleFilterChange('sort', e.target.value)}
              />
            </div>

            {/* Skills Filter */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="text-xs font-semibold uppercase tracking-wider text-slate-500">Filter by Skills</label>
                {searchParams.skillIds && searchParams.skillIds.length > 0 && (
                  <button
                    type="button"
                    onClick={() =>
                      setSearchParams((prev) => ({
                        ...prev,
                        page: 0,
                        skillIds: undefined,
                      }))
                    }
                    className="text-xs text-indigo-600 hover:text-indigo-800 font-medium"
                  >
                    Clear skills
                  </button>
                )}
              </div>

              <div className="flex flex-wrap gap-2">
                {availableSkills.map((skill) => {
                  const selected = searchParams.skillIds?.includes(skill.id);
                  return (
                    <button
                      key={skill.id}
                      type="button"
                      onClick={() => toggleSkill(skill.id)}
                      className={`px-3 py-1.5 rounded-full text-xs font-medium border transition-colors ${
                        selected
                          ? 'bg-indigo-600 text-white border-indigo-600'
                          : 'bg-white text-slate-600 border-slate-300 hover:border-indigo-400 hover:text-indigo-600'
                      }`}
                    >
                      {skill.name}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Active Filters + Reset */}
            {hasActiveFilters && (
              <div className="flex items-center justify-between gap-3 pt-2 border-t border-slate-200">
                <div className="flex flex-wrap gap-2 items-center">
                  <span className="text-xs text-slate-400">Active filters:</span>
                  {searchParams.skillIds?.map((skillId) => {
                    const skill = availableSkills.find((s) => s.id === skillId);
                    return (
                      <Badge key={skillId} variant="info">
                        {skill?.name || skillId}
                        <button type="button" onClick={() => toggleSkill(skillId)} className="ml-1">
                          <X className="w-3 h-3" />
                        </button>
                      </Badge>
                    );
                  })}
                </div>

                <Button type="button" variant="outline" size="sm" onClick={resetFilters}>
                  Reset Filters
                </Button>
              </div>
            )}
          </div>

          {/* Search Results Area */}
          {!hasSearched ? (
            <EmptyState
              icon={<Search className="w-8 h-8 text-indigo-500" />}
              title="Search for your next opportunity"
              description="Use the search bar and filters above to discover matching career opportunities across verified employers."
            />
          ) : isSearchLoading ? (
            <LoadingSpinner text="Searching opportunities..." />
          ) : isSearchError ? (
            <ErrorState
              title="Could not complete search"
              message={(searchError as any)?.response?.data?.message || 'Failed to fetch search results'}
              onRetry={() => refetchSearch()}
            />
          ) : !searchData || searchData.content.length === 0 ? (
            <EmptyState
              icon={<Briefcase className="w-8 h-8 text-slate-400" />}
              title="No jobs found"
              description="No job openings matched your search criteria. Try adjusting your keywords or filters."
              actionText="Reset Search"
              onAction={resetFilters}
            />
          ) : (
            <div className="space-y-4">
              <div className="text-xs text-slate-500 font-medium">
                Found {searchData.totalElements} job {searchData.totalElements === 1 ? 'opening' : 'openings'}
              </div>

              <div className="grid grid-cols-1 gap-4">{searchData.content.map(renderJobCard)}</div>

              <PaginationControls
                currentPage={searchData.number}
                totalPages={searchData.totalPages}
                totalElements={searchData.totalElements}
                pageSize={searchData.size}
                onPageChange={(newPage) => setSearchParams((prev) => ({ ...prev, page: newPage }))}
              />
            </div>
          )}
        </div>
      )}

      {/* =========================================================
          TAB 2: CURRENT OPENINGS
      ========================================================== */}
      {activeTab === 'current' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-slate-100">
            <div>
              <h2 className="text-lg font-bold text-slate-900">Current Job Openings</h2>
              <p className="text-xs text-slate-500">All currently active and verified positions open for applications</p>
            </div>
            {currentOpeningsData && (
              <span className="text-xs text-slate-500 font-medium">
                Found {currentOpeningsData.totalElements} active {currentOpeningsData.totalElements === 1 ? 'opening' : 'openings'}
              </span>
            )}
          </div>

          {isCurrentLoading ? (
            <LoadingSpinner text="Loading current job openings..." />
          ) : isCurrentError ? (
            <ErrorState
              title="Could not load openings"
              message={(currentError as any)?.response?.data?.message || 'Failed to fetch current openings'}
              onRetry={() => refetchCurrent()}
            />
          ) : !currentOpeningsData || currentOpeningsData.content.length === 0 ? (
            <EmptyState
              icon={<Briefcase className="w-8 h-8 text-slate-400" />}
              title="No active job openings"
              description="There are currently no active job openings available. Please check back soon."
            />
          ) : (
            <div className="space-y-4">
              <div className="grid grid-cols-1 gap-4">{currentOpeningsData.content.map(renderJobCard)}</div>

              <PaginationControls
                currentPage={currentOpeningsData.number}
                totalPages={currentOpeningsData.totalPages}
                totalElements={currentOpeningsData.totalElements}
                pageSize={currentOpeningsData.size}
                onPageChange={(newPage) => setCurrentOpeningsPage(newPage)}
              />
            </div>
          )}
        </div>
      )}
    </div>
  );
}