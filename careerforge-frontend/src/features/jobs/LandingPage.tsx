import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Briefcase, Sparkles, Building2, Search, Target } from 'lucide-react';
import { useAuthStore } from '@/features/auth/authStore';

export function LandingPage() {
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuthStore();

  const getDashboardPath = () => {
    if (user?.role === 'ROLE_ADMIN') return '/admin/dashboard';
    if (user?.role === 'ROLE_RECRUITER') return '/recruiter/dashboard';
    return '/student/dashboard';
  };

  return (
    <div className="flex flex-col">
      {/* Hero Section */}
      <section className="relative overflow-hidden pt-20 pb-28 bg-gradient-to-b from-indigo-50/70 via-white to-slate-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center relative z-10">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-100/80 text-indigo-700 text-xs font-semibold mb-6">
            <Sparkles className="w-3.5 h-3.5" />
            Deterministic Skill-Matching Engine Powered
          </div>

          <h1 className="text-4xl sm:text-6xl font-extrabold text-slate-900 tracking-tight max-w-4xl mx-auto leading-tight sm:leading-none">
            Empowering Careers with <span className="text-indigo-600">Intelligent Recruitment</span>
          </h1>

          <p className="mt-6 text-lg sm:text-xl text-slate-600 max-w-2xl mx-auto">
            Connect students and top verified employers through algorithmic candidate-job compatibility scoring, automated applicant tracking, and transparent hiring pipelines.
          </p>

          <div className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4">
            {isAuthenticated ? (
              <Button size="lg" onClick={() => navigate(getDashboardPath())}>
                Go to Dashboard
              </Button>
            ) : (
              <>
                <Button size="lg" onClick={() => navigate('/jobs')}>
                  <Search className="w-4 h-4 mr-2" />
                  Explore Jobs
                </Button>
                <Button variant="outline" size="lg" onClick={() => navigate('/register')}>
                  Join as Recruiter / Student
                </Button>
              </>
            )}
          </div>
        </div>
      </section>

      {/* Value Pillars */}
      <section className="py-20 bg-white border-t border-slate-100">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold text-slate-900">Why CareerForge?</h2>
            <p className="text-slate-500 mt-2 text-sm max-w-xl mx-auto">
              Engineered with rigorous mathematical matching models and enterprise security architecture.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="p-8 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col">
              <div className="w-12 h-12 rounded-xl bg-indigo-600 text-white flex items-center justify-center mb-6">
                <Target className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-slate-900 mb-3">Deterministic Skill Matching</h3>
              <p className="text-sm text-slate-600 leading-relaxed flex-1">
                Weighted multi-factor compatibility algorithms analyze candidate proficiency against required and optional skills with zero black-box bias.
              </p>
            </div>

            <div className="p-8 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col">
              <div className="w-12 h-12 rounded-xl bg-indigo-600 text-white flex items-center justify-center mb-6">
                <Building2 className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-slate-900 mb-3">Verified Employer Ecosystem</h3>
              <p className="text-sm text-slate-600 leading-relaxed flex-1">
                Strict administrative moderation and company verification workflows ensure all jobs and hiring organizations are authentic and compliant.
              </p>
            </div>

            <div className="p-8 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col">
              <div className="w-12 h-12 rounded-xl bg-indigo-600 text-white flex items-center justify-center mb-6">
                <Briefcase className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-slate-900 mb-3">Complete ATS Pipeline</h3>
              <p className="text-sm text-slate-600 leading-relaxed flex-1">
                Multi-stage application tracking with strict state transitions, interview scheduling, confidential recruiter evaluations, and real-time status alerts.
              </p>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
