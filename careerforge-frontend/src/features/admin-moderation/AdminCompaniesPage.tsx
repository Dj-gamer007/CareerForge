import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminService } from '@/services/admin.service';
import { queryKeys } from '@/lib/queryClient';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge, getStatusBadgeVariant } from '@/components/ui/Badge';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Textarea } from '@/components/ui/Textarea';
import { Modal } from '@/components/ui/Modal';
import { PaginationControls } from '@/components/table/PaginationControls';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { Building2, CheckCircle, XCircle, Eye } from 'lucide-react';
import { CompanyVerificationStatus } from '@/types/company.types';
import { AdminCompanySummaryResponse } from '@/types/admin.types';

export function AdminCompaniesPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [statusFilter, setStatusFilter] = useState<CompanyVerificationStatus | ''>('');

  const [selectedCompany, setSelectedCompany] = useState<AdminCompanySummaryResponse | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [isVerifyModalOpen, setIsVerifyModalOpen] = useState(false);
  const [targetStatus, setTargetStatus] = useState<CompanyVerificationStatus>('VERIFIED');
  const [reason, setReason] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const { data: companiesData, isLoading, isError, error, refetch } = useQuery({
    queryKey: queryKeys.admin.companies({
      page,
      size: 10,
      search: searchInput || undefined,
      status: statusFilter || undefined,
    }),
    queryFn: () =>
      adminService.getCompanies({
        page,
        size: 10,
        search: searchInput || undefined,
        status: statusFilter ? (statusFilter as CompanyVerificationStatus) : undefined,
      }),
    placeholderData: (previousData) => previousData,
    refetchInterval: 2000,
    refetchIntervalInBackground: false,
  });

  const { data: companyDetail, isLoading: isDetailLoading } = useQuery({
    queryKey: queryKeys.admin.companyDetail(selectedCompany?.id || 0),
    queryFn: () => adminService.getCompanyById(selectedCompany!.id),
    enabled: isDetailModalOpen && !!selectedCompany?.id,
    refetchInterval: isDetailModalOpen && !!selectedCompany?.id ? 2500 : false,
    refetchIntervalInBackground: false,
  });

  const verifyMutation = useMutation({
    mutationFn: ({ id, verificationStatus, reason }: { id: number; verificationStatus: CompanyVerificationStatus; reason: string }) =>
      adminService.verifyCompany(id, { verificationStatus, reason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.companies() });
      if (selectedCompany) {
        queryClient.invalidateQueries({ queryKey: queryKeys.admin.companyDetail(selectedCompany.id) });
      }
      setIsVerifyModalOpen(false);
      setReason('');
      setErrorMsg(null);
    },
    onError: (err: any) => {
      setErrorMsg(err.response?.data?.message || 'Failed to update company verification status');
    },
  });

  const openVerifyModal = (company: AdminCompanySummaryResponse, status: CompanyVerificationStatus) => {
    setSelectedCompany(company);
    setTargetStatus(status);
    setReason('');
    setErrorMsg(null);
    setIsVerifyModalOpen(true);
  };

  const openDetailModal = (company: AdminCompanySummaryResponse) => {
    setSelectedCompany(company);
    setIsDetailModalOpen(true);
  };

  if (isLoading && !companiesData) return <LoadingSpinner text="Loading company verification queue..." />;
  if (isError) {
    return (
      <ErrorState
        error={error}
        onRetry={() => refetch()}
      />
    );
  }

  const companies = companiesData?.content || [];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Company Verification & Moderation"
        description="Verify employer legitimacy, inspect registered recruiters, and approve job publishing permissions"
      />

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Input
          placeholder="Search by company name, location..."
          value={searchInput}
          onChange={(e) => {
            setSearchInput(e.target.value);
            setPage(0);
          }}
        />

        <Select
          options={[
            { label: 'All Verification Statuses', value: '' },
            { label: 'Pending Verification', value: 'PENDING' },
            { label: 'Verified Employers', value: 'VERIFIED' },
            { label: 'Rejected Applications', value: 'REJECTED' },
          ]}
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value as CompanyVerificationStatus | '');
            setPage(0);
          }}
        />
      </div>

      <Card>
        <CardContent className="p-0 overflow-x-auto">
          {companies.length === 0 ? (
            <EmptyState
              icon={<Building2 className="w-8 h-8 text-slate-400" />}
              title="No companies in queue"
              description="No company records match your current filter selection."
            />
          ) : (
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3">Company</th>
                  <th className="px-6 py-3">Industry / Size</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Roster</th>
                  <th className="px-6 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {companies.map((c) => (
                  <tr key={c.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="px-6 py-4">
                      <p className="font-bold text-slate-900">{c.name}</p>
                      <p className="text-xs text-slate-400">{c.location || 'Location unspecified'}</p>
                    </td>
                    <td className="px-6 py-4 text-xs">
                      <p className="text-slate-900 font-medium">{c.industry || 'General'}</p>
                      <p className="text-slate-400">{c.companySize || 'Size N/A'}</p>
                    </td>
                    <td className="px-6 py-4">
                      <Badge variant={getStatusBadgeVariant(c.verificationStatus)}>
                        {c.verificationStatus}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-600">
                      {c.recruiterCount} Recruiters &bull; {c.jobCount} Jobs
                    </td>
                    <td className="px-6 py-4 text-right space-x-2">
                      <Button size="sm" variant="ghost" onClick={() => openDetailModal(c)}>
                        <Eye className="w-4 h-4 mr-1" />
                        Inspect
                      </Button>

                      {c.verificationStatus !== 'VERIFIED' && (
                        <Button
                          size="sm"
                          variant="outline"
                          className="text-emerald-700 border-emerald-200 hover:bg-emerald-50"
                          onClick={() => openVerifyModal(c, 'VERIFIED')}
                        >
                          <CheckCircle className="w-3.5 h-3.5 mr-1" />
                          Verify
                        </Button>
                      )}

                      {c.verificationStatus !== 'REJECTED' && (
                        <Button
                          size="sm"
                          variant="outline"
                          className="text-rose-600 border-rose-200 hover:bg-rose-50"
                          onClick={() => openVerifyModal(c, 'REJECTED')}
                        >
                          <XCircle className="w-3.5 h-3.5 mr-1" />
                          Reject
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
        {companiesData && (
          <PaginationControls
            currentPage={companiesData.page ?? companiesData.number ?? 0}
            totalPages={companiesData.totalPages}
            totalElements={companiesData.totalElements}
            pageSize={companiesData.size}
            onPageChange={(newPage) => setPage(Number.isFinite(newPage) ? newPage : 0)}
          />
        )}
      </Card>

      {/* Company Detail Modal */}
      <Modal
        isOpen={isDetailModalOpen}
        onClose={() => setIsDetailModalOpen(false)}
        title="Company Dossier & Recruiters"
        description="Inspect company registration details and associated recruiter accounts"
        maxWidth="lg"
      >
        {isDetailLoading || !companyDetail ? (
          <LoadingSpinner text="Fetching company details..." />
        ) : (
          <div className="space-y-6">
            <div className="grid grid-cols-2 gap-4 p-4 rounded-xl bg-slate-50 border border-slate-100 text-xs">
              <div>
                <span className="text-slate-400">Name</span>
                <p className="font-bold text-slate-900 mt-0.5">{companyDetail.name}</p>
              </div>
              <div>
                <span className="text-slate-400">Website</span>
                <p className="font-bold text-indigo-600 mt-0.5">{companyDetail.website || 'N/A'}</p>
              </div>
              <div>
                <span className="text-slate-400">Location</span>
                <p className="font-bold text-slate-900 mt-0.5">{companyDetail.location || 'N/A'}</p>
              </div>
              <div>
                <span className="text-slate-400">Status</span>
                <p className="mt-0.5">
                  <Badge variant={getStatusBadgeVariant(companyDetail.verificationStatus)}>
                    {companyDetail.verificationStatus}
                  </Badge>
                </p>
              </div>
            </div>

            {companyDetail.description && (
              <div>
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-1">Description</h4>
                <p className="text-xs text-slate-700 whitespace-pre-line">{companyDetail.description}</p>
              </div>
            )}

            {/* Recruiter Roster */}
            <div>
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
                Registered Recruiters ({companyDetail.recruiters?.length || 0})
              </h4>
              <div className="space-y-2 max-h-48 overflow-y-auto">
                {companyDetail.recruiters?.map((r) => (
                  <div key={r.id} className="p-3 bg-slate-50 rounded-lg text-xs flex justify-between items-center border border-slate-200">
                    <div>
                      <p className="font-bold text-slate-900">{r.firstName} {r.lastName}</p>
                      <p className="text-slate-500">{r.email} &bull; {r.designation}</p>
                    </div>
                    {r.isCompanyAdmin && (
                      <Badge variant="purple">Company Admin</Badge>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </Modal>

      {/* Verification Action Modal */}
      <Modal
        isOpen={isVerifyModalOpen}
        onClose={() => setIsVerifyModalOpen(false)}
        title={`Set Verification to: ${targetStatus}`}
        description={`Modifying verification for: ${selectedCompany?.name}`}
      >
        <div className="space-y-4">
          {errorMsg && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg text-rose-700 text-xs">
              {errorMsg}
            </div>
          )}

          <Textarea
            label="Administrative Justification Reason *"
            placeholder="Document reason for audit logging compliance (min. 5 characters)..."
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={3}
          />

          <div className="flex justify-end gap-3 pt-2">
            <Button variant="outline" size="sm" onClick={() => setIsVerifyModalOpen(false)}>
              Cancel
            </Button>
            <Button
              size="sm"
              variant={targetStatus === 'VERIFIED' ? 'primary' : 'destructive'}
              onClick={() =>
                verifyMutation.mutate({
                  id: selectedCompany!.id,
                  verificationStatus: targetStatus,
                  reason,
                })
              }
              isLoading={verifyMutation.isPending}
              disabled={reason.trim().length < 5}
            >
              Confirm Decision
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
