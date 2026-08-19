import { Briefcase } from 'lucide-react';
import { Link } from 'react-router-dom';

export function Footer() {
  return (
    <footer className="bg-white border-t border-slate-200 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <div className="flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-2.5 font-bold text-lg text-slate-900">
            <div className="w-7 h-7 rounded-lg bg-indigo-600 flex items-center justify-center text-white">
              <Briefcase className="w-4 h-4" />
            </div>
            <span>Career<span className="text-indigo-600">Forge</span></span>
          </div>
          <p className="text-xs text-slate-500 text-center">
            &copy; {new Date().getFullYear()} CareerForge. Intelligent Career & Recruitment Management Platform.
          </p>
          <div className="flex items-center gap-6 text-xs text-slate-500">
            <Link to="/jobs" className="hover:text-indigo-600 transition-colors">
              Find Jobs
            </Link>
            <Link to="/companies" className="hover:text-indigo-600 transition-colors">
              Companies
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
