import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { recruiterService } from '@/services/recruiter.service';
import { queryKeys } from '@/lib/queryClient';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { ErrorState } from '@/components/feedback/ErrorState';
import { Building2, Globe, CheckCircle2, ArrowLeft } from 'lucide-react';

function getValidExternalWebsiteUrl(rawUrl?: string): string | null {
  if (!rawUrl) return null;
  const trimmed = rawUrl.trim();
  if (!trimmed) return null;

  const lower = trimmed.toLowerCase();
  if (
    lower.includes('localhost') ||
    lower.endsWith('.local') ||
    lower.includes('.local/') ||
    lower.endsWith('.internal') ||
    lower.includes('.internal/')
  ) {
    return null;
  }

  const normalized = lower.startsWith('http://') || lower.startsWith('https://')
    ? trimmed
    : `https://${trimmed}`;

  try {
    const parsed = new URL(normalized);
    if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
      return null;
    }
    const hostname = parsed.hostname.toLowerCase();
    if (
      hostname === 'localhost' ||
      hostname.endsWith('.local') ||
      hostname.endsWith('.internal') ||
      !hostname.includes('.')
    ) {
      return null;
    }
    return normalized;
  } catch {
    return null;
  }
}

export function CompanyPublicDetailPage() {
  const { slug } = useParams<{ slug: string }>();

  const { data: company, isLoading, isError, error } = useQuery({
    queryKey: queryKeys.publicCompanies.detail(slug || ''),
    queryFn: () => {
      if (slug && !isNaN(Number(slug))) {
        return recruiterService.getCompanyById(Number(slug));
      }
      return recruiterService.getCompanyBySlug(slug || '');
    },
    enabled: !!slug,
  });

  const validWebsiteUrl = getValidExternalWebsiteUrl(company?.website);

  if (isLoading) return <LoadingSpinner text="Loading company profile..." />;
  if (isError || !company) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12">
        <ErrorState
          error={error}
          title={!company && !error ? 'Company Not Found' : undefined}
          message={!company && !error ? 'This company profile is not accessible.' : undefined}
        />
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <Link to="/companies" className="inline-flex items-center text-xs font-semibold text-slate-500 hover:text-indigo-600 mb-6">
        <ArrowLeft className="w-4 h-4 mr-1" />
        Back to Company Directory
      </Link>

      <Card>
        <CardContent className="p-6 sm:p-8 space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 rounded-2xl bg-slate-100 flex items-center justify-center text-indigo-600 font-bold border border-slate-200">
                <Building2 className="w-8 h-8" />
              </div>
              <div>
                <div className="flex items-center gap-2 flex-wrap">
                  <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900">{company.name}</h1>
                  <Badge variant="success" className="flex items-center gap-1">
                    <CheckCircle2 className="w-3 h-3" />
                    Verified
                  </Badge>
                </div>
                <p className="text-sm text-slate-500 mt-1">{company.industry || 'Industry unspecified'}</p>
              </div>
            </div>

            {validWebsiteUrl && (
              <a
                href={validWebsiteUrl}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1 text-xs font-semibold text-indigo-600 hover:underline"
              >
                <Globe className="w-4 h-4" />
                Visit Website
              </a>
            )}
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 p-4 rounded-xl bg-slate-50 border border-slate-100 text-xs">
            <div>
              <span className="text-slate-400">Location</span>
              <p className="font-semibold text-slate-900 mt-0.5">{company.location || 'Global / Remote'}</p>
            </div>
            <div>
              <span className="text-slate-400">Company Size</span>
              <p className="font-semibold text-slate-900 mt-0.5">{company.companySize || 'Private'}</p>
            </div>
            <div>
              <span className="text-slate-400">Verification Status</span>
              <p className="font-semibold text-emerald-700 mt-0.5">{company.verificationStatus}</p>
            </div>
          </div>

          {company.description && (
            <div>
              <h3 className="text-base font-bold text-slate-900 mb-2">About {company.name}</h3>
              <p className="text-sm text-slate-700 leading-relaxed whitespace-pre-line">{company.description}</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
