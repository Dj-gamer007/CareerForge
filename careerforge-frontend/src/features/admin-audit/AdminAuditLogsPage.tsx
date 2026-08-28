import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { adminService } from '@/services/admin.service';
import { queryKeys } from '@/lib/queryClient';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Modal } from '@/components/ui/Modal';
import { PaginationControls } from '@/components/table/PaginationControls';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { formatDateTime } from '@/lib/utils';
import { Activity, Eye } from 'lucide-react';
import { AuditEventType, AuditStatus, AuditLogSummaryResponse } from '@/types/audit.types';

export function AdminAuditLogsPage() {
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [eventTypeFilter, setEventTypeFilter] = useState<AuditEventType | ''>('');
  const [statusFilter, setStatusFilter] = useState<AuditStatus | ''>('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');

  const [selectedLog, setSelectedLog] = useState<AuditLogSummaryResponse | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);

  const { data: auditData, isLoading, isError, error, refetch } = useQuery({
    queryKey: queryKeys.admin.auditLogs({
      page,
      size: 15,
      search: searchInput || undefined,
      eventType: eventTypeFilter || undefined,
      status: statusFilter || undefined,
      dateFrom: dateFrom ? `${dateFrom}T00:00:00` : undefined,
      dateTo: dateTo ? `${dateTo}T23:59:59` : undefined,
    }),
    queryFn: () =>
      adminService.getAuditLogs({
        page,
        size: 15,
        search: searchInput || undefined,
        eventType: eventTypeFilter ? (eventTypeFilter as AuditEventType) : undefined,
        status: statusFilter ? (statusFilter as AuditStatus) : undefined,
        dateFrom: dateFrom ? `${dateFrom}T00:00:00` : undefined,
        dateTo: dateTo ? `${dateTo}T23:59:59` : undefined,
      }),
    placeholderData: (previousData) => previousData,
    refetchInterval: 2000,
    refetchIntervalInBackground: false,
  });

  const { data: logDetail, isLoading: isDetailLoading } = useQuery({
    queryKey: queryKeys.admin.auditLogDetail(selectedLog?.id || 0),
    queryFn: () => adminService.getAuditLogById(selectedLog!.id),
    enabled: isDetailModalOpen && !!selectedLog?.id,
    refetchInterval: isDetailModalOpen && !!selectedLog?.id ? 2500 : false,
    refetchIntervalInBackground: false,
  });

  const openDetailModal = (log: AuditLogSummaryResponse) => {
    setSelectedLog(log);
    setIsDetailModalOpen(true);
  };

  if (isLoading && !auditData) return <LoadingSpinner text="Loading immutable security audit trail..." />;
  if (isError) {
    return (
      <ErrorState
        error={error}
        onRetry={() => refetch()}
      />
    );
  }

  const logs = auditData?.content || [];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Security & Governance Audit Trail"
        description="Append-only immutable record of all administrative mutations, authentication events, and moderation decisions"
      />

      {/* Filter Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
        <Input
          placeholder="Search by actor email, reason..."
          value={searchInput}
          onChange={(e) => {
            setSearchInput(e.target.value);
            setPage(0);
          }}
        />

        <Select
          options={[
            { label: 'All Event Types', value: '' },
            { label: 'USER_STATUS_UPDATED', value: 'USER_STATUS_UPDATED' },
            { label: 'USER_SELF_DISABLE_REJECTED', value: 'USER_SELF_DISABLE_REJECTED' },
            { label: 'COMPANY_VERIFICATION_UPDATED', value: 'COMPANY_VERIFICATION_UPDATED' },
            { label: 'JOB_MODERATION_PERFORMED', value: 'JOB_MODERATION_PERFORMED' },
            { label: 'JOB_PUBLISH_GUARD_BLOCKED', value: 'JOB_PUBLISH_GUARD_BLOCKED' },
            { label: 'ADMIN_LOGIN_SUCCESS', value: 'ADMIN_LOGIN_SUCCESS' },
            { label: 'ADMIN_LOGIN_FAILURE', value: 'ADMIN_LOGIN_FAILURE' },
          ]}
          value={eventTypeFilter}
          onChange={(e) => {
            setEventTypeFilter(e.target.value as AuditEventType | '');
            setPage(0);
          }}
        />

        <Select
          options={[
            { label: 'All Outcomes', value: '' },
            { label: 'SUCCESS', value: 'SUCCESS' },
            { label: 'FAILURE', value: 'FAILURE' },
          ]}
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value as AuditStatus | '');
            setPage(0);
          }}
        />

        <Input
          type="date"
          placeholder="From Date"
          value={dateFrom}
          onChange={(e) => {
            setDateFrom(e.target.value);
            setPage(0);
          }}
        />

        <Input
          type="date"
          placeholder="To Date"
          value={dateTo}
          onChange={(e) => {
            setDateTo(e.target.value);
            setPage(0);
          }}
        />
      </div>

      <Card>
        <CardContent className="p-0 overflow-x-auto">
          {logs.length === 0 ? (
            <EmptyState
              icon={<Activity className="w-8 h-8 text-slate-400" />}
              title="No audit records"
              description="No audit trail events match the selected criteria."
            />
          ) : (
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3">Timestamp</th>
                  <th className="px-6 py-3">Event Type</th>
                  <th className="px-6 py-3">Outcome</th>
                  <th className="px-6 py-3">Actor</th>
                  <th className="px-6 py-3">Target</th>
                  <th className="px-6 py-3 text-right">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono text-xs">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="px-6 py-4 whitespace-nowrap text-slate-500">
                      {formatDateTime(log.createdAt)}
                    </td>
                    <td className="px-6 py-4 font-semibold text-slate-900">
                      {log.eventType}
                    </td>
                    <td className="px-6 py-4">
                      <Badge variant={log.status === 'SUCCESS' ? 'success' : 'destructive'}>
                        {log.status}
                      </Badge>
                    </td>
                    <td className="px-6 py-4">
                      <span className="font-semibold text-slate-900">{log.actorEmail}</span>
                      <span className="text-[10px] text-slate-400 block font-sans">
                        Role: {log.actorRole}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className="text-slate-900">
                        {log.targetEntityType} #{log.targetEntityId || 'N/A'}
                      </span>
                      {log.targetIdentifier && (
                        <span className="text-[10px] text-slate-400 block truncate max-w-[140px]">
                          {log.targetIdentifier}
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <Button size="sm" variant="ghost" onClick={() => openDetailModal(log)}>
                        <Eye className="w-4 h-4 mr-1" />
                        Inspect
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
        {auditData && (
          <PaginationControls
            currentPage={auditData.page ?? auditData.number ?? 0}
            totalPages={auditData.totalPages}
            totalElements={auditData.totalElements}
            pageSize={auditData.size}
            onPageChange={(newPage) => setPage(Number.isFinite(newPage) ? newPage : 0)}
          />
        )}
      </Card>

      {/* JSON Inspector Modal */}
      <Modal
        isOpen={isDetailModalOpen}
        onClose={() => setIsDetailModalOpen(false)}
        title="Audit Event Details & Payload Inspector"
        description={`Event ID: #${selectedLog?.id} &bull; Immutable forensic audit snapshot`}
        maxWidth="xl"
      >
        {isDetailLoading || !logDetail ? (
          <LoadingSpinner text="Fetching audit event details..." />
        ) : (
          <div className="space-y-4 text-xs font-mono">
            <div className="grid grid-cols-2 gap-4 p-4 rounded-xl bg-slate-50 border border-slate-200 font-sans text-xs">
              <div>
                <span className="text-slate-400 font-bold uppercase">Event Type</span>
                <p className="font-bold text-slate-900 mt-0.5">{logDetail.eventType}</p>
              </div>
              <div>
                <span className="text-slate-400 font-bold uppercase">Outcome Status</span>
                <p className="mt-0.5">
                  <Badge variant={logDetail.status === 'SUCCESS' ? 'success' : 'destructive'}>
                    {logDetail.status}
                  </Badge>
                </p>
              </div>
              <div>
                <span className="text-slate-400 font-bold uppercase">Actor</span>
                <p className="font-bold text-slate-900 mt-0.5">{logDetail.actorEmail} ({logDetail.actorRole})</p>
              </div>
              <div>
                <span className="text-slate-400 font-bold uppercase">Timestamp</span>
                <p className="font-bold text-slate-900 mt-0.5">{formatDateTime(logDetail.createdAt)}</p>
              </div>
            </div>

            {logDetail.reason && (
              <div className="p-3 bg-amber-50/60 border border-amber-200 rounded-lg font-sans">
                <span className="font-bold text-amber-900 uppercase text-[10px]">Justification Reason:</span>
                <p className="text-amber-800 text-xs mt-0.5">{logDetail.reason}</p>
              </div>
            )}

            {/* Sanitized JSON Details */}
            <div>
              <span className="font-bold font-sans text-slate-500 uppercase text-[10px] block mb-1">
                Sanitized State Details (JSON Payload)
              </span>
              <pre className="p-4 rounded-xl bg-slate-900 text-emerald-400 overflow-x-auto text-[11px] leading-relaxed">
                {logDetail.details
                  ? JSON.stringify(JSON.parse(logDetail.details), null, 2)
                  : '// No additional state payload recorded'}
              </pre>
            </div>

            {/* Client Context */}
            <div className="p-3 bg-slate-50 rounded-lg text-slate-500 text-[10px] space-y-1 font-sans border border-slate-200">
              <p><span className="font-bold">Client IP Address:</span> {logDetail.ipAddress || '127.0.0.1'}</p>
              <p className="truncate"><span className="font-bold">User-Agent:</span> {logDetail.userAgent || 'N/A'}</p>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
