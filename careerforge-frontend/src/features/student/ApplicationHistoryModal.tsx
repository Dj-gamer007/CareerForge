import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Modal } from '@/components/ui/Modal';
import { applicationService } from '@/services/application.service';
import { ApplicationStatusHistoryResponse, ApplicationStatus } from '@/types/application.types';
import { formatDateTime } from '@/lib/utils';
import {
  CheckCircle2,
  Clock,
  Star,
  Calendar,
  XCircle,
  Ban,
  FileText,
  AlertCircle,
  Loader2,
  User,
  Building2,
} from 'lucide-react';

interface ApplicationHistoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  applicationId: number | null;
  jobTitle?: string;
  companyName?: string;
}

const statusConfig: Record<
  ApplicationStatus,
  { label: string; bg: string; text: string; border: string; icon: React.ElementType }
> = {
  APPLIED: {
    label: 'Applied',
    bg: 'bg-blue-50',
    text: 'text-blue-700',
    border: 'border-blue-200',
    icon: FileText,
  },
  UNDER_REVIEW: {
    label: 'Under Review',
    bg: 'bg-amber-50',
    text: 'text-amber-700',
    border: 'border-amber-200',
    icon: Clock,
  },
  SHORTLISTED: {
    label: 'Shortlisted',
    bg: 'bg-purple-50',
    text: 'text-purple-700',
    border: 'border-purple-200',
    icon: Star,
  },
  INTERVIEW_SCHEDULED: {
    label: 'Interview Scheduled',
    bg: 'bg-indigo-50',
    text: 'text-indigo-700',
    border: 'border-indigo-200',
    icon: Calendar,
  },
  ACCEPTED: {
    label: 'Accepted / Offer Extended',
    bg: 'bg-emerald-50',
    text: 'text-emerald-700',
    border: 'border-emerald-200',
    icon: CheckCircle2,
  },
  REJECTED: {
    label: 'Not Selected',
    bg: 'bg-rose-50',
    text: 'text-rose-700',
    border: 'border-rose-200',
    icon: XCircle,
  },
  WITHDRAWN: {
    label: 'Withdrawn',
    bg: 'bg-slate-100',
    text: 'text-slate-700',
    border: 'border-slate-300',
    icon: Ban,
  },
};

export const ApplicationHistoryModal: React.FC<ApplicationHistoryModalProps> = ({
  isOpen,
  onClose,
  applicationId,
  jobTitle,
  companyName,
}) => {
  const {
    data: history,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ['application-history', applicationId],
    queryFn: () => (applicationId ? applicationService.getStudentApplicationHistory(applicationId) : Promise.resolve([])),
    enabled: isOpen && !!applicationId,
    refetchInterval: isOpen && !!applicationId ? 2000 : false,
  });

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Application Timeline & History"
      description={jobTitle ? `${jobTitle}${companyName ? ` • ${companyName}` : ''}` : undefined}
      maxWidth="lg"
    >
      <div className="p-6">
        {isLoading && (
          <div className="flex flex-col items-center justify-center py-12 space-y-3">
            <Loader2 className="w-8 h-8 animate-spin text-primary-600" />
            <p className="text-sm text-slate-500 font-medium">Loading status timeline...</p>
          </div>
        )}

        {isError && (
          <div className="flex flex-col items-center justify-center py-8 text-center space-y-3">
            <div className="w-12 h-12 rounded-full bg-rose-100 flex items-center justify-center text-rose-600">
              <AlertCircle className="w-6 h-6" />
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900">Failed to load history</p>
              <p className="text-xs text-slate-500 mt-1">An error occurred while fetching the timeline records.</p>
            </div>
            <button
              onClick={() => refetch()}
              className="px-3 py-1.5 text-xs font-semibold text-primary-600 hover:text-primary-700 hover:bg-primary-50 rounded-lg transition-colors"
            >
              Try Again
            </button>
          </div>
        )}

        {!isLoading && !isError && (!history || history.length === 0) && (
          <div className="text-center py-10">
            <Clock className="w-10 h-10 text-slate-300 mx-auto mb-2" />
            <p className="text-sm text-slate-600 font-medium">No history events recorded yet.</p>
          </div>
        )}

        {!isLoading && !isError && history && history.length > 0 && (
          <div className="relative pl-6 space-y-8 before:absolute before:left-3.5 before:top-3 before:bottom-3 before:w-0.5 before:bg-slate-200">
            {history.map((event: ApplicationStatusHistoryResponse, index: number) => {
              const cfg = statusConfig[event.toStatus] || {
                label: event.toStatus,
                bg: 'bg-slate-50',
                text: 'text-slate-700',
                border: 'border-slate-200',
                icon: Clock,
              };
              const Icon = cfg.icon;
              const isLast = index === history.length - 1;

              return (
                <div key={event.id || index} className="relative flex items-start group">
                  {/* Status Indicator Icon */}
                  <div
                    className={`absolute -left-6 flex items-center justify-center w-7 h-7 rounded-full border-2 bg-white ${
                      isLast ? 'border-primary-600 ring-4 ring-primary-50 text-primary-600' : `${cfg.border} ${cfg.text}`
                    }`}
                  >
                    <Icon className="w-3.5 h-3.5" />
                  </div>

                  {/* Event Details Card */}
                  <div className="ml-4 flex-1 bg-slate-50/60 rounded-xl p-4 border border-slate-100 transition-all hover:border-slate-200 hover:bg-slate-50">
                    <div className="flex flex-wrap items-center justify-between gap-2 mb-1.5">
                      <div className="flex items-center space-x-2">
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${cfg.bg} ${cfg.text} border ${cfg.border}`}>
                          {cfg.label}
                        </span>
                        {isLast && (
                          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-primary-100 text-primary-700">
                            Current Status
                          </span>
                        )}
                      </div>
                      <time className="text-xs text-slate-500 font-medium">
                        {formatDateTime(event.changedAt)}
                      </time>
                    </div>

                    {/* Transition summary */}
                    {event.fromStatus && (
                      <p className="text-xs text-slate-500 mt-1">
                        Transitioned from <span className="font-semibold text-slate-700">{statusConfig[event.fromStatus]?.label || event.fromStatus}</span>
                      </p>
                    )}

                    {/* Actor label */}
                    <div className="flex items-center space-x-1.5 text-[11px] text-slate-400 mt-2">
                      {event.changedBy === 'STUDENT' ? (
                        <>
                          <User className="w-3 h-3 text-slate-400" />
                          <span>Initiated by Candidate</span>
                        </>
                      ) : (
                        <>
                          <Building2 className="w-3 h-3 text-slate-400" />
                          <span>Updated by Hiring Team</span>
                        </>
                      )}
                    </div>

                    {/* Notes / Remarks if provided */}
                    {event.notes && (
                      <div className="mt-2.5 pt-2.5 border-t border-slate-200/60 text-xs text-slate-600 bg-white/70 rounded-lg p-2">
                        <span className="font-medium text-slate-700">Note: </span>
                        {event.notes}
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </Modal>
  );
};
