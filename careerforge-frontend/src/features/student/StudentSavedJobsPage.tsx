import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { applicationService } from '@/services/application.service';
import { queryKeys } from '@/lib/queryClient';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { PaginationControls } from '@/components/table/PaginationControls';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { Link } from 'react-router-dom';
import { Bookmark, Building2, Trash2, ArrowRight } from 'lucide-react';

export function StudentSavedJobsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: queryKeys.student.savedJobs(page),
    queryFn: () => applicationService.getSavedJobs({ page, size: 10 }),
  });

  const removeMutation = useMutation({
    mutationFn: (jobId: number) => applicationService.removeSavedJob(jobId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.student.savedJobs() }),
  });

  if (isLoading) return <LoadingSpinner text="Loading your bookmarks..." />;
  if (isError) {
    return (
      <ErrorState
        title="Could not load bookmarks"
        message={(error as any)?.response?.data?.message || 'Failed to fetch saved jobs'}
        onRetry={() => refetch()}
      />
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Bookmarked / Saved Jobs"
        description="Quickly access and apply to opportunities you have saved"
      />

      {!data || data.content.length === 0 ? (
        <EmptyState
          icon={<Bookmark className="w-8 h-8 text-slate-400" />}
          title="No bookmarks saved"
          description="You haven't bookmarked any jobs yet. Browse public jobs to save interesting opportunities."
          actionText="Explore Jobs"
          onAction={() => {}}
        />
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-4">
            {data.content.map((saved: any) => (
              <Card key={saved.id}>
                <CardContent className="p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div className="space-y-1">
                    <h3 className="text-base font-bold text-slate-900">{saved.job?.title || 'Saved Job'}</h3>
                    <p className="text-xs text-slate-500 flex items-center gap-1.5 font-medium">
                      <Building2 className="w-3.5 h-3.5" />
                      {saved.job?.company?.name || 'Company'} &bull; {saved.job?.location || 'Remote'}
                    </p>
                  </div>

                  <div className="flex items-center gap-3">
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-rose-600 hover:bg-rose-50"
                      onClick={() => removeMutation.mutate(saved.job?.id || saved.id)}
                    >
                      <Trash2 className="w-4 h-4 mr-1" />
                      Remove
                    </Button>
                    <Link to={`/jobs/${saved.job?.slug || saved.job?.id}`}>
                      <Button size="sm">
                        View & Apply
                        <ArrowRight className="w-3.5 h-3.5 ml-1" />
                      </Button>
                    </Link>
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
