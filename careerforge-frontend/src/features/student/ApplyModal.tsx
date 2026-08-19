import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { studentService } from '@/services/student.service';
import { applicationService } from '@/services/application.service';
import { queryKeys } from '@/lib/queryClient';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { Textarea } from '@/components/ui/Textarea';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { AlertCircle, FileCheck, CheckCircle2 } from 'lucide-react';
import { Link } from 'react-router-dom';

export interface ApplyModalProps {
  isOpen: boolean;
  onClose: () => void;
  jobId: number;
  jobTitle: string;
  companyName: string;
}

export function ApplyModal({ isOpen, onClose, jobId, jobTitle, companyName }: ApplyModalProps) {
  const [coverLetter, setCoverLetter] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);
  const queryClient = useQueryClient();

  const { data: profile, isLoading: isProfileLoading } = useQuery({
    queryKey: queryKeys.student.profile,
    queryFn: () => studentService.getProfile(),
    enabled: isOpen,
  });

  const activeResume = profile?.resumes?.find((r) => r.isActive);

  const applyMutation = useMutation({
    mutationFn: () =>
      applicationService.submitApplication({
        jobId,
        resumeId: activeResume?.id,
        coverLetter: coverLetter || undefined,
      }),
    onSuccess: () => {
      setIsSuccess(true);
      queryClient.invalidateQueries({ queryKey: queryKeys.student.applications() });
    },
    onError: (err: any) => {
      setErrorMsg(err.response?.data?.message || 'Failed to submit application. Please try again.');
    },
  });

  const handleApply = () => {
    setErrorMsg(null);
    applyMutation.mutate();
  };

  const handleClose = () => {
    setIsSuccess(false);
    setErrorMsg(null);
    setCoverLetter('');
    onClose();
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title={isSuccess ? 'Application Submitted!' : `Apply to ${jobTitle}`}
      description={isSuccess ? `Your application was sent to ${companyName}` : `Submitting to ${companyName}`}
      maxWidth="md"
    >
      {isProfileLoading ? (
        <LoadingSpinner text="Checking profile & active resume..." />
      ) : isSuccess ? (
        <div className="text-center py-6">
          <div className="w-12 h-12 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto mb-4">
            <CheckCircle2 className="w-7 h-7" />
          </div>
          <h4 className="text-lg font-bold text-slate-900">Application Received</h4>
          <p className="text-sm text-slate-500 mt-2 max-w-sm mx-auto">
            Your profile and compatibility snapshot have been registered with the hiring recruiter.
          </p>
          <div className="mt-6 flex justify-center gap-3">
            <Button size="sm" onClick={handleClose}>
              Done
            </Button>
            <Link to="/student/applications">
              <Button variant="outline" size="sm">
                View Applications
              </Button>
            </Link>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          {errorMsg && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg flex items-center gap-2 text-rose-700 text-xs">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          {/* Active Resume Status */}
          <div className="p-3 rounded-lg bg-slate-50 border border-slate-200 text-xs">
            <p className="font-semibold text-slate-700 mb-1">Attached Resume:</p>
            {activeResume ? (
              <div className="flex items-center gap-2 text-emerald-700 font-medium">
                <FileCheck className="w-4 h-4" />
                <span>
                  {activeResume.originalFileName} (v{activeResume.version})
                </span>
              </div>
            ) : (
              <div className="text-amber-700">
                <p>No active resume set. Please upload a resume in your profile before applying.</p>
                <Link to="/student/profile" className="text-indigo-600 font-semibold underline mt-1 inline-block">
                  Go to Profile Manager
                </Link>
              </div>
            )}
          </div>

          {/* Optional Cover Letter */}
          <Textarea
            label="Cover Letter / Note to Recruiter (Optional)"
            placeholder="Introduce yourself and explain why you're a great fit for this position..."
            value={coverLetter}
            onChange={(e) => setCoverLetter(e.target.value)}
            rows={4}
          />

          <div className="flex justify-end gap-3 pt-2">
            <Button variant="outline" size="sm" onClick={handleClose}>
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={handleApply}
              isLoading={applyMutation.isPending}
              disabled={!activeResume}
            >
              Submit Application
            </Button>
          </div>
        </div>
      )}
    </Modal>
  );
}
