import { useQuery } from '@tanstack/react-query';
import { jobService } from '@/services/job.service';
import { queryKeys } from '@/lib/queryClient';
import { Modal } from '@/components/ui/Modal';
import { ScoreGauge } from '@/components/ui/ScoreGauge';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { ErrorState } from '@/components/feedback/ErrorState';
import { CheckCircle2, XCircle, Sparkles } from 'lucide-react';

interface MatchPreviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  jobId: number;
}

export function MatchPreviewModal({ isOpen, onClose, jobId }: MatchPreviewModalProps) {
  const { data: match, isLoading, isError, error, refetch } = useQuery({
    queryKey: queryKeys.student.matchPreview(jobId),
    queryFn: () => jobService.getMatchPreview(jobId),
    enabled: isOpen && !!jobId,
  });

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Compatibility & Skill Match Analysis"
      description="Deterministic evaluation comparing candidate profile against job technical requirements"
      maxWidth="lg"
    >
      {isLoading ? (
        <LoadingSpinner text="Computing weighted skill match..." />
      ) : isError ? (
        <ErrorState
          title="Analysis Failed"
          message={(error as any)?.response?.data?.message || 'Unable to calculate match score'}
          onRetry={() => refetch()}
        />
      ) : match ? (
        <div className="space-y-6">
          {/* Top Score Banner */}
          <div className="flex flex-col sm:flex-row items-center justify-between p-6 rounded-2xl bg-gradient-to-br from-indigo-900 to-slate-900 text-white gap-6">
            <div className="space-y-2 text-center sm:text-left">
              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                <Sparkles className="w-3.5 h-3.5" />
                Score Breakdown
              </span>
              <h3 className="text-xl font-bold">Overall Match Score</h3>
              <p className="text-xs text-indigo-200">
                {match.overallScore >= 75
                  ? 'High compatibility: Your technical skills strongly align with the requirements.'
                  : match.overallScore >= 50
                  ? 'Moderate compatibility: You satisfy core requirements, but some skills may be missing.'
                  : 'Low compatibility: Consider upskilling or targeting other opportunities.'}
              </p>
            </div>
            <ScoreGauge score={match.overallScore} size="lg" />
          </div>

          {/* Matched vs Missing Skills */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Matched Skills */}
            <div className="p-4 rounded-xl border border-emerald-100 bg-emerald-50/40 space-y-3">
              <div className="flex items-center gap-2 text-emerald-800 font-bold text-xs uppercase tracking-wider">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                Matched Skills ({match.matchedSkills?.length || 0})
              </div>
              <div className="space-y-2 max-h-48 overflow-y-auto">
                {(match.matchedSkills || []).length === 0 ? (
                  <p className="text-xs text-slate-400">No overlapping skills found</p>
                ) : (
                  (match.matchedSkills || []).map((s) => (
                    <div key={s.skillId} className="p-2.5 bg-white rounded-lg border border-emerald-200/60 text-xs shadow-2xs">
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-slate-900">{s.skillName}</span>
                        <span className="text-[10px] text-slate-500">{s.studentProficiency}</span>
                      </div>
                      <div className="flex items-center justify-between text-[11px] text-slate-400 mt-1">
                        <span>Required: {s.requiredProficiency}</span>
                        <span className="font-semibold text-emerald-600">{(s.proficiencyMultiplier * 100).toFixed(0)}%</span>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Missing Skills */}
            <div className="p-4 rounded-xl border border-rose-100 bg-rose-50/40 space-y-3">
              <div className="flex items-center gap-2 text-rose-800 font-bold text-xs uppercase tracking-wider">
                <XCircle className="w-4 h-4 text-rose-600" />
                Missing Competencies ({((match.missingRequiredSkills?.length || 0) + (match.missingOptionalSkills?.length || 0))})
              </div>
              <div className="space-y-2 max-h-48 overflow-y-auto">
                {((match.missingRequiredSkills?.length || 0) + (match.missingOptionalSkills?.length || 0)) === 0 ? (
                  <p className="text-xs text-emerald-700 font-medium">All required skills present in your profile!</p>
                ) : (
                  <>
                    {(match.missingRequiredSkills || []).map((s) => (
                      <div key={s.skillId} className="p-2.5 bg-white rounded-lg border border-rose-200/60 text-xs shadow-2xs">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-slate-900">{s.skillName}</span>
                          <span className="text-[10px] font-semibold text-rose-600">Mandatory</span>
                        </div>
                        <p className="text-[11px] text-slate-500 mt-1">
                          Target Level: <span className="font-medium text-slate-700">{s.requiredProficiency}</span>
                        </p>
                      </div>
                    ))}
                    {(match.missingOptionalSkills || []).map((s) => (
                      <div key={s.skillId} className="p-2.5 bg-white rounded-lg border border-amber-200/60 text-xs shadow-2xs">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-slate-900">{s.skillName}</span>
                          <span className="text-[10px] font-semibold text-amber-600">Optional</span>
                        </div>
                        <p className="text-[11px] text-slate-500 mt-1">
                          Target Level: <span className="font-medium text-slate-700">{s.requiredProficiency}</span>
                        </p>
                      </div>
                    ))}
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </Modal>
  );
}
