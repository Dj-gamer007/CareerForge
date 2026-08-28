import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/features/auth/authStore';
import { recruiterService } from '@/services/recruiter.service';
import { queryKeys } from '@/lib/queryClient';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Textarea } from '@/components/ui/Textarea';
import { Badge, getStatusBadgeVariant } from '@/components/ui/Badge';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { ErrorState } from '@/components/feedback/ErrorState';
import { Building2 } from 'lucide-react';

export function RecruiterCompanyPage() {
  const queryClient = useQueryClient();
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuthStore();
  const userId = user?.id;

  const [isEditing, setIsEditing] = useState(false);
  const [form, setForm] = useState({
    name: '',
    website: '',
    logoUrl: '',
    description: '',
    industry: '',
    companySize: '',
    location: '',
  });

  const {
    data: company,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: queryKeys.recruiter.company(userId),
    queryFn: () => recruiterService.getMyCompany(),
    enabled: isAuthenticated && !isAuthLoading && user?.role === 'ROLE_RECRUITER' && !!userId,
    retry: false,
    refetchInterval: (query) => (query.state.data ? 2000 : false),
    refetchIntervalInBackground: false,
  });

  const saveMutation = useMutation({
    mutationFn: (payload: any) =>
      company?.id ? recruiterService.updateMyCompany(payload) : recruiterService.registerCompany(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.recruiter.company() });
      await queryClient.refetchQueries({ queryKey: queryKeys.recruiter.company() });
      setIsEditing(false);
    },
  });

  if (isAuthLoading || isLoading) return <LoadingSpinner text="Loading company information..." />;

  if (isError) {
    return (
      <ErrorState
        error={error}
        onRetry={() => refetch()}
      />
    );
  }

  const startEdit = () => {
    setForm({
      name: company?.name || '',
      website: company?.website || '',
      logoUrl: company?.logoUrl || '',
      description: company?.description || '',
      industry: company?.industry || '',
      companySize: company?.companySize || '',
      location: company?.location || '',
    });
    setIsEditing(true);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Company Profile & Verification"
        description="Maintain your organizational information and track administrative verification status"
      />

      <Card>
        <CardHeader className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
              <Building2 className="w-5 h-5" />
            </div>
            <div>
              <CardTitle>{company?.name || 'Company Profile'}</CardTitle>
              {company && (
                <div className="mt-1">
                  <Badge variant={getStatusBadgeVariant(company.verificationStatus)}>
                    {company.verificationStatus}
                  </Badge>
                </div>
              )}
            </div>
          </div>
          {!isEditing && (
            <Button size="sm" variant="outline" onClick={startEdit}>
              {company ? 'Edit Profile' : 'Register Company'}
            </Button>
          )}
        </CardHeader>
        <CardContent className="p-6">
          {isEditing ? (
            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input
                  label="Company Name *"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                />
                <Input
                  label="Website URL"
                  value={form.website}
                  onChange={(e) => setForm({ ...form, website: e.target.value })}
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <Input
                  label="Industry"
                  value={form.industry}
                  onChange={(e) => setForm({ ...form, industry: e.target.value })}
                />
                <Input
                  label="Company Size (e.g. 50-200)"
                  value={form.companySize}
                  onChange={(e) => setForm({ ...form, companySize: e.target.value })}
                />
                <Input
                  label="Headquarters Location"
                  value={form.location}
                  onChange={(e) => setForm({ ...form, location: e.target.value })}
                />
              </div>

              <Textarea
                label="Company Description"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                rows={4}
              />

              <div className="flex justify-end gap-3 pt-2">
                <Button variant="outline" size="sm" onClick={() => setIsEditing(false)}>
                  Cancel
                </Button>
                <Button
                  size="sm"
                  onClick={() => saveMutation.mutate(form)}
                  isLoading={saveMutation.isPending}
                >
                  Save Changes
                </Button>
              </div>
            </div>
          ) : company ? (
            <div className="space-y-6">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 p-4 rounded-xl bg-slate-50 border border-slate-100 text-xs">
                <div>
                  <span className="text-slate-400">Industry</span>
                  <p className="font-bold text-slate-900 mt-0.5">{company.industry || 'Not set'}</p>
                </div>
                <div>
                  <span className="text-slate-400">Headquarters</span>
                  <p className="font-bold text-slate-900 mt-0.5">{company.location || 'Not set'}</p>
                </div>
                <div>
                  <span className="text-slate-400">Website</span>
                  <p className="font-bold text-indigo-600 mt-0.5">{company.website || 'Not set'}</p>
                </div>
              </div>

              {company.description && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-1">About the Company</h4>
                  <p className="text-sm text-slate-700 whitespace-pre-line leading-relaxed">{company.description}</p>
                </div>
              )}
            </div>
          ) : (
            <div className="text-center py-8">
              <p className="text-sm text-slate-500 mb-4">You have not registered an employer organization yet.</p>
              <Button size="sm" onClick={startEdit}>
                Register Company Profile
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
